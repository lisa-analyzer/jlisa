package it.unive.jlisa.witness.validator.violation;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import it.unive.jlisa.witness.validator.RunSummary;
import it.unive.jlisa.witness.validator.model.*;
import it.unive.jlisa.witness.validator.search.Trace;
import java.util.*;

/**
 * Core JDI event processing loop for violation witness validation.
 * <h3>Lifecycle</h3>
 * <ol>
 * <li>On construction: register {@link ClassPrepareRequest}s for every class
 * that has breakpoints in the plan, and {@link MethodEntryRequest}s for all
 * {@code Verifier.nondet*} interceptions.</li>
 * <li>{@link #run()} resumes the VM and processes events until a terminal
 * condition is reached or the timeout expires.</li>
 * </ol>
 * <h3>Terminal conditions</h3>
 * <ul>
 * <li>{@link ValidationResult#CORRECT} — target breakpoint reached, or an
 * uncaught exception fires after all planned interceptions are consumed</li>
 * <li>{@link ValidationResult#SPURIOUS} — avoid-point reached, the VM exits, or
 * an unrelated exception is thrown, without hitting the target, <em>and</em>
 * every {@code Verifier.nondet*} call returned a value fixed by the witness
 * (witness steps that could not be read do not matter then: the run is
 * determined)</li>
 * <li>{@link ValidationResult#UNKNOWN} — the same endings after a
 * {@code nondet*} call the witness did not fix (the run used a value of its
 * own, so it says nothing about the witness), or a timeout / VM disconnect</li>
 * <li>{@link ValidationResult#ERROR} — the JVM threw an {@link Error}</li>
 * </ul>
 */
public final class EventLoop {

	/**
	 * Validation outcome returned by {@link #run()}.
	 */
	public enum ValidationResult {
		CORRECT,
		SPURIOUS,
		UNKNOWN,
		ERROR
	}

	/**
	 * Class-name filter for the SV-COMP {@code Verifier} class (any package).
	 */
	private static final String VERIFIER_CLASS_FILTER = "*Verifier";

	/**
	 * Returned by {@code nondet*()} methods of type String that the witness
	 * does not fix.
	 */
	// TODO: find a less naive way for setting default values.
	private static final String DEFAULT_STRING = "a";

	/**
	 * Default timeout: 80 seconds (BenchExec limit is 90 s for violations).
	 */
	private static final long TIMEOUT_MS = 80_000L;

	private final VirtualMachine vm;
	private final ViolationPlan plan;
	private final Property property;

	// the function_return waypoints of the witness, passed in order
	private final WitnessCalls calls;
	// the task sources, for the target of the violation
	private final WitnessLocations locations;
	// Verifier.nondet* calls that returned a value not fixed by the witness
	private int freeNondetCalls = 0;
	// a witness value that could not be returned (the run left the witness)
	private boolean witnessValueNotApplied = false;

	// Deferred breakpoints: class name → list of (line, plan-item-type)
	private final Map<String, List<PendingBreakpoint>> pendingBreakpoints = new HashMap<>();

	// Active breakpoint requests: Location → plan item
	private record PendingBreakpoint(
			int line,
			PointType type,
			Object payload) {
	}

	private enum PointType {
		TARGET,
		AVOID,
		BRANCH
	}

	/**
	 * @param vm        the target VM, suspended at start
	 * @param plan      the plan of the witness
	 * @param property  the property
	 * @param locations the task sources, or null if they cannot be read
	 */
	public EventLoop(
			VirtualMachine vm,
			ViolationPlan plan,
			Property property,
			WitnessLocations locations) {
		this.vm = vm;
		this.plan = plan;
		this.property = property;
		this.locations = locations;
		this.calls = new WitnessCalls(plan.interceptions(), locations);
	}

	/**
	 * Registers all event requests and drives the event loop to completion.
	 *
	 * @return the validation result
	 */
	public ValidationResult run() {
		setupRequests();
		vm.resume();

		long deadline = System.currentTimeMillis() + TIMEOUT_MS;
		EventQueue queue = vm.eventQueue();

		while (true) {
			long remaining = deadline - System.currentTimeMillis();
			if (remaining <= 0) {
				ValidatorLogger.result("Timeout after {}ms — could not validate", TIMEOUT_MS);
				vm.exit(1);
				return handleErrorStatus("timeout", "no verdict after " + TIMEOUT_MS + " ms");
			}

			EventSet eventSet;
			try {
				eventSet = queue.remove(Math.min(remaining, 1000L));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				vm.exit(1);
				return handleErrorStatus("interrupted", "validator thread interrupted");
			} catch (VMDisconnectedException e) {
				// VM terminated — it ran to completion without reaching the
				// target
				ValidatorLogger.event("VM disconnected");
				return handleErrorStatus("vm-disconnected", "target JVM disconnected before a verdict");
			}

			if (eventSet == null) {
				continue; // poll timeout — check overall deadline on next
							// iteration
			}

			Optional<ValidationResult> result;
			for (Event event : eventSet) {
				result = processEvent(event);

				if (result.isPresent()) {
					try {
						vm.exit(0);
					} catch (Exception ignored) {
						// VM already gone (e.g. after VMDeathEvent): the result
						// still stands
					}
					return result.get();
				}
			}

			try {
				eventSet.resume();
			} catch (VMDisconnectedException e) {
				return handleErrorStatus("vm-disconnected", "target JVM disconnected before a verdict");
			}
		}
	}

	// ------------------------------------------------------------------
	// event request setup
	// ------------------------------------------------------------------

	private void setupRequests() {
		EventRequestManager erm = vm.eventRequestManager();

		// 1. MethodEntryRequest for the Verifier.nondet* calls
		RunSummary.add("witness.values", calls.size());

		// One request for every Verifier method: each nondet* call either gets
		// the witness value
		// or is counted as free (one request per interception key would report
		// each call twice).
		MethodEntryRequest req = erm.createMethodEntryRequest();
		req.addClassFilter(VERIFIER_CLASS_FILTER);
		req.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		req.enable();
		ValidatorLogger.jdi("MethodEntryRequest: {} ({} witness values)", VERIFIER_CLASS_FILTER, calls.size());

		// 2. ClassPrepareRequests for classes with breakpoints (the target is
		// checked at the violation, from the stack)
		for (AvoidPoint ap : plan.avoidPoints()) {
			queueBreakpoint(ap.fileName(), ap.line(), PointType.AVOID, ap);
		}
		for (BranchDecision bd : plan.branchDecisions()) {
			queueBreakpoint(bd.fileName(), bd.line(), PointType.BRANCH, bd);
		}
		RunSummary.add("plan.targets", plan.targetPoints().size());
		RunSummary.add("plan.avoid-points", plan.avoidPoints().size());
		RunSummary.add("plan.branches", plan.branchDecisions().size());

		// Register ClassPrepareRequests for every class that needs breakpoints.
		// We use the file name as a class-name filter (approximate — works for
		// simple programs).
		for (String fileName : pendingBreakpoints.keySet()) {
			String classFilter = fileNameToClassFilter(fileName);
			ClassPrepareRequest cpReq = erm.createClassPrepareRequest();
			cpReq.addClassFilter(classFilter);
			cpReq.setSuspendPolicy(EventRequest.SUSPEND_ALL);
			cpReq.enable();
			ValidatorLogger.jdi("ClassPrepareRequest: filter={}", classFilter);
		}

		// 3. ExceptionRequest: catch uncaught exceptions (AssertionError,
		// RuntimeException)
		ExceptionRequest exReq = erm.createExceptionRequest(null, false, true);
		exReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		exReq.enable();
		ValidatorLogger.jdi("ExceptionRequest registered (uncaught exceptions)");
	}

	private void queueBreakpoint(
			String fileName,
			int line,
			PointType type,
			Object payload) {
		pendingBreakpoints.computeIfAbsent(fileName, k -> new ArrayList<>())
				.add(new PendingBreakpoint(line, type, payload));
	}

	// ------------------------------------------------------------------
	// event processing
	// ------------------------------------------------------------------

	private Optional<ValidationResult> processEvent(
			Event event) {
		if (event instanceof VMStartEvent) {
			ValidatorLogger.jdi("VMStartEvent — execution about to begin");
			return Optional.empty();
		}

		if (event instanceof ClassPrepareEvent cpe) {
			return handleClassPrepare(cpe);
		}

		if (event instanceof MethodEntryEvent mee) {
			return handleMethodEntry(mee);
		}

		if (event instanceof BreakpointEvent bpe) {
			return handleBreakpoint(bpe);
		}

		if (event instanceof ExceptionEvent ee) {
			return handleException(ee);
		}

		if (event instanceof VMDeathEvent) {
			ValidatorLogger.event("VMDeathEvent — program exited without hitting target");
			return Optional.of(noViolation("program-exited", "program ended without the violation"));
		}

		return Optional.empty();
	}

	private Optional<ValidationResult> handleClassPrepare(
			ClassPrepareEvent event) {
		ReferenceType refType = event.referenceType();
		String className = refType.name();
		ValidatorLogger.jdi("ClassPrepare: {}", className);

		// Find pending breakpoints for this class (matched by file name)
		for (Map.Entry<String, List<PendingBreakpoint>> entry : pendingBreakpoints.entrySet()) {
			String fileName = entry.getKey();
			if (!classMatchesFile(className, fileName)) {
				continue;
			}
			for (PendingBreakpoint pb : entry.getValue()) {
				installBreakpoint(refType, pb, fileName);
			}
		}
		return Optional.empty();
	}

	private void installBreakpoint(
			ReferenceType refType,
			PendingBreakpoint pb,
			String fileName) {
		try {
			List<Location> locations = refType.locationsOfLine(pb.line());
			if (locations.isEmpty()) {
				RunSummary.count("breakpoints.skipped");
				ValidatorLogger.warn("[JDI]    No location at {}:{} — breakpoint skipped",
						fileName, pb.line());
				return;
			}
			BreakpointRequest bpReq = vm.eventRequestManager()
					.createBreakpointRequest(locations.get(0));
			bpReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			bpReq.putProperty("planItem", pb);
			bpReq.enable();
			RunSummary.count("breakpoints.installed");
			ValidatorLogger.jdi("BreakpointRequest: {}:{} ({})", fileName, pb.line(), pb.type());
		} catch (AbsentInformationException e) {
			RunSummary.count("breakpoints.skipped");
			ValidatorLogger.warn("[JDI]    No line number info for {} — compile with -g", refType.name());
		}
	}

	private Optional<ValidationResult> handleMethodEntry(
			MethodEntryEvent event) {
		String className = event.method().declaringType().name();
		String methodName = event.method().name();
		if (!methodName.startsWith("nondet") || methodName.equals("nondetObject")) {
			return Optional.empty(); // e.g. Verifier.assume
		}

		Interception ix = null;
		try {
			ix = calls.match(event.thread().frame(1).location()).orElse(null);
		} catch (IncompatibleThreadStateException | IndexOutOfBoundsException e) {
			ValidatorLogger.warn("[EVENT]  no caller frame for {}.{}(): {}", className, methodName, e.getMessage());
		}

		if (ix == null || !ix.fixesValue()) {
			// Not fixed by the witness: any value is consistent with it, but
			// the run can no longer reject it. Strings get a short default:
			// Verifier.nondetString() picks a random size, which halts in
			// assume() when negative and allocates up to 2 GB otherwise.
			freeNondetCalls++;
			RunSummary.count(ix == null ? "nondet.free" : "nondet.not-single-value");
			if (event.method().returnTypeName().equals("java.lang.String")) {
				try {
					ForceReturnSteerer.steer(event.thread(), DEFAULT_STRING, event.method().signature());
					ValidatorLogger.event("MethodEntry: {}.{}() — no witness value, returning default \"{}\"",
							className, methodName, DEFAULT_STRING);
					return Optional.empty();
				} catch (Exception e) {
					ValidatorLogger.warn("[EVENT]  forceEarlyReturn failed: {} — letting method run normally",
							e.getMessage());
				}
			}
			ValidatorLogger.event("MethodEntry: {}.{}() — no witness value{}, runs freely",
					className, methodName, ix == null ? "" : " (" + ix.constraint() + ")");
			return Optional.empty();
		}

		ValidatorLogger.event("MethodEntry: {}.{}() — forcing return value: {}",
				className, methodName, ix.value());

		try {
			ForceReturnSteerer.steer(event.thread(), ix.value(), event.method().signature());
		} catch (Exception e) {
			ValidatorLogger.warn("[EVENT]  forceEarlyReturn failed: {} — letting method run normally",
					e.getMessage());
			freeNondetCalls++;
			witnessValueNotApplied = true;
			RunSummary.count("nondet.value-not-applied");
			return Optional.empty();
		}
		RunSummary.count("nondet.fixed");

		return Optional.empty();
	}

	private Optional<ValidationResult> handleBreakpoint(
			BreakpointEvent event) {
		Location loc = event.location();
		String sourceName = safeSourceName(loc);
		int line = loc.lineNumber();

		ValidatorLogger.event("Breakpoint: {}:{}", sourceName, line);

		Object planItem = null;
		// Find the plan item via the request property
		Object prop = event.request().getProperty("planItem");
		if (prop instanceof PendingBreakpoint pb) {
			planItem = pb.payload();
			switch (pb.type()) {
			case TARGET -> {
				ValidatorLogger.event("Breakpoint: {}:{} — TARGET REACHED", sourceName, line);
				return Optional
						.of(correct("target-reached", "violation location " + sourceName + ":" + line + " reached"));
			}
			case AVOID -> {
				ValidatorLogger.event("Breakpoint: {}:{} — AVOID POINT REACHED", sourceName, line);
				return Optional.of(noViolation("avoid-point", "location " + sourceName + ":" + line
						+ " that the witness excludes reached"));
			}
			case BRANCH -> {
				BranchDecision bd = (BranchDecision) planItem;
				handleBranchDecision(event.thread(), bd, sourceName, line);
			}
			}
		}

		return Optional.empty();
	}

	private void handleBranchDecision(
			ThreadReference thread,
			BranchDecision bd,
			String sourceName,
			int line) {
		// For branching waypoints, we need to ensure the branch goes in the
		// direction
		// specified by the witness. The VariableSteerer is used to flip the
		// condition
		// variable if needed. Currently a best-effort heuristic: look for a
		// boolean
		// local variable whose name appears in the context.
		// Full Z3-based synthesis is a future enhancement.
		ValidatorLogger.event("BranchDecision: {}:{} expected direction={}", sourceName, line, bd.direction());
		// No-op for now — the branch will follow whatever the natural execution
		// dictates.
		// If this leads to an incorrect path the VMDeathEvent or ExceptionEvent
		// will
		// eventually report "Witness Spurious".
	}

	private Optional<ValidationResult> handleException(
			ExceptionEvent event) {
		ObjectReference reference = event.exception();
		String exceptionClass = reference.type().name();
		Location loc = event.location();
		ValidatorLogger.event("Exception: {} at {}:{}",
				exceptionClass, safeSourceName(loc), loc.lineNumber());

		// An uncaught exception after all planned interceptions is the property
		// violation
		// we were looking for → witness confirmed

		if (property == Property.ASSERT) {
			if (exceptionClass.contains("AssertionError")) {
				return Optional.of(violation(event, "assertion-error", exceptionClass + " at " + where(loc)));
			}
		}

		boolean isError = isRefInstance(reference, (ClassType) vm.classesByName("java.lang.Error").getFirst());
		boolean isException = isRefInstance(reference, (ClassType) vm.classesByName("java.lang.Exception").getFirst());
		boolean isRuntimeException = isRefInstance(reference,
				(ClassType) vm.classesByName("java.lang.RuntimeException").getFirst());

		if (isError) {
			ValidatorLogger.event("Uncaught JVM ERROR - " + reference);
			recordUnusedValues();
			RunSummary.reason("jvm-error", exceptionClass + " at " + where(loc));
			return Optional.of(ValidationResult.ERROR);
		} else if (isException) {
			if (property == Property.RUNTIME && isRuntimeException) {
				return Optional.of(violation(event, "runtime-exception", exceptionClass + " at " + where(loc)));
			}
			ValidatorLogger.event("Uncaught Exception - " + reference);
			return Optional.of(noViolation("other-exception", exceptionClass + " at " + where(loc)
					+ ", not a violation of " + property));
		}

		return Optional.empty();
	}

	// ------------------------------------------------------------------
	// private utilities
	// ------------------------------------------------------------------

	private boolean isRefInstance(
			ObjectReference ref,
			ClassType errorType) {
		ReferenceType type = ref.referenceType();

		while (type instanceof ClassType classType) {
			if (type.equals(errorType)) {
				return true;
			}

			type = classType.superclass();
		}

		return false;
	}

	/**
	 * The run ended without the violation. That rejects the witness only if the
	 * witness fixed every nondet value the run used; otherwise the run took
	 * values of its own and says nothing about the witness.
	 */
	/**
	 * Verifier.nondet* calls of the run that returned a value the witness did
	 * not fix.
	 */
	public int freeNondetCalls() {
		return freeNondetCalls;
	}

	/** True if a witness value could not be returned in the run. */
	public boolean witnessValueNotApplied() {
		return witnessValueNotApplied;
	}

	private ValidationResult noViolation(
			String code,
			String detail) {
		recordUnusedValues();
		// Unread steps matter only if a nondet call was free: when the witness
		// fixed every
		// nondet value, the run is determined and an unread step cannot change
		// it.
		if (plan.skippedSteps() > 0 && freeNondetCalls > 0) {
			// The run did not follow the witness: it says nothing about it.
			ValidatorLogger.event("No violation, but {} witness step(s) could not be read — unknown",
					plan.skippedSteps());
			RunSummary.reason(code, detail + "; " + plan.skippedSteps()
					+ " witness step(s) could not be read");
			return ValidationResult.UNKNOWN;
		}
		if (freeNondetCalls > 0) {
			ValidatorLogger.event("No violation, but {} nondet call(s) not fixed by the witness — unknown",
					freeNondetCalls);
			RunSummary.reason(code, detail + "; " + freeNondetCalls + " nondet call(s) not fixed by the witness");
			return ValidationResult.UNKNOWN;
		}
		RunSummary.reason(code, detail + "; every nondet call was fixed by the witness");
		return ValidationResult.SPURIOUS;
	}

	/**
	 * The property is violated: the witness is confirmed if the violation is at
	 * its target, that is, a frame of the thread is at the target line, or the
	 * target is where jlisa would put the target of this violation. A violation
	 * elsewhere rejects the witness if the witness fixed every nondet value of
	 * the run; otherwise it says nothing about the witness.
	 */
	private ValidationResult violation(
			ExceptionEvent event,
			String code,
			String detail) {
		if (plan.targetPoints().isEmpty() || atTarget(event.thread())) {
			ValidatorLogger.event("Violation at the target — CORRECT");
			return correct(code, detail);
		}
		recordUnusedValues();
		// When the witness fixed every nondet value, the run is the only one
		// the witness allows: its violation elsewhere rejects the witness.
		// Otherwise the run took values of its own and says nothing about it.
		if (freeNondetCalls == 0 && !witnessValueNotApplied) {
			ValidatorLogger.event("Violation, but not at the target {} — spurious", plan.targetPoints());
			RunSummary.reason("violation-elsewhere", detail + ", not at the target " + plan.targetPoints()
					+ "; every nondet call was fixed by the witness");
			return ValidationResult.SPURIOUS;
		}
		ValidatorLogger.event("Violation, but not at the target {} — unknown", plan.targetPoints());
		RunSummary.reason("violation-elsewhere", detail + ", not at the target " + plan.targetPoints() + "; "
				+ freeNondetCalls + " nondet call(s) not fixed by the witness");
		return ValidationResult.UNKNOWN;
	}

	private boolean atTarget(
			ThreadReference thread) {
		List<Trace.Frame> frames = new ArrayList<>();
		try {
			for (StackFrame f : thread.frames()) {
				Location l = f.location();
				String path;
				try {
					path = l.sourcePath();
				} catch (AbsentInformationException e) {
					path = null;
				}
				for (TargetPoint t : plan.targetPoints()) {
					if (l.lineNumber() == t.line()
							&& WitnessCalls.baseName(t.fileName()).equals(safeSourceName(l))) {
						return true;
					}
				}
				frames.add(new Trace.Frame(path, l.lineNumber(), l.codeIndex(), l.declaringType().name(),
						l.method().name()));
			}
		} catch (IncompatibleThreadStateException e) {
			return false;
		}
		if (locations == null) {
			return false;
		}
		WitnessLocations.Point p = locations.target(new Trace(List.of(), null, frames), property);
		if (p == null) {
			return false;
		}
		for (TargetPoint t : plan.targetPoints()) {
			if (p.line() == t.line() && WitnessCalls.baseName(t.fileName())
					.equals(WitnessCalls.baseName(p.sourcePath()))) {
				return true;
			}
		}
		return false;
	}

	private ValidationResult correct(
			String code,
			String detail) {
		recordUnusedValues();
		RunSummary.reason(code, detail);
		return ValidationResult.CORRECT;
	}

	/** Timeout or VM disconnect: never evidence against the witness. */
	private ValidationResult handleErrorStatus(
			String code,
			String detail) {
		recordUnusedValues();
		RunSummary.reason(code, detail);
		return ValidationResult.UNKNOWN;
	}

	/**
	 * Witness values that the run never asked for: a sign that it left the
	 * witness path.
	 */
	private void recordUnusedValues() {
		int unused = calls.unused();
		RunSummary.set("witness.values-unused", unused);
	}

	private static String where(
			Location loc) {
		return safeSourceName(loc) + ":" + loc.lineNumber();
	}

	/**
	 * Heuristically checks if a JVM class name corresponds to a source file
	 * name. E.g. {@code "Main"} matches {@code "Main.java"},
	 * {@code "org.example.Foo"} matches {@code "Foo.java"} or
	 * {@code "org/example/Foo.java"}.
	 */
	private static boolean classMatchesFile(
			String className,
			String fileName) {
		String simple = className.contains(".") ? className.substring(className.lastIndexOf('.') + 1)
				: className;
		// Strip inner class suffix (e.g. "Outer$Inner" → "Outer")
		if (simple.contains("$")) {
			simple = simple.substring(0, simple.indexOf('$'));
		}
		String baseFile = fileName.replace('/', '.').replace('\\', '.');
		if (baseFile.endsWith(".java")) {
			baseFile = baseFile.substring(0, baseFile.length() - 5);
		}
		// baseFile may be "Main" or "org.example.Main"
		return baseFile.endsWith(simple) || simple.equals(baseFile);
	}

	/**
	 * Converts a source file name to a class-name filter prefix for
	 * {@link ClassPrepareRequest#addClassFilter}. Uses wildcard {@code *}
	 * suffix so that inner classes and package-prefixed names are matched.
	 */
	private static String fileNameToClassFilter(
			String fileName) {
		String base = fileName;
		int slashIdx = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		if (slashIdx >= 0) {
			base = fileName.substring(slashIdx + 1);
		}
		if (base.endsWith(".java")) {
			base = base.substring(0, base.length() - 5);
		}
		return "*" + base + "*";
	}

	private static String safeSourceName(
			Location loc) {
		try {
			return loc.sourceName();
		} catch (AbsentInformationException e) {
			return loc.declaringType().name();
		}
	}
}

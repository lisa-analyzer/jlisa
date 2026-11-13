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
	// where the first nondet calls not fixed by the witness are, as JDI tells
	// (reported with the verdict, to tell why they are not fixed)
	private final List<String> freeCallSites = new ArrayList<>();
	private static final int MAX_REPORTED_SITES = 3;
	// a witness value that could not be returned (the run left the witness)
	private boolean witnessValueNotApplied = false;

	/**
	 * Whether the target JVM disconnected before a verdict, without the VM
	 * death event of a program that ends (e.g., it could not start, or it was
	 * killed).
	 */
	private boolean disconnected = false;

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
				disconnected = true;
				return handleErrorStatus("vm-disconnected", "target JVM disconnected before a verdict");
			}

			if (eventSet == null) {
				continue; // poll timeout — check overall deadline on next
							// iteration
			}

			Optional<ValidationResult> result;
			for (Event event : eventSet) {
				try {
					result = processEvent(event);
				} catch (VMDisconnectedException e) {
					ValidatorLogger.event("VM disconnected while handling {}", event);
					disconnected = true;
				return handleErrorStatus("vm-disconnected", "target JVM disconnected before a verdict");
				}

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
				disconnected = true;
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
			if (!SourceFiles.defines(fileName, refType)) {
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
		String site = "unknown caller";
		Location caller = callerOf(event.thread());
		if (caller == null)
			ValidatorLogger.warn("[EVENT]  no caller frame for {}.{}()", className, methodName);
		else {
			site = describe(caller);
			ix = calls.match(caller).orElse(null);
		}
		if (ix == null && freeCallSites.size() < MAX_REPORTED_SITES) {
			freeCallSites.add(methodName + " at " + site);
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

		if (isError && isResourceExhaustion(reference)) {
			// the program ran out of stack or memory with the values of the
			// witness (e.g., a recursion as deep as an input): the run says
			// nothing about the violation, and the validator did not fail
			ValidatorLogger.event("Resources exhausted - " + reference);
			recordUnusedValues();
			RunSummary.reason("resource-exhausted", exceptionClass + " at " + where(loc));
			return Optional.of(ValidationResult.UNKNOWN);
		}
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

	/**
	 * Yields whether the given exception is a {@link VirtualMachineError}
	 * signalling that the program ran out of resources (stack or memory).
	 */
	private boolean isResourceExhaustion(
			ObjectReference ref) {
		for (String name : List.of("java.lang.StackOverflowError", "java.lang.OutOfMemoryError"))
			for (ReferenceType type : vm.classesByName(name))
				if (type instanceof ClassType && isRefInstance(ref, (ClassType) type))
					return true;
		return false;
	}

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
	 * Yields whether the target JVM disconnected before a verdict, without
	 * ending as programs do (e.g., it could not start, or it was killed): the
	 * run says nothing about the witness, and it can be repeated.
	 *
	 * @return whether the target JVM disconnected
	 */
	public boolean disconnected() {
		return disconnected;
	}

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
			RunSummary.reason(code, detail + "; " + freeNondetCalls + " nondet call(s) not fixed by the witness"
					+ freeSites());
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
		if (plan.targetPoints().isEmpty() || atTarget(event.thread())
				|| createdAtTarget(event.exception(), event.thread())) {
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
					+ frames(event.thread()) + "; every nondet call was fixed by the witness");
			return ValidationResult.SPURIOUS;
		}
		ValidatorLogger.event("Violation, but not at the target {} — unknown", plan.targetPoints());
		RunSummary.reason("violation-elsewhere", detail + ", not at the target " + plan.targetPoints()
				+ frames(event.thread()) + "; " + freeNondetCalls + " nondet call(s) not fixed by the witness"
				+ freeSites());
		return ValidationResult.UNKNOWN;
	}

	/**
	 * True if the given exception was created at the target, according to its
	 * stack trace. An exception caught and rethrown (e.g., by the handler of a
	 * {@code finally} block, at the end of the block) is uncaught only at the
	 * rethrow, where the frames of the thread no longer reach the statement
	 * raising it: its stack trace, filled when it is created, still does. The
	 * thread must be suspended by the event of the exception.
	 */
	private boolean createdAtTarget(
			ObjectReference exception,
			ThreadReference thread) {
		try {
			List<Method> methods = exception.referenceType().methodsByName("getStackTrace",
					"()[Ljava/lang/StackTraceElement;");
			if (methods.isEmpty()) {
				return false;
			}
			Value trace = exception.invokeMethod(thread, methods.getFirst(), List.of(),
					ObjectReference.INVOKE_SINGLE_THREADED);
			if (!(trace instanceof ArrayReference elements)) {
				return false;
			}
			for (Value value : elements.getValues()) {
				if (!(value instanceof ObjectReference element)) {
					continue;
				}
				ReferenceType type = element.referenceType();
				Field fileField = type.fieldByName("fileName");
				Field lineField = type.fieldByName("lineNumber");
				Field classField = type.fieldByName("declaringClass");
				if (fileField == null || lineField == null) {
					continue;
				}
				Value file = element.getValue(fileField);
				Value line = element.getValue(lineField);
				if (!(file instanceof StringReference fileName) || !(line instanceof IntegerValue lineNumber)) {
					continue;
				}
				Value declaring = classField == null ? null : element.getValue(classField);
				for (TargetPoint t : plan.targetPoints()) {
					if (lineNumber.value() == t.line()
							&& (declaring instanceof StringReference className
									? SourceFiles.contains(t.fileName(), className.value(), fileName.value())
									: WitnessCalls.baseName(t.fileName()).equals(fileName.value()))) {
						return true;
					}
				}
			}
		} catch (InvalidTypeException | ClassNotLoadedException | IncompatibleThreadStateException
				| InvocationException | RuntimeException e) {
			ValidatorLogger.event("Cannot read the stack trace of the exception: {}", e.toString());
		}
		return false;
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
					if (l.lineNumber() == t.line() && SourceFiles.contains(t.fileName(), l)) {
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
			if (p.line() == t.line() && SourceFiles.matches(t.fileName(), p.sourcePath())) {
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

	/**
	 * The number of attempts at reading the frame of the caller of a method
	 * that has just been entered.
	 */
	private static final int CALLER_ATTEMPTS = 5;

	/**
	 * Yields the location of the caller of the method the given thread has just
	 * entered, or {@code null} if it cannot be read. The frames of the thread
	 * are read a few times, as they might not be available right away (e.g.,
	 * when the target JVM is under load).
	 */
	private Location callerOf(
			ThreadReference thread) {
		for (int attempt = 1; attempt <= CALLER_ATTEMPTS; attempt++) {
			try {
				return thread.frame(1).location();
			} catch (IncompatibleThreadStateException | IndexOutOfBoundsException e) {
				ValidatorLogger.warn("[EVENT]  cannot read the caller frame (attempt {}): {}", attempt, e);
				RunSummary.count("caller-frame." + e.getClass().getSimpleName());
				try {
					Thread.sleep(10L * attempt);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * Describes the given location as JDI tells it: the path of its source
	 * relative to the root of the sources, and its line.
	 */
	private static String describe(
			Location loc) {
		String path;
		try {
			path = loc.sourcePath();
		} catch (AbsentInformationException e) {
			path = loc.declaringType().name() + " (no source information)";
		}
		return path + ":" + loc.lineNumber();
	}

	/**
	 * Describes where the first nondet calls not fixed by the witness are, if
	 * any.
	 */
	private String freeSites() {
		return freeCallSites.isEmpty() ? "" : " (" + String.join(", ", freeCallSites) + ")";
	}

	/**
	 * Describes the first frames of the given thread, as JDI tells them.
	 */
	private static String frames(
			ThreadReference thread) {
		try {
			List<String> frames = new ArrayList<>();
			for (StackFrame f : thread.frames()) {
				if (frames.size() == MAX_REPORTED_SITES) {
					break;
				}
				frames.add(describe(f.location()));
			}
			return " (frames: " + String.join(", ", frames) + ")";
		} catch (IncompatibleThreadStateException e) {
			return "";
		}
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

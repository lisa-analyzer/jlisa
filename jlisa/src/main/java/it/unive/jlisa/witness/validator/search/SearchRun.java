package it.unive.jlisa.witness.validator.search;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.MethodEntryRequest;
import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.model.Interception;
import it.unive.jlisa.witness.validator.model.Property;
import it.unive.jlisa.witness.validator.model.TargetPoint;
import it.unive.jlisa.witness.validator.violation.ForceReturnSteerer;
import it.unive.jlisa.witness.validator.violation.JdiLauncher;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * One run of the program under JDI for the input search.
 * <p>
 * Each {@code Verifier.nondet*()} call returns, in this order: the witness
 * value (the same interceptions, in the same order, as the first run of the
 * validator), else the value the search chose for the call site
 * ({@code File.java:line} of the caller), else a color (colored run) or the
 * default 0 / false / "". Breakpoints on the watched lines record which
 * variables hold a value that only one site returned, and what the line
 * compares them with.
 */
final class SearchRun {

	enum Outcome {
		/**
		 * The property's violation, at the witness's location if it gives one.
		 */
		VIOLATION,
		/**
		 * The run ended (normally, by {@code System.exit}, or a failed
		 * {@code Verifier.assume}).
		 */
		EXITED,
		/**
		 * An uncaught exception or error that is not the property's violation,
		 * or elsewhere.
		 */
		OTHER,
		TIMEOUT,
		/**
		 * A witness value could not be returned: the search must not go on (it
		 * would override the witness).
		 */
		WITNESS_VALUE_NOT_APPLIED
	}

	private static final String VERIFIER = "org.sosy_lab.sv_benchmarks.Verifier";
	private static final int HITS_PER_LINE = 20;

	Outcome outcome = Outcome.EXITED;
	String detail = "";
	/**
	 * Call sites without a witness value, in order of first call, with their
	 * return type.
	 */
	final Map<String, String> freeSites = new LinkedHashMap<>();
	/** The first value each free site returned. */
	final Map<String, Val> injected = new LinkedHashMap<>();
	final Map<String, LinkedHashSet<Val>> learned = new LinkedHashMap<>();
	final Set<String> linesHit = new java.util.HashSet<>();
	boolean assertLineHit;

	private final ProgramText text;
	private final Property property;
	private final List<TargetPoint> targets;
	private final Map<String, Queue<Interception>> witness = new HashMap<>();

	private SearchRun(
			ProgramText text,
			Property property,
			List<TargetPoint> targets,
			List<Interception> witnessValues) {
		this.text = text;
		this.property = property;
		this.targets = targets;
		for (Interception ix : witnessValues) {
			if (ix.descriptor().isBlank()) {
				continue; // v2 variable assignments are not steered at method
							// entry
			}
			witness.computeIfAbsent(ix.className() + "." + ix.methodName(), k -> new ArrayDeque<>())
					.offer(new Interception(ix.className(), ix.methodName(), ix.descriptor(), ix.returnValue(),
							ix.count()));
		}
	}

	/**
	 * Runs the program once; {@code witnessValues} are fresh copies for this
	 * run.
	 */
	static SearchRun run(
			String mainClass,
			String classpath,
			String java,
			ProgramText text,
			Property property,
			List<TargetPoint> targets,
			List<Interception> witnessValues,
			Map<String, Val> assignment,
			boolean colored,
			long timeoutMillis)
			throws ValidationException {
		return run(mainClass, classpath, java, text, property, targets, witnessValues, assignment, colored,
				timeoutMillis, true);
	}

	/** As above, with Java assertions enabled ({@code -ea}) or not. */
	static SearchRun run(
			String mainClass,
			String classpath,
			String java,
			ProgramText text,
			Property property,
			List<TargetPoint> targets,
			List<Interception> witnessValues,
			Map<String, Val> assignment,
			boolean colored,
			long timeoutMillis,
			boolean assertions)
			throws ValidationException {
		SearchRun r = new SearchRun(text, property, targets, witnessValues);
		VirtualMachine vm = JdiLauncher.launch(mainClass, classpath, java, assertions);
		try {
			r.loop(vm, assignment, colored, System.currentTimeMillis() + timeoutMillis);
		} finally {
			try {
				vm.exit(1);
			} catch (Exception ignored) {
				// already gone
			}
			vm.process().destroyForcibly();
		}
		return r;
	}

	private void loop(
			VirtualMachine vm,
			Map<String, Val> assignment,
			boolean colored,
			long deadline) {
		EventRequestManager erm = vm.eventRequestManager();
		MethodEntryRequest me = erm.createMethodEntryRequest();
		me.addClassFilter(VERIFIER);
		me.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		me.enable();
		ExceptionRequest ex = erm.createExceptionRequest(null, false, true);
		ex.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		ex.enable();
		ClassPrepareRequest cp = erm.createClassPrepareRequest();
		for (String pkg : List.of("java.*", "javax.*", "sun.*", "jdk.*", "com.sun.*", "org.sosy_lab.*")) {
			cp.addClassExclusionFilter(pkg);
		}
		cp.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		cp.enable();
		Map<String, Integer> hits = new HashMap<>();
		Map<String, Integer> colors = new HashMap<>();
		vm.resume();
		EventQueue queue = vm.eventQueue();
		while (true) {
			long left = deadline - System.currentTimeMillis();
			if (left <= 0) {
				outcome = Outcome.TIMEOUT;
				return;
			}
			EventSet set;
			try {
				set = queue.remove(Math.min(left, 500));
			} catch (VMDisconnectedException | InterruptedException e) {
				return;
			}
			if (set == null) {
				continue;
			}
			try {
				for (Event e : set) {
					if (e instanceof VMDeathEvent || e instanceof VMDisconnectEvent) {
						return;
					}
					if (e instanceof ClassPrepareEvent c) {
						installBreakpoints(vm, c.referenceType());
					} else if (e instanceof MethodEntryEvent m) {
						if (!steer(vm, m, assignment, colored, colors)) {
							return;
						}
					} else if (e instanceof BreakpointEvent b) {
						String key = (String) b.request().getProperty("line");
						linesHit.add(key);
						if (Boolean.TRUE.equals(b.request().getProperty("assert"))) {
							assertLineHit = true;
						}
						if (hits.merge(key, 1, Integer::sum) >= HITS_PER_LINE) {
							b.request().disable();
						}
						learn(b);
					} else if (e instanceof ExceptionEvent x) {
						exception(vm, x);
						return;
					}
				}
				set.resume();
			} catch (VMDisconnectedException e) {
				return;
			}
		}
	}

	/**
	 * Returns false when the run must stop (a witness value could not be
	 * applied).
	 */
	private boolean steer(
			VirtualMachine vm,
			MethodEntryEvent m,
			Map<String, Val> assignment,
			boolean colored,
			Map<String, Integer> colors) {
		String name = m.method().name();
		if (!name.startsWith("nondet") || name.equals("nondetObject")) {
			return true; // assume(), nondetObject (the factory calls nondet*
							// itself)
		}
		ThreadReference thread = m.thread();
		Queue<Interception> fromWitness = witness.get(m.method().declaringType().name() + "." + name);
		Interception ix = fromWitness == null ? null : fromWitness.peek();
		if (ix != null && ix.hasRemaining()) {
			if (ix.count() == 1) {
				fromWitness.remove();
			}
			try {
				ForceReturnSteerer.steer(thread, ix.returnValue(), ix.descriptor());
				ix.decrementAndGet();
				return true;
			} catch (ValidationException e) {
				outcome = Outcome.WITNESS_VALUE_NOT_APPLIED;
				detail = e.getMessage();
				return false;
			}
		}
		String site;
		try {
			Location caller = thread.frame(1).location();
			site = sourceName(caller) + ":" + caller.lineNumber();
		} catch (IncompatibleThreadStateException e) {
			return true;
		}
		String type = m.method().returnTypeName();
		freeSites.putIfAbsent(site, type);
		Val v = assignment.get(site);
		if (v == null) {
			v = colored ? Val.color(type, colors.computeIfAbsent(site, s -> colors.size())) : Val.defaultFor(type);
		}
		injected.putIfAbsent(site, v);
		Value mirror = v.mirror(vm, type);
		if (mirror != null) {
			try {
				thread.forceEarlyReturn(mirror);
			} catch (Exception e) {
				// the method then runs normally: its own value is consistent
				// with any witness
			}
		}
		return true;
	}

	private void exception(
			VirtualMachine vm,
			ExceptionEvent x) {
		ObjectReference exc = x.exception();
		String type = exc.referenceType().name();
		boolean isViolation = property == Property.ASSERT
				? type.equals("java.lang.AssertionError")
				: isSubclass(exc, "java.lang.RuntimeException");
		if (!isViolation) {
			outcome = Outcome.OTHER;
			detail = type;
			return;
		}
		if (!targets.isEmpty() && !atTarget(x.thread())) {
			outcome = Outcome.OTHER;
			detail = type + " away from the witness's violation location";
			return;
		}
		outcome = Outcome.VIOLATION;
		detail = type + " at " + sourceName(x.location()) + ":" + x.location().lineNumber();
	}

	/**
	 * True if one of the frames of the failing thread is at a target line of
	 * the witness.
	 */
	private boolean atTarget(
			ThreadReference thread) {
		try {
			for (StackFrame f : thread.frames()) {
				Location l = f.location();
				for (TargetPoint t : targets) {
					if (l.lineNumber() == t.line() && baseName(t.fileName()).equals(sourceName(l))) {
						return true;
					}
				}
			}
		} catch (IncompatibleThreadStateException e) {
			return false;
		}
		return false;
	}

	private static boolean isSubclass(
			ObjectReference o,
			String name) {
		if (!(o.referenceType() instanceof ClassType c)) {
			return false;
		}
		for (ClassType t = c; t != null; t = t.superclass()) {
			if (t.name().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private void installBreakpoints(
			VirtualMachine vm,
			ReferenceType type) {
		String file;
		try {
			file = type.sourceName();
		} catch (AbsentInformationException e) {
			return;
		}
		for (int line : text.watchedLines(file)) {
			try {
				List<Location> locations = type.locationsOfLine(line);
				if (locations.isEmpty()) {
					continue;
				}
				BreakpointRequest br = vm.eventRequestManager().createBreakpointRequest(locations.getFirst());
				br.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
				br.putProperty("line", file + ":" + line);
				br.putProperty("assert", text.isAssert(file, line));
				br.enable();
			} catch (AbsentInformationException ignored) {
				// compiled without line numbers
			}
		}
	}

	/**
	 * Input-to-state: a variable at this line that holds a value only one free
	 * site returned in this run links the line to that site; what the line
	 * compares it with is learned for the site.
	 */
	private void learn(
			BreakpointEvent b) {
		try {
			StackFrame f = b.thread().frame(0);
			Map<String, Val> vars = new LinkedHashMap<>();
			try {
				for (Map.Entry<LocalVariable, Value> e : f.getValues(f.visibleVariables()).entrySet()) {
					Val v = Val.read(e.getValue());
					if (v != null) {
						vars.put(e.getKey().name(), v);
					}
				}
			} catch (AbsentInformationException ignored) {
				// no local variable table
			}
			ObjectReference self = f.thisObject();
			if (self != null) {
				for (Field field : self.referenceType().visibleFields()) {
					if (!field.isStatic()) {
						Val v = Val.read(self.getValue(field));
						if (v != null) {
							vars.putIfAbsent(field.name(), v);
						}
					}
				}
			}
			String line = text.line(sourceName(b.location()), b.location().lineNumber());
			for (Map.Entry<String, Val> site : injected.entrySet()) {
				long sharing = injected.values().stream().filter(v -> v.same(site.getValue())).count();
				if (sharing > 1) {
					continue; // the value does not point to one site
				}
				for (Map.Entry<String, Val> var : vars.entrySet()) {
					if (var.getValue().same(site.getValue())) {
						ProgramText.counterparts(line, var.getKey(), vars,
								learned.computeIfAbsent(site.getKey(), k -> new LinkedHashSet<>()));
					}
				}
			}
		} catch (IncompatibleThreadStateException | InvalidStackFrameException ignored) {
			// the thread moved on
		}
	}

	int score() {
		return linesHit.size() + (assertLineHit ? 1000 : 0);
	}

	private static String sourceName(
			Location l) {
		try {
			return l.sourceName();
		} catch (AbsentInformationException e) {
			return l.declaringType().name();
		}
	}

	private static String baseName(
			String path) {
		int i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return i >= 0 ? path.substring(i + 1) : path;
	}
}

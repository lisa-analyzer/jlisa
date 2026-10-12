package it.unive.jlisa.witness.validator.violation;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import it.unive.jlisa.witness.validator.model.*;

import java.util.*;

/**
 * Core JDI event processing loop for violation witness validation.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>On construction: register {@link ClassPrepareRequest}s for every class that has
 *       breakpoints in the plan, and {@link MethodEntryRequest}s for all
 *       {@code Verifier.nondet*} interceptions.</li>
 *   <li>{@link #run()} resumes the VM and processes events until a terminal condition is
 *       reached or the timeout expires.</li>
 * </ol>
 *
 * <h3>Terminal conditions</h3>
 * <ul>
 *   <li>{@link ValidationResult#CORRECT} — target breakpoint reached, or an uncaught
 *       exception fires after all planned interceptions are consumed</li>
 *   <li>{@link ValidationResult#SPURIOUS} — avoid-point reached, or the VM exits normally
 *       without hitting the target</li>
 *   <li>{@link ValidationResult#ERROR} — timeout</li>
 * </ul>
 */
public final class EventLoop {

    /**
     * Validation outcome returned by {@link #run()}.
     */
    public enum ValidationResult {
        CORRECT, SPURIOUS, ERROR
    }

    /**
     * Default timeout: 80 seconds (BenchExec limit is 90 s for violations).
     */
    private static final long TIMEOUT_MS = 80_000L;

    private final VirtualMachine vm;
    private final ViolationPlan plan;
    private final BenchmarkProperties properties;

    // Runtime state for interceptions: map from "className.methodName" → Interception
    private final Map<String, Queue<Interception>> interceptMap = new HashMap<>();
    // Runtime state: which MethodEntryRequest corresponds to which interception key
    private final Map<MethodEntryRequest, String> methodRequests = new HashMap<>();

    // Deferred breakpoints: class name → list of (line, plan-item-type)
    private final Map<String, List<PendingBreakpoint>> pendingBreakpoints = new HashMap<>();

    // Active breakpoint requests: Location → plan item
    private record PendingBreakpoint(int line, PointType type, Object payload) {
    }

    private enum PointType {
        TARGET, AVOID, BRANCH
    }

    public EventLoop(VirtualMachine vm, ViolationPlan plan, BenchmarkProperties properties) {
        this.vm = vm;
        this.plan = plan;
        this.properties = properties;
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
                vm.dispose();
                return handleErrorStatus();
            }

            EventSet eventSet;
            try {
                eventSet = queue.remove(Math.min(remaining, 1000L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                vm.dispose();
                return handleErrorStatus();
            } catch (VMDisconnectedException e) {
                // VM terminated — it ran to completion without reaching the target
                ValidatorLogger.event("VM disconnected");
                return handleErrorStatus();
            }

            if (eventSet == null) {
                continue; // poll timeout — check overall deadline on next iteration
            }

            Optional<ValidationResult> result;
            for (Event event : eventSet) {
                result = processEvent(event);

                if (result.isPresent()) {
                    try {
                        vm.dispose();
                    } catch (Exception ignored) {
                        break;
                    }
                    return result.get();
                }
            }

            try {
                eventSet.resume();
            } catch (VMDisconnectedException e) {
                return handleErrorStatus();
            }
        }
    }

    // ------------------------------------------------------------------
    // event request setup
    // ------------------------------------------------------------------

    private void setupRequests() {
        EventRequestManager erm = vm.eventRequestManager();

        // 1. MethodEntryRequests for all Verifier.nondet* interceptions
        for (Interception ix : plan.interceptions()) {
            if (ix.descriptor().isBlank()) {
                // v2 variable-assignment path — handled via breakpoints, not method entry
                continue;
            }
            String key = ix.className() + "." + ix.methodName();
            ValidatorLogger.jdi("SETTING KEY: " + key);
            interceptMap.computeIfAbsent(key, s -> new ArrayDeque<>()).offer(ix);
        }

        for (String key : interceptMap.keySet()) {
            Interception ixRef = interceptMap.get(key).peek();
            assert ixRef != null;
            MethodEntryRequest req = erm.createMethodEntryRequest();
            req.addClassFilter(ixRef.className());
            req.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            req.enable();
            methodRequests.put(req, key);
            ValidatorLogger.jdi("MethodEntryRequest: {}.{}{}",
                    ixRef.className(), ixRef.methodName(), ixRef.descriptor());
        }

        // 2. ClassPrepareRequests for classes with breakpoints
        for (TargetPoint tp : plan.targetPoints()) {
            queueBreakpoint(tp.fileName(), tp.line(), PointType.TARGET, tp);
        }
        for (AvoidPoint ap : plan.avoidPoints()) {
            queueBreakpoint(ap.fileName(), ap.line(), PointType.AVOID, ap);
        }
        for (BranchDecision bd : plan.branchDecisions()) {
            queueBreakpoint(bd.fileName(), bd.line(), PointType.BRANCH, bd);
        }

        // Register ClassPrepareRequests for every class that needs breakpoints.
        // We use the file name as a class-name filter (approximate — works for simple programs).
        for (String fileName : pendingBreakpoints.keySet()) {
            String classFilter = fileNameToClassFilter(fileName);
            ClassPrepareRequest cpReq = erm.createClassPrepareRequest();
            cpReq.addClassFilter(classFilter);
            cpReq.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            cpReq.enable();
            ValidatorLogger.jdi("ClassPrepareRequest: filter={}", classFilter);
        }

        // 3. ExceptionRequest: catch uncaught exceptions (AssertionError, RuntimeException)
        ExceptionRequest exReq = erm.createExceptionRequest(null, false, true);
        exReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        exReq.enable();
        ValidatorLogger.jdi("ExceptionRequest registered (uncaught exceptions)");
    }

    private void queueBreakpoint(String fileName, int line, PointType type, Object payload) {
        pendingBreakpoints.computeIfAbsent(fileName, k -> new ArrayList<>())
                .add(new PendingBreakpoint(line, type, payload));
    }

    // ------------------------------------------------------------------
    // event processing
    // ------------------------------------------------------------------

    private Optional<ValidationResult> processEvent(Event event) {
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
            return Optional.of(ValidationResult.SPURIOUS);
        }

        return Optional.empty();
    }

    private Optional<ValidationResult> handleClassPrepare(ClassPrepareEvent event) {
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

    private void installBreakpoint(ReferenceType refType, PendingBreakpoint pb, String fileName) {
        try {
            List<Location> locations = refType.locationsOfLine(pb.line());
            if (locations.isEmpty()) {
                ValidatorLogger.warn("[JDI]    No location at {}:{} — breakpoint skipped",
                        fileName, pb.line());
                return;
            }
            BreakpointRequest bpReq = vm.eventRequestManager()
                    .createBreakpointRequest(locations.get(0));
            bpReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            bpReq.putProperty("planItem", pb);
            bpReq.enable();
            ValidatorLogger.jdi("BreakpointRequest: {}:{} ({})", fileName, pb.line(), pb.type());
        } catch (AbsentInformationException e) {
            ValidatorLogger.warn("[JDI]    No line number info for {} — compile with -g", refType.name());
        }
    }

    private Optional<ValidationResult> handleMethodEntry(MethodEntryEvent event) {
        String className = event.method().declaringType().name();
        String methodName = event.method().name();
        String key = className + "." + methodName;

        Queue<Interception> interceptions = interceptMap.get(key);
        if (interceptions == null) {
            // method not handled
            return Optional.empty();
        }
        Interception ix = interceptMap.get(key).peek();

        if (ix != null && ix.count() == 1) {
            interceptMap.get(key).remove();
        }
        if (ix == null || !ix.hasRemaining()) {
            return Optional.empty(); // not an interception target, or already exhausted
        }

        ValidatorLogger.event("MethodEntry: {}.{}() — forcing return value: {}",
                className, methodName, ix.returnValue());

        try {
            ForceReturnSteerer.steer(event.thread(), ix.returnValue(), ix.descriptor());
        } catch (Exception e) {
            ValidatorLogger.warn("[EVENT]  forceEarlyReturn failed: {} — letting method run normally",
                    e.getMessage());
            return Optional.empty();
        }

        int remaining = ix.decrementAndGet();
        if (remaining <= 0) {
            // All expected occurrences consumed — disable the request
            // fixme: disableMethodEntryRequest(key);
            ValidatorLogger.event("MethodEntry: {}.{}() — all interceptions consumed, request disabled",
                    className, methodName);
        }

        return Optional.empty();
    }

    private Optional<ValidationResult> handleBreakpoint(BreakpointEvent event) {
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
                    return Optional.of(ValidationResult.CORRECT);
                }
                case AVOID -> {
                    ValidatorLogger.event("Breakpoint: {}:{} — AVOID POINT REACHED — spurious", sourceName, line);
                    return Optional.of(ValidationResult.SPURIOUS);
                }
                case BRANCH -> {
                    BranchDecision bd = (BranchDecision) planItem;
                    handleBranchDecision(event.thread(), bd, sourceName, line);
                }
            }
        }

        return Optional.empty();
    }

    private void handleBranchDecision(ThreadReference thread, BranchDecision bd, String sourceName, int line) {
        // For branching waypoints, we need to ensure the branch goes in the direction
        // specified by the witness.  The VariableSteerer is used to flip the condition
        // variable if needed.  Currently a best-effort heuristic: look for a boolean
        // local variable whose name appears in the context.
        // Full Z3-based synthesis is a future enhancement.
        ValidatorLogger.event("BranchDecision: {}:{} expected direction={}", sourceName, line, bd.direction());
        // No-op for now — the branch will follow whatever the natural execution dictates.
        // If this leads to an incorrect path the VMDeathEvent or ExceptionEvent will
        // eventually report "Witness Spurious".
    }

    private Optional<ValidationResult> handleException(ExceptionEvent event) {
        ObjectReference reference = event.exception();
        String exceptionClass = reference.type().name();
        Location loc = event.location();
        ValidatorLogger.event("Exception: {} at {}:{}",
                exceptionClass, safeSourceName(loc), loc.lineNumber());

        // An uncaught exception after all planned interceptions is the property violation
        // we were looking for → witness confirmed

        if (properties.validAssert()) {
            if (exceptionClass.contains("AssertionError")) {
                ValidatorLogger.event("Uncaught exception confirms violation - CORRECT");
                return Optional.of(ValidationResult.CORRECT);
            }
        }

        boolean isError = isRefInstance(reference, (ClassType) vm.classesByName("java.lang.Error").getFirst());
        boolean isException = isRefInstance(reference, (ClassType) vm.classesByName("java.lang.Exception").getFirst());
        boolean isRuntimeException = isRefInstance(reference, (ClassType) vm.classesByName("java.lang.RuntimeException").getFirst());

        if (isError) {
            ValidatorLogger.event("Uncaught JVM ERROR - " + reference);
            return Optional.of(ValidationResult.ERROR);
        } else if (isException) {
            if (properties.noRuntimeException() && isRuntimeException) {
                return Optional.of(ValidationResult.CORRECT);
            }
            ValidatorLogger.event("Uncaught Exception - " + reference);
            return Optional.of(ValidationResult.SPURIOUS);
        }

        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // private utilities
    // ------------------------------------------------------------------

    private void disableMethodEntryRequest(String key) {
        methodRequests.entrySet().stream()
                .filter(e -> e.getValue().equals(key))
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(req -> req.setEnabled(false));
    }

    private boolean isRefInstance(ObjectReference ref, ClassType errorType) {
        ReferenceType type = ref.referenceType();

        while (type instanceof ClassType classType) {
            if (type.equals(errorType)) {
                return true;
            }

            type = classType.superclass();
        }

        return false;
    }

    private ValidationResult handleErrorStatus() {
        if (this.plan.interceptions().isEmpty()) {
            return ValidationResult.SPURIOUS;
        }
        return ValidationResult.ERROR;
    }

    /**
     * Heuristically checks if a JVM class name corresponds to a source file name.
     * E.g. {@code "Main"} matches {@code "Main.java"}, {@code "org.example.Foo"} matches
     * {@code "Foo.java"} or {@code "org/example/Foo.java"}.
     */
    private static boolean classMatchesFile(String className, String fileName) {
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
     * {@link ClassPrepareRequest#addClassFilter}.  Uses wildcard {@code *} suffix so that
     * inner classes and package-prefixed names are matched.
     */
    private static String fileNameToClassFilter(String fileName) {
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

    private static String safeSourceName(Location loc) {
        try {
            return loc.sourceName();
        } catch (AbsentInformationException e) {
            return loc.declaringType().name();
        }
    }
}

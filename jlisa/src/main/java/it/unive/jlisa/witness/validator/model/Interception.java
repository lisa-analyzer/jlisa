package it.unive.jlisa.witness.validator.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Describes a JDI method interception: when the target JVM enters {@code className.methodName},
 * we call {@link com.sun.jdi.ThreadReference#forceEarlyReturn(com.sun.jdi.Value)} to inject
 * {@code returnValue} as the method's result.
 *
 * <p>The {@code descriptor} is the JVM method descriptor (e.g. {@code ()I}) used to determine
 * which {@link com.sun.jdi.Value} mirror to create. The {@code remainingFires} counter tracks
 * how many times this interception should still fire; once exhausted the corresponding
 * {@link com.sun.jdi.request.MethodEntryRequest} is disabled so remaining calls run freely.
 */
public final class Interception {

    private final String className;
    private final String methodName;
    private final String descriptor;
    private final String returnValue;
    private final AtomicInteger remainingFires;

    public Interception(String className, String methodName, String descriptor, String returnValue, int count) {
        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.returnValue = returnValue;
        this.remainingFires = new AtomicInteger(count);
    }

    /**
     * @return fully-qualified class name (e.g. {@code org.sosy_lab.sv_benchmarks.Verifier})
     */
    public String className() {
        return className;
    }

    /**
     * @return simple method name (e.g. {@code nondetInt})
     */
    public String methodName() {
        return methodName;
    }

    /**
     * @return JVM method descriptor suffix, starting with {@code (} (e.g. {@code ()I} for
     * {@code int nondetInt()}).  Used by {@code ForceReturnSteerer} to select the
     * correct {@code VirtualMachine.mirrorOf()} overload.
     */
    public String descriptor() {
        return descriptor;
    }

    /**
     * @return the concrete return value as a string (e.g. {@code "0"}, {@code "true"})
     */
    public String returnValue() {
        return returnValue;
    }

    /**
     * Decrements the remaining-fires counter and returns the new value.
     * When it reaches zero the caller should disable the method-entry request.
     */
    public int decrementAndGet() {
        return remainingFires.decrementAndGet();
    }

    /**
     * Returns the current remaining-fires count (snapshot — may change concurrently).
     */
    public int count() {
        return remainingFires.get();
    }

    /**
     * Returns {@code true} if this interception should still fire at least once.
     */
    public boolean hasRemaining() {
        return remainingFires.get() > 0;
    }

    @Override
    public String toString() {
        return className + "." + methodName + descriptor + " → return " + returnValue
                + " (" + remainingFires.get() + " remaining)";
    }
}

package it.unive.jlisa.witness.validator.model;

/**
 * Describes a JDI method interception: when the target JVM enters
 * {@code className.methodName}, we call
 * {@link com.sun.jdi.ThreadReference#forceEarlyReturn(com.sun.jdi.Value)} to
 * inject {@code returnValue} as the method's result.
 * <p>
 * The {@code descriptor} is the JVM method descriptor (e.g. {@code ()I}) used
 * to determine which {@link com.sun.jdi.Value} mirror to create. The
 * {@code remainingFires} counter tracks how many times this interception should
 * still fire; once exhausted the corresponding
 * {@link com.sun.jdi.request.MethodEntryRequest} is disabled so remaining calls
 * run freely.
 */
public final class Interception {

	private final String className;
	private final int lineNumber;
	private final String returnValue;

	public Interception(
			String className,
			int lineNumber,
			String returnValue) {
		this.className = className;
		this.lineNumber = lineNumber;
		this.returnValue = returnValue;
	}

	/**
	 * @return fully-qualified class name (e.g.
	 *             {@code org.sosy_lab.sv_benchmarks.Verifier})
	 */
	public String className() {
		return className;
	}

	/**
	 * @return line number
	 */
	public int lineNumber() {
		return lineNumber;
	}

	/**
	 * @return the concrete return value as a string (e.g. {@code "0"},
	 *             {@code "true"})
	 */
	public String returnValue() {
		return returnValue;
	}

	@Override
	public String toString() {
		return className + "." + lineNumber + " → return " + returnValue;
	}
}

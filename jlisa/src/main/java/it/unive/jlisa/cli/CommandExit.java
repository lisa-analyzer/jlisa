package it.unive.jlisa.cli;

/**
 * Thrown by a sub-command that ends with the given exit code (e.g., after a
 * failure that it has already reported), instead of exiting the virtual
 * machine: {@link it.unive.jlisa.Main#main(String[])} exits with the code, while
 * {@link it.unive.jlisa.Main#run(String[])} (e.g., in tests) lets it propagate.
 */
public class CommandExit
		extends
		RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int code;

	/**
	 * Builds the exception.
	 *
	 * @param code the exit code
	 */
	public CommandExit(
			int code) {
		super("jlisa exited with code " + code);
		this.code = code;
	}

	/**
	 * Yields the exit code.
	 *
	 * @return the exit code
	 */
	public int getCode() {
		return code;
	}
}

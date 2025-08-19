package it.unive.jlisa.frontend.exceptions;

public class JavaSyntaxException extends RuntimeException {

	private static final long serialVersionUID = 4950907533241537846L;

	/**
	 * Builds the exception.
	 */
	public JavaSyntaxException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the inner cause of this exception
	 */
	public JavaSyntaxException(
			String message,
			Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public JavaSyntaxException(
			String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the inner cause of this exception
	 */
	public JavaSyntaxException(
			Throwable cause) {
		super(cause);
	}
}
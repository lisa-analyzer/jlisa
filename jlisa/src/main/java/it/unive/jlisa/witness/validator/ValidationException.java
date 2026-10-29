package it.unive.jlisa.witness.validator;

/**
 * Thrown when witness parsing or validation encounters an unrecoverable error.
 * The validator catches this at the top level and outputs {@code Could not validate}.
 */
public final class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

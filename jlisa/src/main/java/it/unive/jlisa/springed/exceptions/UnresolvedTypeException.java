package it.unive.jlisa.springed.exceptions;

import java.io.Serial;

public class UnresolvedTypeException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 5748301926650184773L;

	private String unresolvedName;

	public UnresolvedTypeException() {
		super();
	}

	public UnresolvedTypeException(
			String message,
			Throwable cause,
			String unresolvedName) {
		super(message, cause);
		this.unresolvedName = unresolvedName;
	}

	public UnresolvedTypeException(
			String message,
			String unresolvedName) {
		super(message);
		this.unresolvedName = unresolvedName;
	}

	public String getUnresolvedName() {
		return unresolvedName;
	}

}

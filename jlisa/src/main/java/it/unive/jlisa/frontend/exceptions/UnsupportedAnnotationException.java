package it.unive.jlisa.frontend.exceptions;

import java.io.Serial;

public class UnsupportedAnnotationException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 8172645391027583942L;

	public UnsupportedAnnotationException() {
		super();
	}

	public UnsupportedAnnotationException(
			String message) {
		super(message);
	}
}

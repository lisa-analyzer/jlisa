package it.unive.jlisa.frontend.exceptions;

import it.unive.lisa.program.cfg.CodeLocation;

public class ParsingException extends RuntimeException {

	public enum Type {
		UNSUPPORTED_STATEMENT,
		MISSING_EXPECTED_EXPRESSION,
		VARIABLE_ALREADY_DECLARED,
		MALFORMED_SOURCE
	}

	private String message;
	private String name;
	private CodeLocation location;
	private Type type;

	public ParsingException(
			String name,
			Type type,
			String message,
			CodeLocation location) {
		this.message = message;
		this.name = name;
		this.location = location;
		this.type = type;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return "ParsingException{" +
				"type=" + type +
				", name='" + name + '\'' +
				", message='" + getMessage() + '\'' +
				", location='" + location + '\'' +
				'}';
	}

	public String toCSVEntry(
			String separator,
			String delimiter) {
		StringBuilder sb = new StringBuilder();
		sb.append(delimiter).append(this.name).append(delimiter)
				.append(separator)
				.append(delimiter).append(this.message).append(delimiter)
				.append(separator)
				.append(delimiter).append(this.type).append(delimiter)
				.append(separator)
				.append(delimiter).append(this.location).append(delimiter);
		return sb.toString();
	}

	@Override
	public String getMessage() {
		return "ParsingException{" +
				"type=" + type +
				", name='" + name + '\'' +
				", message='" + message + '\'' +
				", location='" + location + '\'' +
				'}';
	}
}

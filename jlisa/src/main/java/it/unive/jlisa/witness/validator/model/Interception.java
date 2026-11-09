package it.unive.jlisa.witness.validator.model;

/**
 * A {@code function_return} waypoint of a violation witness on a
 * {@code Verifier.nondet*()} call: when the run passes the call at this
 * location, the call returns {@link #value()}.
 *
 * @param fileName   the file of the call, as written in the witness
 * @param line       the line of the call, from 1
 * @param column     the column of the call, from 1, or null if the witness
 *                       gives none
 * @param value      the value the call returns: a {@code Boolean},
 *                       {@code Long}, {@code Float}, {@code Double},
 *                       {@code Character} or {@code String}; null when the
 *                       constraint does not fix a single value (the call then
 *                       runs freely)
 * @param constraint the constraint of the waypoint, as written in the witness
 */
public record Interception(
		String fileName,
		int line,
		Integer column,
		Object value,
		String constraint) {

	/**
	 * Returns true if the constraint fixes the value of the call.
	 */
	public boolean fixesValue() {
		return value != null;
	}

	@Override
	public String toString() {
		return fileName + ":" + line + (column == null ? "" : ":" + column) + " → " + constraint
				+ (value == null ? " (no single value)" : "");
	}
}

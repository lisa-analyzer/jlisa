package it.unive.jlisa.program;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;

/**
 * A location of an element in a Java source file, made of the position of its
 * first character and the position of its last character. Lines start from 1,
 * columns start from 0. Two elements built from the same source code have
 * different locations as long as they do not start and end at the same
 * characters.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaLocation
		extends
		SourceCodeLocation {

	/**
	 * The line of the last character of this code element.
	 */
	private final int endLine;

	/**
	 * The column of the last character of this code element.
	 */
	private final int endCol;

	/**
	 * Builds the location.
	 *
	 * @param sourceFile the source file where this code element happens
	 * @param line       the line of the first character of this code element
	 * @param col        the column of the first character of this code element
	 * @param endLine    the line of the last character of this code element
	 * @param endCol     the column of the last character of this code element
	 *
	 * @throws IllegalArgumentException if the last character comes before the
	 *                                      first one
	 */
	public JavaLocation(
			String sourceFile,
			int line,
			int col,
			int endLine,
			int endCol) {
		super(sourceFile, line, col);
		if (endLine < line || (endLine == line && endCol < col))
			throw new IllegalArgumentException("The end of a location cannot come before its start");
		this.endLine = endLine;
		this.endCol = endCol;
	}

	/**
	 * Returns the line of the last character of this code element.
	 *
	 * @return the line
	 */
	public int getEndLine() {
		return endLine;
	}

	/**
	 * Returns the column of the last character of this code element.
	 *
	 * @return the column
	 */
	public int getEndCol() {
		return endCol;
	}

	@Override
	public String getCodeLocation() {
		return super.getCodeLocation() + "-" + endLine + ":" + endCol;
	}

	@Override
	public String toString() {
		return getCodeLocation();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + endCol;
		result = prime * result + endLine;
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (!super.equals(obj))
			return false;
		JavaLocation other = (JavaLocation) obj;
		return endLine == other.endLine && endCol == other.endCol;
	}

	@Override
	public int compareTo(
			CodeLocation other) {
		int cmp = super.compareTo(other);
		if (cmp != 0 || !(other instanceof JavaLocation))
			return cmp;
		JavaLocation o = (JavaLocation) other;
		if ((cmp = Integer.compare(endLine, o.endLine)) != 0)
			return cmp;
		return Integer.compare(endCol, o.endCol);
	}
}

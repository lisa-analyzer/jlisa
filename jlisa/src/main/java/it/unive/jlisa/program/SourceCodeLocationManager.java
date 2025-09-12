package it.unive.jlisa.program;

import it.unive.lisa.program.SourceCodeLocation;

/**
 * Manages source code locations within a file, allowing navigation through line
 * and column positions. This class is used by the front-end to determine which
 * locations a statement should use during code analysis or compilation.
 * <p>
 * The manager maintains a current position that can be advanced line by line or
 * column by column, and provides methods to retrieve location information at
 * any position within the source file.
 * </p>
 */

public class SourceCodeLocationManager {
	/** The path or name of the source file being managed */
	private String sourceFile;

	/** The current line number (0-based) */
	private int currentLineNumber = 0;

	/** The current column number (0-based) */
	private int currentColumnNumber = 0;

	/** The current location object representing the current position */
	private SourceCodeLocation currentLocation;

	/** The root location representing the starting position of the manager */
	private SourceCodeLocation rootLocation;

	/**
	 * Constructs a new SourceCodeLocationManager starting at position (0,0).
	 *
	 * @param sourceFile the path or name of the source file to manage
	 */
	public SourceCodeLocationManager(
			String sourceFile) {
		this.sourceFile = sourceFile;
		this.rootLocation = new SourceCodeLocation(sourceFile, 0, 0);
		this.currentLocation = rootLocation;
	}

	/**
	 * Constructs a new SourceCodeLocationManager starting at the specified
	 * position.
	 *
	 * @param sourceFile the path or name of the source file to manage
	 * @param lineNumber the initial line number (0-based)
	 * @param rowNumber  the initial column number (0-based)
	 */
	public SourceCodeLocationManager(
			String sourceFile,
			int lineNumber,
			int rowNumber) {
		this.sourceFile = sourceFile;
		this.rootLocation = new SourceCodeLocation(sourceFile, lineNumber, rowNumber);
		this.currentLineNumber = lineNumber;
		this.currentColumnNumber = rowNumber;
		this.currentLocation = rootLocation;
	}

	/**
	 * Returns the root location where this manager was initialized.
	 *
	 * @return the root SourceCodeLocation object
	 */
	public SourceCodeLocation getRoot() {
		return rootLocation;
	}

	/**
	 * Advances to the next line and resets the column to 0. Updates the current
	 * location to reflect the new position.
	 *
	 * @return the new current SourceCodeLocation after advancing to the next
	 *             row
	 */
	public SourceCodeLocation nextRow() {
		currentLineNumber++;
		currentColumnNumber = 0;
		currentLocation = new SourceCodeLocation(sourceFile, currentLineNumber, currentColumnNumber);
		return currentLocation;
	}

	/**
	 * Advances to the next column on the current line. Updates the current
	 * location to reflect the new position.
	 *
	 * @return the new current SourceCodeLocation after advancing to the next
	 *             column
	 */
	public SourceCodeLocation nextColumn() {
		currentColumnNumber++;
		currentLocation = new SourceCodeLocation(sourceFile, currentLineNumber, currentColumnNumber);
		return currentLocation;
	}

	/**
	 * Returns the current location managed by this instance.
	 *
	 * @return the current SourceCodeLocation
	 */
	public SourceCodeLocation getCurrentLocation() {
		return currentLocation;
	}

	/**
	 * Creates and returns a SourceCodeLocation for the specified position
	 * without modifying the current position of this manager.
	 *
	 * @param lineNumber   the line number for the requested location
	 * @param columnNumber the column number for the requested location
	 * 
	 * @return a new SourceCodeLocation at the specified position
	 */
	public SourceCodeLocation getLocation(
			int lineNumber,
			int columnNumber) {
		return new SourceCodeLocation(sourceFile, lineNumber, columnNumber);
	}
}
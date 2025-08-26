package it.unive.jlisa.program;

import it.unive.lisa.program.SourceCodeLocation;

public class SourceCodeLocationManager {
    String sourceFile;
    int currentLineNumber = 0;
    int currentColumnNumber = 0;
    SourceCodeLocation currentLocation;
    SourceCodeLocation rootLocation;

    public SourceCodeLocationManager(String sourceFile) {
        this.sourceFile = sourceFile;
        this.rootLocation = new SourceCodeLocation(sourceFile, 0, 0);
        this.currentLocation = rootLocation;
    }

    public SourceCodeLocationManager(String sourceFile, int lineNumber, int rowNumber) {
        this.sourceFile = sourceFile;
        this.rootLocation = new SourceCodeLocation(sourceFile, lineNumber, rowNumber);
        this.currentLineNumber = lineNumber;
        this.currentColumnNumber = rowNumber;
        this.currentLocation = rootLocation;
    }

    public SourceCodeLocation getRoot() {
        return rootLocation;
    }
    public SourceCodeLocation nextRow() {
        currentLineNumber++;
        currentColumnNumber = 0;
        currentLocation = new SourceCodeLocation(sourceFile, currentLineNumber, currentColumnNumber);
        return currentLocation;
    }

    public SourceCodeLocation nextColumn() {
        currentColumnNumber++;
        currentLocation = new SourceCodeLocation(sourceFile, currentLineNumber, currentColumnNumber);
        return currentLocation;
    }

    public SourceCodeLocation getCurrentLocation() {
        return currentLocation;
    }

    public SourceCodeLocation getLocation(int lineNumber, int columnNumber) {
        return new SourceCodeLocation(sourceFile, lineNumber, columnNumber);
    }
}

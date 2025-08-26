package it.unive.jlisa.program;

import it.unive.jlisa.program.cfg.SyntheticCodeLocation;

public class SyntheticCodeLocationManager {
    String sourceFile;
    int currentOffset;
    SyntheticCodeLocation currentLocation;
    SyntheticCodeLocation rootLocation;

    public SyntheticCodeLocationManager(String sourceFile) {
        this.sourceFile = sourceFile;
        this.currentOffset = 0;
        this.currentLocation = new SyntheticCodeLocation(sourceFile, currentOffset);
        this.rootLocation = new SyntheticCodeLocation(sourceFile, currentOffset);
    }

    public SyntheticCodeLocation getRoot() {
        return rootLocation;
    }


    public SyntheticCodeLocation getCurrentLocation() {
        return currentLocation;
    }

    public SyntheticCodeLocation nextLocation() {
        this.currentOffset+= 1;
        this.currentLocation = new SyntheticCodeLocation(sourceFile, currentOffset);
        return this.currentLocation;
    }

    public SyntheticCodeLocation getLocation(int offset) {
        return new SyntheticCodeLocation(sourceFile, offset);
    }
}

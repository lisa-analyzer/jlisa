package it.unive.jlisa.program.cfg;

import it.unive.lisa.program.cfg.CodeLocation;

public class SyntheticCodeLocation implements CodeLocation {
    private final String sourceFile;

    private final int offset;

    public SyntheticCodeLocation(String sourceFile, int offset) {
        this.sourceFile = "$" +sourceFile;
        this.offset = offset;
    }

    @Override
    public String getCodeLocation() {
        return "'" + sourceFile + "':::" + offset;
    }

    @Override
    public int compareTo(CodeLocation o) {
        return 0;
    }
}

package it.unive.jlisa.program.cfg;

import it.unive.lisa.program.cfg.CodeLocation;
import org.apache.commons.lang3.StringUtils;

public class SyntheticCodeLocation implements CodeLocation {
    private final String sourceFile;

    private final int offset;

    public SyntheticCodeLocation(String sourceFile, int offset) {
        this.sourceFile = "$" +sourceFile;
        this.offset = offset;
    }

    @Override
    public String getCodeLocation() {
        return "'" + sourceFile + "':" + offset;
    }

    @Override
    public int compareTo(
            CodeLocation other) {
        if (!(other instanceof SyntheticCodeLocation))
            return -1;

        SyntheticCodeLocation o = (SyntheticCodeLocation) other;

        int cmp;

        if ((cmp = StringUtils.compare(getSourceFile(), o.getSourceFile())) != 0)
            return cmp;

        return Integer.compare(getOffset(), o.getOffset());
    }

    private int getOffset() {
        return offset;
    }

    private String getSourceFile() {
        return sourceFile;
    }


    public String toString() {
        return "'" + sourceFile + "':" + offset;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + offset;
        result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
        return result;
    }

    @Override
    public boolean equals(
            Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SyntheticCodeLocation other = (SyntheticCodeLocation) obj;
        if (offset != other.offset)
            return false;
        if (sourceFile == null) {
            if (other.sourceFile != null)
                return false;
        } else if (!sourceFile.equals(other.sourceFile))
            return false;
        return true;
    }
}

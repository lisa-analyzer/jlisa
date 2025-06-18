package it.unive.jlisa.program.type;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

import java.util.Set;

public class DoubleType implements JavaNumericType {
    public static final DoubleType INSTANCE = new DoubleType();

    protected DoubleType() {}

    @Override
    public String toString() {
        return "double";
    }

    @Override
    public Set<Type> allInstances(TypeSystem types) {
        return Set.of();
    }

    @Override
    public boolean is8Bits() {
        return false;
    }

    @Override
    public boolean is16Bits() {
        return false;
    }

    @Override
    public boolean is32Bits() {
        return false;
    }

    @Override
    public boolean is64Bits() {
        return true;
    }

    @Override
    public boolean isUnsigned() {
        return false;
    }

    @Override
    public boolean isIntegral() {
        return false;
    }

}

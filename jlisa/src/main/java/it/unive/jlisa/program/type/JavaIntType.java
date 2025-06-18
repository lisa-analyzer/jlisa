package it.unive.jlisa.program.type;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

import java.util.Set;

public class JavaIntType implements JavaNumericType {
    public static final JavaIntType INSTANCE = new JavaIntType();

    protected JavaIntType() {}

    @Override
    public String toString() {
        return "int";
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
        return true;
    }

    @Override
    public boolean is64Bits() {
        return false;
    }

    @Override
    public boolean isUnsigned() {
        return false;
    }

    @Override
    public boolean isIntegral() {
        return true;
    }

}

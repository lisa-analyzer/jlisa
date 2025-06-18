package it.unive.jlisa.program.type;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

import java.util.Set;

public class JavaFloatType implements JavaNumericType {
    public static final JavaFloatType INSTANCE = new JavaFloatType();

    protected JavaFloatType() {}

    @Override
    public String toString() {
        return "float";
    }

    @Override
    public boolean canBeAssignedTo(
            Type other) {
        return (other.isUntyped() || other instanceof JavaFloatType || other instanceof JavaDoubleType);
    }

    @Override
    public Type commonSupertype(
            Type other) {
        if (other instanceof JavaLongType) {
            return this;
        }
        return JavaNumericType.super.commonSupertype(other);
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
        return false;
    }

}

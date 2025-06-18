package it.unive.jlisa.program.type;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

import java.util.Set;

public class JavaLongType implements JavaNumericType {
    public static final JavaLongType INSTANCE = new JavaLongType();

    protected JavaLongType() {}

    @Override
    public String toString() {
        return "long";
    }


    @Override
    public boolean canBeAssignedTo(
            Type other) {
        return (other.isUntyped() || other instanceof JavaLongType || other instanceof JavaDoubleType || other instanceof JavaFloatType);
    }

    @Override
    public Type commonSupertype(
            Type other) {
        if (other instanceof JavaFloatType) {
            return other;
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
        return true;
    }

}

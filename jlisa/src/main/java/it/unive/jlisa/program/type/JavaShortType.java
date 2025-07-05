package it.unive.jlisa.program.type;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

import java.util.Collections;
import java.util.Set;

public class JavaShortType implements JavaNumericType {
    public static final JavaShortType INSTANCE = new JavaShortType();

    protected JavaShortType() {}

	/**
	 * Checks whether {@code value} fits the Java's {@code short} type.
	 * 
	 * @param value the long value to check
	 * @return {@code true} if {@code value} is within the range of a {@code short},
	 *         {@code false} otherwise
	 */
    public static boolean fitsInType(long value) {
        return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
    }
    
    @Override
    public String toString() {
        return "short";
    }

    @Override
    public Set<Type> allInstances(TypeSystem types) {
        return Collections.singleton(this);
    }

    @Override
    public boolean is8Bits() {
        return false;
    }

    @Override
    public boolean is16Bits() {
        return true;
    }

    @Override
    public boolean is32Bits() {
        return false;
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

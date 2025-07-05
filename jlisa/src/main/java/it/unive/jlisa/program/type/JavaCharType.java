package it.unive.jlisa.program.type;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

import java.util.Collections;
import java.util.Set;

public class JavaCharType implements JavaNumericType{
	public static final JavaCharType INSTANCE = new JavaCharType();

	protected JavaCharType() {}

	/**
	 * Checks whether {@code value} fits the Java's {@code char} type.
	 * 
	 * @param value the long value to check
	 * @return {@code true} if {@code value} is within the range of a {@code char},
	 *         {@code false} otherwise
	 */
	public static boolean fitsInType(long value) {
		return value >= Character.MIN_VALUE && value <= Character.MAX_VALUE;
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
		return true;
	}

	@Override
	public boolean isIntegral() {
		return false;
	}
	@Override
	public String toString() {
		return "char";
	}
	@Override
	public Set<Type> allInstances(TypeSystem types) {
		return Collections.singleton(this);
	}
}

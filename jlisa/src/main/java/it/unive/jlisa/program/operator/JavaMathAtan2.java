package it.unive.jlisa.program.operator;

import java.util.Collections;
import java.util.Set;

import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaMathAtan2 implements BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaMathAtan2 INSTANCE = new JavaMathAtan2();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaMathAtan2() {
	}

	@Override
	public String toString() {
		return "atan2";
	}

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
		return Collections.singleton(JavaDoubleType.INSTANCE);

	}
}

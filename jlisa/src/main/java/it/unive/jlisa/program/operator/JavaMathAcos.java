package it.unive.jlisa.program.operator;

import java.util.Collections;
import java.util.Set;

import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaMathAcos implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaMathAcos INSTANCE = new JavaMathAcos();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaMathAcos() {
	}

	@Override
	public String toString() {
		return "acos";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		return Collections.singleton(JavaDoubleType.INSTANCE);
	}
}

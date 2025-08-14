package it.unive.jlisa.program.operator;

import java.util.Set;

import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaMathAbs implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaMathAbs INSTANCE = new JavaMathAbs();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaMathAbs() {
	}

	@Override
	public String toString() {
		return "abs";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		return argument;
	}
}

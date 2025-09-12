package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaMathSqrtOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaMathSqrtOperator INSTANCE = new JavaMathSqrtOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaMathSqrtOperator() {
	}

	@Override
	public String toString() {
		return "sqrt";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		return Collections.singleton(JavaDoubleType.INSTANCE);
	}
}

package it.unive.jlisa.program.operator;

import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

public class JavaMathAbsOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaMathAbsOperator INSTANCE = new JavaMathAbsOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaMathAbsOperator() {
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

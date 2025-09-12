package it.unive.jlisa.program.operator;

import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaStringGetBytesOperator implements StringOperator, UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringGetBytesOperator INSTANCE = new JavaStringGetBytesOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringGetBytesOperator() {
	}

	@Override
	public String toString() {
		return "strvalueof";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		// TODO: understand how to correctly type arrays
		return Collections.singleton(types.getType("JavaArrayType"));
	}
}

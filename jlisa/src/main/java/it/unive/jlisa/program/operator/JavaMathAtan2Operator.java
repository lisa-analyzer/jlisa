package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaMathAtan2Operator implements BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaMathAtan2Operator INSTANCE = new JavaMathAtan2Operator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaMathAtan2Operator() {
	}

	@Override
	public String toString() {
		return "atan2";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		return Collections.singleton(JavaDoubleType.INSTANCE);

	}
}

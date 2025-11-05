package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaMathRoundOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaMathRoundOperator INSTANCE = new JavaMathRoundOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaMathRoundOperator() {
	}

	@Override
	public String toString() {
		return "round";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().anyMatch(t -> t.equals(JavaDoubleType.INSTANCE)))
			return Collections.singleton(JavaLongType.INSTANCE);
		if (argument.stream().anyMatch(t -> t.equals(JavaFloatType.INSTANCE)))
			return Collections.singleton(JavaIntType.INSTANCE);
		return Collections.emptySet();
	}
}

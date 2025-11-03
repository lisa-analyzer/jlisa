package it.unive.jlisa.program.operator;

import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaMathMin implements BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaMathMin INSTANCE = new JavaMathMin();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaMathMin() {
	}

	@Override
	public String toString() {
		return "min";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (left.stream().noneMatch(Type::isNumericType) || right.stream().noneMatch(Type::isNumericType))
			return Collections.emptySet();
		Set<Type> set = NumericType.commonNumericalType(left, right);
		if (set.isEmpty())
			return Collections.emptySet();
		return set;
	}
}

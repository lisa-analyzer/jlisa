package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaCharType;
import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaStringIndexOfCharFromIndexOperator implements StringOperator, TernaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringIndexOfCharFromIndexOperator INSTANCE = new JavaStringIndexOfCharFromIndexOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringIndexOfCharFromIndexOperator() {
	}

	@Override
	public String toString() {
		return "strindexof";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> middle,
			Set<Type> right) {
		if (left.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		if (middle.stream().noneMatch(t -> t.equals(JavaCharType.INSTANCE)))
			return Collections.emptySet();
		if (right.stream().noneMatch(t -> t.equals(types.getIntegerType())))
			return Collections.emptySet();
		return Collections.singleton(types.getIntegerType());
	}

}

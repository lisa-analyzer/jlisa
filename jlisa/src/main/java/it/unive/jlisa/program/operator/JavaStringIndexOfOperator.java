package it.unive.jlisa.program.operator;

import it.unive.lisa.symbolic.value.operator.binary.StringOperation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaStringIndexOfOperator extends StringOperation {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringIndexOfOperator INSTANCE = new JavaStringIndexOfOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringIndexOfOperator() {
	}

	@Override
	public String toString() {
		return "strindexof";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (left.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		if (right.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		return Collections.singleton(types.getIntegerType());
	}

	@Override
	protected Type resultType(
			TypeSystem types) {
		return types.getIntegerType();
	}

}

package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaStringInsertCharArrayOperator implements TernaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringInsertCharArrayOperator INSTANCE = new JavaStringInsertCharArrayOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringInsertCharArrayOperator() {
	}

	@Override
	public String toString() {
		return "string-insertchararray";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> middle,
			Set<Type> right) {
		if (middle.stream().noneMatch(t -> t.equals(JavaIntType.INSTANCE)))
			return Collections.emptySet();
		if (right.stream().noneMatch(t -> t.equals(JavaArrayType.CHAR_ARRAY)))
			return Collections.emptySet();
		return Collections.singleton(types.getStringType());
	}

}
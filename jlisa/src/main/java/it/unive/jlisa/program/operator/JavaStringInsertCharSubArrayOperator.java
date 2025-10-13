package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaStringInsertCharSubArrayOperator implements NaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringInsertCharSubArrayOperator INSTANCE = new JavaStringInsertCharSubArrayOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringInsertCharSubArrayOperator() {
	}

	@Override
	public String toString() {
		return "string-insertcharsubarray";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type>[] operands) {
		if (operands[1].stream().noneMatch(t -> t.equals(JavaIntType.INSTANCE)))
			return Collections.emptySet();
		if (operands[2].stream().noneMatch(t -> t.equals(JavaArrayType.CHAR_ARRAY)))
			return Collections.emptySet();
		if (operands[3].stream().noneMatch(t -> t.equals(JavaIntType.INSTANCE)))
			return Collections.emptySet();
		if (operands[4].stream().noneMatch(t -> t.equals(JavaIntType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(types.getStringType());
	}

}
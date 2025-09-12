package it.unive.jlisa.program.operator;

import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaStringConcatOperator extends StringConcat {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringConcatOperator INSTANCE = new JavaStringConcatOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringConcatOperator() {
	}

	@Override
	public String toString() {
		return "strcat";
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
		return Collections.singleton(types.getStringType());
	}

	@Override
	protected Type resultType(
			TypeSystem types) {
		return types.getStringType();
	}

}
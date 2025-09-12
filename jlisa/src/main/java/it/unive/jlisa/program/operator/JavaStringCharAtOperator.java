package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaCharType;
import it.unive.lisa.symbolic.value.operator.binary.StringOperation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaStringCharAtOperator extends StringOperation {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringCharAtOperator INSTANCE = new JavaStringCharAtOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringCharAtOperator() {
	}

	@Override
	public String toString() {
		return "strcharat";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (left.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		return Collections.singleton(JavaCharType.INSTANCE);
	}

	@Override
	protected Type resultType(
			TypeSystem types) {
		return JavaCharType.INSTANCE;
	}
}
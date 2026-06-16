package it.unive.jlisa.program.operator;

import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaFieldSetClassNameOperator implements StringOperator, UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaFieldSetClassNameOperator INSTANCE = new JavaFieldSetClassNameOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaFieldSetClassNameOperator() {
	}

	@Override
	public String toString() {
		return "field-set-class-name";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		return Collections.singleton(types.getStringType());
	}
}


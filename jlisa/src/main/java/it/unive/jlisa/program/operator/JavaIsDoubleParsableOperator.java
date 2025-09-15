package it.unive.jlisa.program.operator;

import java.util.Collections;
import java.util.Set;

import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaIsDoubleParsableOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaIsDoubleParsableOperator INSTANCE = new JavaIsDoubleParsableOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaIsDoubleParsableOperator() {
	}

	@Override
	public String toString() {
		return "is-double-parsable";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		return Collections.singleton(JavaBooleanType.INSTANCE);
	}

}

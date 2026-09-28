package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaClassForNameOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaClassForNameOperator INSTANCE = new JavaClassForNameOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaClassForNameOperator() {
	}

	@Override
	public String toString() {
		return "class-for-name";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(arg -> arg.equals(types.getStringType()))) {
			return Collections.emptySet();
		}
		return Collections.singleton(JavaClassType.getStringType());
	}

}

package it.unive.jlisa.program.operator;

import java.util.Collections;
import java.util.Set;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaStringToUpperCase implements
StringOperator,
UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringToUpperCase INSTANCE = new JavaStringToUpperCase();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringToUpperCase() {
	}

	@Override
	public String toString() {
		return "strupper";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(JavaClassType.lookup("String", null))))
			return Collections.emptySet();
		return Collections.singleton(types.getIntegerType());
	}

}

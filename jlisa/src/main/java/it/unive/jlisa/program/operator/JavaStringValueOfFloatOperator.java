package it.unive.jlisa.program.operator;

import java.util.Collections;
import java.util.Set;

import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaStringValueOfFloatOperator implements StringOperator, UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringValueOfFloatOperator INSTANCE = new JavaStringValueOfFloatOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringValueOfFloatOperator() {
	}

	@Override
	public String toString() {
		return "strvalueof";
	}
		
	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(JavaFloatType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(types.getStringType());
	}
}
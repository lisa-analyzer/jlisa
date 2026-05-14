package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaClassNewInstanceOperator implements StringOperator, UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaClassNewInstanceOperator INSTANCE = new JavaClassNewInstanceOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaClassNewInstanceOperator() {
	}

	@Override
	public String toString() {
		return "class-new-instance";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(JavaClassType.getClassMetaType())))
			return Collections.emptySet();
		return Collections.singleton(JavaClassType.getObjectType());
	}
}

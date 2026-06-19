package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaClassGetFieldOperator implements BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaClassGetFieldOperator INSTANCE = new JavaClassGetFieldOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaClassGetFieldOperator() {
	}

	@Override
	public String toString() {
		return "getField";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (left.stream().noneMatch(t -> t.equals(new JavaReferenceType(JavaClassType.getStringType()))))
			return Collections.emptySet();
		if (right.stream().noneMatch(t -> t.equals(new JavaReferenceType(JavaClassType.getStringType()))))
			return Collections.emptySet();

		return Collections.singleton(new JavaReferenceType(JavaClassType.getFieldMetaType()));
	}

}

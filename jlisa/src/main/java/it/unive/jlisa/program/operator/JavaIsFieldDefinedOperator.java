package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaIsFieldDefinedOperator implements BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaIsFieldDefinedOperator INSTANCE = new JavaIsFieldDefinedOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaIsFieldDefinedOperator() {
	}

	@Override
	public String toString() {
		return "isFieldDefined";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		JavaClassType stringType = JavaClassType.getStringType();

		if (left.stream().noneMatch(t -> t.equals(stringType)))
			return Collections.emptySet();
		if (right.stream().noneMatch(t -> t.equals(stringType)))
			return Collections.emptySet();
		return Collections.singleton(types.getBooleanType());
	}

}

package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaStringAppendFloatOperator implements BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringAppendFloatOperator INSTANCE = new JavaStringAppendFloatOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringAppendFloatOperator() {
	}

	@Override
	public String toString() {
		return "floatappend";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (right.stream().noneMatch(t -> t.equals(JavaFloatType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(types.getStringType());
	}

}
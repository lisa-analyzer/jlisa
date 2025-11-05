package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaByteToUnsignedLongOperator
		implements
		StringOperator,
		UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaByteToUnsignedLongOperator INSTANCE = new JavaByteToUnsignedLongOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaByteToUnsignedLongOperator() {
	}

	@Override
	public String toString() {
		return "bytetounsignedlong";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(JavaByteType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(JavaLongType.INSTANCE);
	}

}

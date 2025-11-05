package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaLongByteValueOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaLongByteValueOperator INSTANCE = new JavaLongByteValueOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaLongByteValueOperator() {
	}

	@Override
	public String toString() {
		return "long-byte-value";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(JavaLongType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(JavaByteType.INSTANCE);
	}
}

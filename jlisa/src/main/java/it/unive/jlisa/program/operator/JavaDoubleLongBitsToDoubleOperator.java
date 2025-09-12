package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaDoubleLongBitsToDoubleOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaDoubleLongBitsToDoubleOperator INSTANCE = new JavaDoubleLongBitsToDoubleOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaDoubleLongBitsToDoubleOperator() {
	}

	@Override
	public String toString() {
		return "double-to-raw-long-bits";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.canBeAssignedTo(JavaLongType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(JavaDoubleType.INSTANCE);
	}

}

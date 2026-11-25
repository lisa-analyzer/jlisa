package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * Whether a string is the decimal representation of an {@code int}, as accepted
 * by {@link Integer#parseInt(String)}.
 */
public class JavaIsIntParsableOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaIsIntParsableOperator INSTANCE = new JavaIsIntParsableOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaIsIntParsableOperator() {
	}

	@Override
	public String toString() {
		return "is-int-parsable";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		return Collections.singleton(JavaBooleanType.INSTANCE);
	}

}

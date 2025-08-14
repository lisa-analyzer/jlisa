package it.unive.jlisa.program.operator;

import java.util.Collections;
import java.util.Set;

import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaCharacterIsDigitOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaCharacterIsDigitOperator INSTANCE = new JavaCharacterIsDigitOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaCharacterIsDigitOperator() {
	}

	@Override
	public String toString() {
		return "isDigit";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(JavaCharType.INSTANCE) || t.equals(JavaIntType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(types.getBooleanType());
	}

}

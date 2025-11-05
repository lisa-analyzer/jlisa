package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaIntToBinaryStringOperator
		implements
		StringOperator,
		UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaIntToBinaryStringOperator INSTANCE = new JavaIntToBinaryStringOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaIntToBinaryStringOperator() {
	}

	@Override
	public String toString() {
		return "inttobinarystring";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(JavaIntType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(types.getStringType());
	}

}

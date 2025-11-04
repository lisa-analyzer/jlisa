package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaFloatIsInfiniteOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaFloatIsInfiniteOperator INSTANCE = new JavaFloatIsInfiniteOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaFloatIsInfiniteOperator() {
	}

	@Override
	public String toString() {
		return "float-is-infinite";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(JavaFloatType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(types.getBooleanType());
	}

}

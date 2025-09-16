package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaFloatParseFloatOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaFloatParseFloatOperator INSTANCE = new JavaFloatParseFloatOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaFloatParseFloatOperator() {
	}

	@Override
	public String toString() {
		return "float-parse-float";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		return Collections.singleton(JavaFloatType.INSTANCE);
	}

}

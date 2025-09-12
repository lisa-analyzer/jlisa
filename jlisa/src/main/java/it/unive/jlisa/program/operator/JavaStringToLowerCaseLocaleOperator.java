package it.unive.jlisa.program.operator;

import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaStringToLowerCaseLocaleOperator extends StringEquals {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringToLowerCaseLocaleOperator INSTANCE = new JavaStringToLowerCaseLocaleOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringToLowerCaseLocaleOperator() {
	}

	@Override
	public String toString() {
		return "strlower";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (left.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		// TODO: implement chech for "Locale" type
		return Collections.singleton(types.getBooleanType());
	}

	@Override
	protected Type resultType(
			TypeSystem types) {
		// TODO: understant how to type "Locale"
		return types.getBooleanType();
	}
}
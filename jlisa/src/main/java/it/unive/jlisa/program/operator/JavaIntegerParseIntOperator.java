package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * The {@code int} represented by a string, as by
 * {@link Integer#parseInt(String)}.
 */
public class JavaIntegerParseIntOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaIntegerParseIntOperator INSTANCE = new JavaIntegerParseIntOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaIntegerParseIntOperator() {
	}

	@Override
	public String toString() {
		return "integer-parse-int";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		return Collections.singleton(JavaIntType.INSTANCE);
	}

}

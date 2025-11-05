package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaNumberIntValueOperator implements UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaNumberIntValueOperator INSTANCE = new JavaNumberIntValueOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaNumberIntValueOperator() {
	}

	@Override
	public String toString() {
		return "intvalue";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(JavaLongType.INSTANCE) || t.equals(JavaIntType.INSTANCE)
				|| t.equals(JavaDoubleType.INSTANCE) || t.equals(JavaShortType.INSTANCE)
				|| t.equals(JavaFloatType.INSTANCE) || t.equals(JavaByteType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(types.getIntegerType());
	}
}

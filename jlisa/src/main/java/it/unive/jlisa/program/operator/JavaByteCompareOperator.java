package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaByteCompareOperator extends StringConcat {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaByteCompareOperator INSTANCE = new JavaByteCompareOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaByteCompareOperator() {
	}

	@Override
	public String toString() {
		return "byte-compare";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (left.stream().noneMatch(t -> t.equals(JavaByteType.INSTANCE)))
			return Collections.emptySet();
		if (right.stream().noneMatch(t -> t.equals(JavaByteType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(JavaIntType.INSTANCE);
	}

	@Override
	protected Type resultType(
			TypeSystem types) {
		return JavaIntType.INSTANCE;
	}

}
package it.unive.jlisa.program.operator;

import java.util.Collections;
import java.util.Set;

import it.unive.jlisa.program.type.JavaCharType;
import it.unive.lisa.symbolic.value.operator.binary.StringOperation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaStringCharAt extends StringOperation {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringCharAt INSTANCE = new JavaStringCharAt();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringCharAt() {
	}

	@Override
	public String toString() {
		return "strcharat";
	}

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
		return Collections.singleton(JavaCharType.INSTANCE);
	}
	
	@Override
	protected Type resultType(
			TypeSystem types) {
		return JavaCharType.INSTANCE;
	}
}
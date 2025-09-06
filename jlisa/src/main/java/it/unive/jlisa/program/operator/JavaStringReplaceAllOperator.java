package it.unive.jlisa.program.operator;

import java.util.Collections;
import java.util.Set;

import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaStringReplaceAllOperator implements StringOperator,TernaryOperator{

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringReplaceAllOperator INSTANCE = new JavaStringReplaceAllOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringReplaceAllOperator() {
	}

	@Override
	public String toString() {
		return "strreplaceall";
	}
	
	
	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> middle, Set<Type> right) {
		if (left.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		if (middle.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		if (right.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		return Collections.singleton(types.getStringType());
	}

}

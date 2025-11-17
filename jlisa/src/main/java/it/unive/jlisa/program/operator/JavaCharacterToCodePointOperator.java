package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaCharacterToCodePointOperator implements BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaCharacterToCodePointOperator INSTANCE = new JavaCharacterToCodePointOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaCharacterToCodePointOperator() {
	}

	@Override
	public String toString() {
		return "char-to-codepoint";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (left.stream().noneMatch(t -> t.equals(JavaCharType.INSTANCE)))
			return Collections.emptySet();
		if (right.stream().noneMatch(t -> t.equals(JavaCharType.INSTANCE)))
			return Collections.emptySet();
		return Collections.singleton(JavaIntType.INSTANCE);
	}

}
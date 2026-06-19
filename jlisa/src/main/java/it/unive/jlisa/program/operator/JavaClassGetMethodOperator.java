package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaClassGetMethodOperator implements NaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaClassGetMethodOperator INSTANCE = new JavaClassGetMethodOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaClassGetMethodOperator() {
	}

	@Override
	public String toString() {
		return "class-get-method";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type>[] operands) {
		// operands[0]: the Class object (receiver)
		// operands[1]: the method name (String)
		// operands[2]: the parameter types (Class[])
		if (operands[0].stream().noneMatch(t -> t.equals(new JavaReferenceType(JavaClassType.getClassMetaType()))))
			return Collections.emptySet();
		if (operands[1].stream().noneMatch(t -> t.isStringType()))
			return Collections.emptySet();

		if (operands[2].stream().noneMatch(t -> t.equals(JavaArrayType.CLASS_ARRAY)))
			return Collections.emptySet();

		return Collections.singleton(new JavaReferenceType(JavaClassType.getMethodType()));
	}

}

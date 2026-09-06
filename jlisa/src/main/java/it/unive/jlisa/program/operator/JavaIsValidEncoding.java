package it.unive.jlisa.program.operator;

import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaIsValidEncoding implements UnaryOperator {

	public static final JavaIsValidEncoding INSTANCE = new JavaIsValidEncoding();

	protected JavaIsValidEncoding() {
	}

	@Override
	public String toString() {
		return "isValidEncoding";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> arg) {
		if (arg.stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		return Collections.singleton(types.getBooleanType());
	}

}


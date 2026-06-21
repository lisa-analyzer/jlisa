package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class GhostGetMethodParameterCountOperator implements UnaryOperator {

	public static final GhostGetMethodParameterCountOperator INSTANCE = new GhostGetMethodParameterCountOperator();

	protected GhostGetMethodParameterCountOperator() {
	}

	@Override
	public String toString() {
		return "getmethod-param-count";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(t -> t.equals(types.getIntegerType())))
			return Collections.emptySet();
		return Collections.singleton(JavaBooleanType.INSTANCE);
	}
}

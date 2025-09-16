package it.unive.jlisa.analysis.type;

import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaInferredTypes extends InferredTypes{

	@Override
	public TypeSet evalValueExpression(ValueExpression expression, TypeSet[] subExpressions, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		TypeSystem types = pp.getProgram().getTypes();
		@SuppressWarnings("unchecked")
		Set<Type>[] result = (Set<Type>[]) new Set[subExpressions.length];
		for( int i = 0; i < subExpressions.length; ++i) {
			result[i] = subExpressions[i].isTop ? types.getTypes() : subExpressions[i].elements;
		}
		Set<Type> inferred = ((it.unive.jlisa.program.operator.NaryExpression) expression).getOperator().typeInference(types, result);
		if (inferred.isEmpty())
			return TypeSet.BOTTOM;
		return new TypeSet(types, inferred);
	}
	
}

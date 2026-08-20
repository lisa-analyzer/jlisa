package it.unive.jlisa.analysis.type;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;
import java.util.stream.Collectors;

public class JavaInferredTypes extends InferredTypes {

	@Override
	public TypeSet evalValueExpression(
			ValueExpression expression,
			TypeSet[] subExpressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TypeSystem types = pp.getProgram().getTypes();
		@SuppressWarnings("unchecked")
		Set<Type>[] result = (Set<Type>[]) new Set[subExpressions.length];
		for (int i = 0; i < subExpressions.length; ++i) {
			result[i] = subExpressions[i].isTop ? types.getTypes() : subExpressions[i].elements;
		}
		Set<Type> inferred = ((it.unive.jlisa.program.operator.NaryExpression) expression).getOperator()
				.typeInference(types, result);
		if (inferred.isEmpty())
			return TypeSet.BOTTOM;
		return new TypeSet(types, inferred);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			TypeSet left,
			TypeSet right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() != TypeCheck.INSTANCE || left.isTop())
			// when left is top there is no concrete null case to special-case
			return super.satisfiesBinaryExpression(expression, left, right, pp, oracle);

		// the parent implementation answers TypeCheck (instanceof) by
		// delegating to TypeCast semantics, under which null is always
		// castable; that makes it wrongly conclude "x instanceof T" is a
		// tautology whenever null is merely one of x's possible (concretely
		// tracked) types, even though "null instanceof T" is always false in
		// Java (unlike a cast of null, which never throws). Strip null out
		// before delegating to the parent's cast-based logic.
		boolean mayBeNull = left.elements.stream()
				.anyMatch(t -> t.isReferenceType() && t.asReferenceType().getInnerType().isNullType());
		if (!mayBeNull)
			return super.satisfiesBinaryExpression(expression, left, right, pp, oracle);

		Set<Type> nonNull = left.elements.stream()
				.filter(t -> !(t.isReferenceType() && t.asReferenceType().getInnerType().isNullType()))
				.collect(Collectors.toSet());
		if (nonNull.isEmpty())
			// the only possible value is null: "x instanceof T" is always
			// false
			return Satisfiability.NOT_SATISFIED;

		Satisfiability sat = super.satisfiesBinaryExpression(expression, new TypeSet(pp.getProgram().getTypes(),
				nonNull), right, pp, oracle);
		// null is one of several possibilities, and it never satisfies
		// instanceof: even if the non-null types would make this a
		// tautology, we cannot conclude SATISFIED overall
		return sat == Satisfiability.SATISFIED ? Satisfiability.UNKNOWN : sat;
	}

}

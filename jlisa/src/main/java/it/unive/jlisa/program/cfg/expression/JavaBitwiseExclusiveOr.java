package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class JavaBitwiseExclusiveOr extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	public JavaBitwiseExclusiveOr(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "^", inferType(left, right), left, right);
	}

	private static Type inferType(
			Expression left,
			Expression right) {
		Type leftType = left.getStaticType();
		Type rightType = right.getStaticType();

		if (leftType.isNumericType() && rightType.isNumericType()) {
			return leftType;
		} else
			return Untyped.INSTANCE;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		if (analysis.getRuntimeTypesOf(state, left, this).stream().noneMatch(Type::isNumericType))
			return state.bottomExecution();
		if (analysis.getRuntimeTypesOf(state, right, this).stream().noneMatch(Type::isNumericType))
			return state.bottomExecution();

		return analysis.smallStepSemantics(
				state,
				new it.unive.lisa.symbolic.value.BinaryExpression(
						Untyped.INSTANCE,
						left,
						right,
						it.unive.lisa.symbolic.value.operator.binary.BitwiseXor.INSTANCE,
						getLocation()),
				this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

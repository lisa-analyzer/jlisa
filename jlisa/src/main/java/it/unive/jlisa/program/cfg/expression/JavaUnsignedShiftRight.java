package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaLongType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class JavaUnsignedShiftRight extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	public JavaUnsignedShiftRight(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, ">>>", inferType(left, right), left, right);
	}

	private static Type inferType(
			Expression left,
			Expression right) {
		Type leftType = left.getStaticType();

		NumericType integerType = left.getProgram().getTypes().getIntegerType();
		if (leftType.canBeAssignedTo(integerType))
			return integerType;

		if (leftType.canBeAssignedTo(JavaLongType.INSTANCE))
			return JavaLongType.INSTANCE;

		else
			return Untyped.INSTANCE;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		if (analysis.getRuntimeTypesOf(state, right, this).stream().noneMatch(t -> t.isNumericType()))
			return state.bottomExecution();

		if (analysis.getRuntimeTypesOf(state, left, this).stream()
				.noneMatch(t -> t.canBeAssignedTo(getProgram().getTypes().getIntegerType())
						|| t.canBeAssignedTo(JavaLongType.INSTANCE)))
			return state.bottomExecution();

		return analysis.smallStepSemantics(
				state,
				new it.unive.lisa.symbolic.value.BinaryExpression(
						Untyped.INSTANCE,
						left,
						right,
						it.unive.lisa.symbolic.value.operator.binary.BitwiseUnsignedShiftRight.INSTANCE,
						getLocation()),
				this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

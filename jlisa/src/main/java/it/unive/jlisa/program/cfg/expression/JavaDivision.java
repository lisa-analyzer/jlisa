package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.continuations.Exception;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;

public class JavaDivision extends Division {

	public JavaDivision(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, left, right);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression left,
			SymbolicExpression right, StatementStore<A> expressions) throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		BinaryExpression expr = new BinaryExpression(
				getCFG().getProgram().getTypes().getBooleanType(), 
				right, 
				new Constant(getCFG().getProgram().getTypes().getIntegerType(), 0, getLocation()), 
				ComparisonEq.INSTANCE, 
				getLocation());

		if (analysis.satisfies(state, expr, this) == Satisfiability.SATISFIED) {
			JavaClassType arithException = JavaClassType.lookup("ArithmeticException", null);
			return analysis.moveExecutionToError(state, new Exception(arithException, this));
		} else if (analysis.satisfies(state, expr, this) == Satisfiability.NOT_SATISFIED)
			return super.fwdBinarySemantics(interprocedural, state, left, right, expressions);
		else {
			JavaClassType arithException = JavaClassType.lookup("ArithmeticException", null);
			AnalysisState<A> exceptionState = analysis.moveExecutionToError(state, new Exception(arithException, this));
			AnalysisState<A> noExceptionState = super.fwdBinarySemantics(interprocedural, state, left, right, expressions);
			return exceptionState.lub(noExceptionState);
		}

	}
}
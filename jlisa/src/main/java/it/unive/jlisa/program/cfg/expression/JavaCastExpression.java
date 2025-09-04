package it.unive.jlisa.program.cfg.expression;

import java.util.Collections;

import it.unive.jlisa.program.type.JavaReferenceType;
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
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;

public class JavaCastExpression extends UnaryExpression {

	private final Type type;

	public JavaCastExpression(CFG cfg, CodeLocation location, Expression subExpression, Type type) {
		super(cfg, location, "cast", subExpression);
		this.type = type.isInMemoryType() ? new JavaReferenceType(type) : type;
	}

	@Override
	public <A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
		Constant typeConv = new Constant(new TypeTokenType(Collections.singleton(type)), type, getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		
		AnalysisState<A> exprState = analysis.smallStepSemantics(state, expr, this);
		AnalysisState<A> result = state.bottom();
		
		for (SymbolicExpression exp : exprState.getExecutionExpressions()) {
			BinaryExpression castExpression =  new BinaryExpression(type, exp, typeConv, TypeCast.INSTANCE, getLocation());
			result = result.lub(analysis.smallStepSemantics(exprState, castExpression, this));
		}
		
		return result;
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return "(" + type+ ") " + getSubExpression();
	}
}

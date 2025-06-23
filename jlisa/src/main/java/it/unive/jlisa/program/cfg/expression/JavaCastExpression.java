package it.unive.jlisa.program.cfg.expression;

import java.util.Collections;

import it.unive.lisa.analysis.AbstractState;
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
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;

public class JavaCastExpression extends UnaryExpression {

	private final Type type;

	public JavaCastExpression(CFG cfg, CodeLocation location, Expression subExpression, Type type) {
		super(cfg, location, "cast", subExpression);
		this.type = type.isInMemoryType() ? new ReferenceType(type) : type;
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
		Constant typeConv = new Constant(new TypeTokenType(Collections.singleton(type)), type, getLocation());

		AnalysisState<A> exprState = state.smallStepSemantics(expr, this);
		AnalysisState<A> result = state.bottom();
		
		for (SymbolicExpression exp : exprState.getComputedExpressions()) {
			BinaryExpression castExpression =  new BinaryExpression(type, exp, typeConv, TypeCast.INSTANCE, getLocation());
			result = result.lub(exprState.smallStepSemantics(castExpression, this));
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

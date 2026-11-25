package it.unive.jlisa.program.java.constructs.floatw;

import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.jlisa.program.operator.JavaFloatParseFloatOperator;
import it.unive.jlisa.program.operator.JavaIsFloatParsableOperator;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class FloatParseFloat extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public FloatParseFloat(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "parseFloat", expr);
	}

	public static FloatParseFloat build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FloatParseFloat(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefExpr = new HeapDereference(stringType, expr, getLocation());
		AccessChild accessExpr = new AccessChild(stringType, derefExpr, var, getLocation());

		// need to check for NumberFormatExcetion
		it.unive.lisa.symbolic.value.UnaryExpression isParsable = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaLongType.INSTANCE,
				accessExpr,
				JavaIsFloatParsableOperator.INSTANCE,
				getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		// NumberFormatException exactly where the string is not parsable, and
		// the number is computed in the other executions
		JavaExceptions.Raising<A, D> raising = JavaExceptions.Raising.ofNative(interprocedural, this, originating,
				getSubExpressions());
		AnalysisState<A> ok = raising.raiseWhere(
				new it.unive.lisa.symbolic.value.UnaryExpression(JavaBooleanType.INSTANCE, isParsable,
						LogicalNegation.INSTANCE, getLocation()),
				true, state, "java.lang.NumberFormatException");
		if (ok.getExecutionState().isBottom())
			return raising.moveFrom(ok.bottomExecution());
		it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaLongType.INSTANCE,
				accessExpr,
				JavaFloatParseFloatOperator.INSTANCE,
				getLocation());

		return raising.moveFrom(analysis.smallStepSemantics(ok, un, context, originating));
	}
}

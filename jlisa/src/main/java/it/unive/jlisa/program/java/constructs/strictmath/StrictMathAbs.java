package it.unive.jlisa.program.java.constructs.strictmath;

import it.unive.jlisa.program.operator.JavaStrictMathAbsOperator;
import it.unive.jlisa.program.type.JavaClassType;
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
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;

public class StrictMathAbs extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public StrictMathAbs(
			CFG cfg,
			CodeLocation location,
			Expression arg) {
		super(cfg, location, "strictabs", arg);
	}

	public static StrictMathAbs build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StrictMathAbs(cfg, location, params[0]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		Set<Type> types = analysis.getRuntimeTypesOf(state, expr, originating);

		AnalysisState<A> result = state.bottomExecution();

		for (Type t : types) {
			Type primType;
			SymbolicExpression exprToAbs = expr;
			if (t.isReferenceType()
					&& (primType = JavaClassType.getUnwrappedType(t.asReferenceType().getInnerType())) != null) {
				// unboxing
				GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
				HeapDereference derefLeft = new HeapDereference(primType, expr, getLocation());
				exprToAbs = new AccessChild(primType, derefLeft, var, getLocation());
			}

			UnaryExpression abs = new UnaryExpression(
					Untyped.INSTANCE,
					exprToAbs,
					JavaStrictMathAbsOperator.INSTANCE,
					getLocation());
			result = result.lub(analysis.smallStepSemantics(state, abs, originating));
		}

		return result;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

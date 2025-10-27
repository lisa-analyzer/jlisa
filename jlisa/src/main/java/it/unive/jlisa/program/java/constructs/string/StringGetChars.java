package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Untyped;

public class StringGetChars extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	protected StringGetChars(
			CFG cfg,
			CodeLocation location,
			Expression[] subExpressions) {
		super(cfg, location, "getChars", subExpressions);
	}

	public static StringGetChars build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringGetChars(cfg, location, params);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		SymbolicExpression[] exprs = new SymbolicExpression[params.length];

		for (int i = 0; i < params.length; ++i) {
			ExpressionSet set = params[i];
			if (set.size() > 1 || set.size() <= 0)
				throw new IllegalArgumentException("Number of operands is incorrect!");
			for (SymbolicExpression expr : set) {
				exprs[i] = expr;
			}
		}

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		// TODO: implements semantics
		AccessChild leftAccess = new AccessChild(JavaArrayType.CHAR_ARRAY, exprs[3], var, getLocation());
		PushAny top = new PushAny(JavaArrayType.CHAR_ARRAY, getLocation());
		return interprocedural.getAnalysis().assign(state, leftAccess, top, originating);
	}
}

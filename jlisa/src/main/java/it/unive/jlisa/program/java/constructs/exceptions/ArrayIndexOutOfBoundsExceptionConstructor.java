package it.unive.jlisa.program.java.constructs.exceptions;

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
import it.unive.lisa.symbolic.value.PushAny;

public class ArrayIndexOutOfBoundsExceptionConstructor extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public ArrayIndexOutOfBoundsExceptionConstructor(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		super(cfg, location, "ArrayIndexOutOfBoundsException", params);
	}

	public static ArrayIndexOutOfBoundsExceptionConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		if (params.length == 1)
			return new ArrayIndexOutOfBoundsExceptionConstructor(cfg, location, params[0]);
		else
			return new ArrayIndexOutOfBoundsExceptionConstructor(cfg, location, params[0], params[1]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(getStaticType(), getLocation()),
				originating);
	}
}
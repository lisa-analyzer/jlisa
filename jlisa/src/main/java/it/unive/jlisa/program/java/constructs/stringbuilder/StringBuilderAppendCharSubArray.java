package it.unive.jlisa.program.java.constructs.stringbuilder;

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
import it.unive.lisa.type.Type;

public class StringBuilderAppendCharSubArray extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringBuilderAppendCharSubArray(
			CFG cfg,
			CodeLocation location,
			Expression p0,
			Expression p1,
			Expression p2,
			Expression p3) {
		super(cfg, location, "append", p0, p1, p2, p3);
	}

	public static StringBuilderAppendCharSubArray build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringBuilderAppendCharSubArray(cfg, location, params[0], params[1], params[2], params[3]);
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

		Type stringType = getProgram().getTypes().getStringType();

		return interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(stringType, getLocation()),
				originating);
	}
}

package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;

public class StringRegionMatches extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	protected StringRegionMatches(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "regionMatches", left, right);
	}

	public static StringRegionMatches build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringRegionMatches(cfg, location, params[0], params[1]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(state,
				new PushAny(JavaBooleanType.INSTANCE, getLocation()), originating);
	}
}

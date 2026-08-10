package it.unive.jlisa.program.java.constructs.sound;

import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.PushAny;

public class SourceDataLineWrite
		extends
		NaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public SourceDataLineWrite(
			CFG cfg,
			CodeLocation location,
			Expression receiver,
			Expression array,
			Expression offset,
			Expression length) {
		super(cfg, location, "SourceDataLineWrite", receiver, array, offset, length);
	}

	public static SourceDataLineWrite build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new SourceDataLineWrite(cfg, location, params[0], params[1], params[2], params[3]);
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
		return interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(JavaIntType.INSTANCE, getLocation()),
				originating);
	}

}

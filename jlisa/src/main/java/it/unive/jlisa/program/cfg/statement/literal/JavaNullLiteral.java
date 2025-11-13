package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.type.NullType;

public class JavaNullLiteral extends Literal<Object> {

	public JavaNullLiteral(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, null, new JavaReferenceType(NullType.INSTANCE));
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions)
			throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(entryState, new NullConstant(getLocation()), this);
	}
}

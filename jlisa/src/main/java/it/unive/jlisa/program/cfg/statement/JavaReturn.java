package it.unive.jlisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.Identifier;

public class JavaReturn
		extends
		Return {

	public JavaReturn(
			CFG cfg,
			CodeLocation location,
			Expression expression) {
		super(cfg, location, expression);
	}

	@Override
	public Statement withValue(
			Expression value) {
		return new JavaReturn(getCFG(), getLocation(), value);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		Identifier meta = getMetaVariable();
		if (expr.getStaticType().isReferenceType() && !(expr instanceof HeapReference))
			expr = new HeapReference(getProgram().getTypes().getReference(expr.getStaticType()), expr, expr.getCodeLocation());
		return interprocedural.getAnalysis().assign(state, meta, expr, this);
	}
}

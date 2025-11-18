package it.unive.jlisa.program.cfg.expression.instrumentations;

import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;

/**
 * Instrumentation to get next element from collection involved in for-each
 * structure
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class GetNextForEach extends it.unive.lisa.program.cfg.statement.UnaryExpression {

	/**
	 * Builds the construct.
	 * 
	 * @param cfg        the cfg containing this expression
	 * @param location   the location where this construct is defined
	 * @param program    the program of the analysis
	 * @param collection the collection/array to get the next element
	 */
	public GetNextForEach(
			CFG cfg,
			CodeLocation location,
			Expression collection) {
		super(cfg, location, "getNextElementForEach", collection.getStaticType(), collection);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		// TODO: to implement semantics
		Type containerType = expr.getStaticType().asReferenceType().getInnerType();
		Type contentType = containerType.asArrayType().getInnerType();
		if (contentType.isInMemoryType())
			contentType = new JavaReferenceType(contentType);

		PushAny top = new PushAny(contentType, getLocation());
		return interprocedural.getAnalysis().smallStepSemantics(state, top, this);
	}
}

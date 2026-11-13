package it.unive.jlisa.program.cfg.expression.instrumentations;

import java.util.List;
import java.util.stream.Stream;

import it.unive.jlisa.program.ConstraintUtils;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.Type;

/**
 * Instrumentation to compute if there is a next element within the collection
 * involved in for-each structure
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class HasNextForEach extends it.unive.lisa.program.cfg.statement.UnaryExpression {

	private String ghostIdxName;
	private String ghostLenName;

	/**
	 * Builds the construct.
	 * 
	 * @param cfg        the cfg containing this expression
	 * @param location   the location where this construct is defined
	 * @param program    the program of the analysis
	 * @param collection the collection/array to get the next element
	 */
	public HasNextForEach(
			CFG cfg,
			CodeLocation location,
			Expression collection,
			String ghostIdxName,
			String ghostLenName) {
		super(cfg, location, "hasNextElementForEach", JavaBooleanType.INSTANCE, collection);
		this.ghostIdxName = ghostIdxName;
		this.ghostLenName = ghostLenName;
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

		CodeLocation loc = getLocation();
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		if (state.withExecutionExpressions(new ExpressionSet()).getExecution().isBottom())
			return state;

		Type containerType = expr.getStaticType();
		if (containerType.asReferenceType() != null
				&& containerType.asReferenceType().getInnerType() instanceof JavaArrayType) {

			// we have `$i` as iterator var
			Variable ghostIt = new Variable(JavaIntType.INSTANCE, ghostIdxName, loc);
			Variable ghostLen = new Variable(JavaIntType.INSTANCE, ghostLenName, loc);


			// if the len is constant, we can give a definite answer. Otherwise we fallback to a top boolean result indicating we may (not) have a next element.

			Stream<BinaryExpression> lengths = ConstraintUtils.extractConstraints(interprocedural, state, ghostLen, this);
			if (lengths == null)
				return topFallback(interprocedural, state, expressions);

			List<BinaryExpression> lengthsList = lengths.toList();
			if (lengthsList.size() == 1) {

				BinaryExpression lt = new BinaryExpression(JavaBooleanType.INSTANCE, ghostIt, ghostLen, ComparisonLt.INSTANCE, loc);
				Satisfiability sat = analysis.satisfies(state, lt, context, this);

				Constant resC = null;
				if (sat == Satisfiability.SATISFIED) {
					// the iterator is definitely < length
					resC = new Constant(JavaBooleanType.INSTANCE, true, loc);
				}
				else if (sat == Satisfiability.UNKNOWN
					|| sat == Satisfiability.NOT_SATISFIED) {
					// if the `<` operator returns unknown, we are surely out of bounds. This is because we know that the array has constant size (and assuming intervals as numeric domain)
					resC = new Constant(JavaBooleanType.INSTANCE, false, loc);
				}

				if (resC != null)
					return analysis.smallStepSemantics(state, resC, context, this);
			}
			else {
				return topFallback(interprocedural, state, expressions);
			}
		}

		return topFallback(interprocedural, state, expressions);
	}

	private <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> topFallback(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					StatementStore<A> expressions)
					throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(state,
				new PushAny(JavaBooleanType.INSTANCE, getLocation()), interprocedural.getCurrentContext(), this);
	}

}

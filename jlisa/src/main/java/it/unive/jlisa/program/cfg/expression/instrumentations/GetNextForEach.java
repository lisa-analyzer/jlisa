package it.unive.jlisa.program.cfg.expression.instrumentations;

import it.unive.jlisa.program.ConstraintUtils;
import it.unive.jlisa.program.cfg.expression.JavaArrayAccess;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;

import java.util.stream.Stream;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import java.util.List;
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
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;

/**
 * Instrumentation to get next element from collection involved in for-each
 * structure
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class GetNextForEach extends it.unive.lisa.program.cfg.statement.UnaryExpression {

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
	public GetNextForEach(
			CFG cfg,
			CodeLocation location,
			Expression collection,
			String ghostIdxName,
			String ghostLenName) {
		super(cfg, location, "getNextElementForEach", collection.getStaticType(), collection);
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

		// this includes the same exact check on the length of the array as `HasNextForEach`.
		// The reason is that if the guard (`HasNext`) returns top, we are in the case where the array length is not known.
		// In that case, return the top content value

		CodeLocation loc = getLocation();

		// state has expr in the execution expressions, get rid of it to check if it's bottom
		if (state.withExecutionExpressions(new ExpressionSet()).getExecution().isBottom())
			return state;

		Type containerType = expr.getStaticType();
		if (containerType.asReferenceType() != null
				&& containerType.asReferenceType().getInnerType() instanceof JavaArrayType arrType) {

			Variable ghostIt = new Variable(JavaIntType.INSTANCE, ghostIdxName, loc);
			Variable ghostLen = new Variable(JavaIntType.INSTANCE, ghostLenName, loc);

			Stream<BinaryExpression> lengths = ConstraintUtils.extractConstraints(interprocedural, state, ghostLen, this);
			if (lengths == null)
				return topFallback(interprocedural, state, expr, expressions);

			List<BinaryExpression> lengthsList = lengths.toList();
			if (lengthsList.size() == 1) {

				IntLiteral dummy = new IntLiteral(getCFG(), loc, 0);
				JavaArrayAccess accessArr = new JavaArrayAccess(getCFG(), arrType.getBaseType(), loc, getSubExpression(), dummy);
				return accessArr.fwdBinarySemantics(interprocedural, state, expr, ghostIt, expressions);
			}
			else {
				return topFallback(interprocedural, state, expr, expressions);
			}
		}

		return topFallback(interprocedural, state, expr, expressions);
	}

	private <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> topFallback(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Type containerType = expr.getStaticType().asReferenceType().getInnerType();
		Type contentType = containerType.asArrayType().getInnerType();

		PushAny top = new PushAny(contentType, getLocation());
		return interprocedural.getAnalysis().smallStepSemantics(state,
				top, interprocedural.getCurrentContext(), this);
	}
}

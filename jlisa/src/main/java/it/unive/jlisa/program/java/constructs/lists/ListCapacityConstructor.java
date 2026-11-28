package it.unive.jlisa.program.java.constructs.lists;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;

/**
 * The constructor of an empty list with an initial capacity (e.g.,
 * {@code new ArrayList<>(10)}): the capacity does not change the list, but a
 * negative one raises {@code IllegalArgumentException}.
 */
public class ListCapacityConstructor
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	/**
	 * The location of the allocation of {@code $elementData}, different from
	 * the one of the list.
	 */
	private final CodeLocation elementDataLocation;

	public ListCapacityConstructor(
			CFG cfg,
			CodeLocation location,
			Expression rec,
			Expression capacity) {
		super(cfg, location, "ArrayList", rec, capacity);
		elementDataLocation = new SyntheticCodeLocationManager("list-elementdata-" + location.toString())
				.nextLocation();
	}

	public static ListCapacityConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListCapacityConstructor(cfg, location, params[0], params[1]);
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
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();
		Constant zero = new Constant(JavaIntType.INSTANCE, 0, location);

		AnalysisState<A> result = state.bottomExecution();
		AnalysisState<A> valid = analysis.assume(state,
				new BinaryExpression(JavaBooleanType.INSTANCE, right, zero, ComparisonGe.INSTANCE, location), context,
				this, this);
		if (!valid.getExecutionState().isBottom())
			result = ListModel.init(analysis, context, valid, left, elementDataLocation, this);

		// a negative capacity raises IllegalArgumentException
		AnalysisState<A> negative = analysis.assume(state,
				new BinaryExpression(JavaBooleanType.INSTANCE, right, zero, ComparisonLt.INSTANCE, location), context,
				this, this);
		if (negative.getExecutionState().isBottom())
			return result;
		String exc = "java.lang.IllegalArgumentException";
		AnalysisState<A> raised = ListModel.raise(interprocedural, negative, exc, this, getSubExpressions(),
				expressions);
		Error error = new Error(JavaClassType.lookup(exc).getReference(), this);
		if (result.getExecutionState().isBottom())
			return analysis.moveExecutionToError(raised, error, context, this);
		return analysis.moveExecutionToError(result, raised, error, context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

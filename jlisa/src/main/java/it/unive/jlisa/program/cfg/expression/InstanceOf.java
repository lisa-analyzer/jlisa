package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

public class InstanceOf extends UnaryExpression {

	private final Type type;

	public InstanceOf(
			CFG cfg,
			CodeLocation location,
			Expression subExpression,
			Type type) {
		super(cfg, location, "instanceof", subExpression);
		this.type = new JavaReferenceType(type);
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		Set<Type> types = analysis.getRuntimeTypesOf(state, expr, context, this);
		AnalysisState<A> result = state.bottomExecution();
		boolean mayBeNull = types.stream()
				.anyMatch(t -> t.isReferenceType() && t.asReferenceType().getInnerType().isNullType());

		for (Type t : types)
			if (t.isReferenceType() && t.asReferenceType().getInnerType().isNullType())
				// a null value is not an instance of any type
				result = result.lub(analysis.smallStepSemantics(when(analysis, state, expr, ComparisonEq.INSTANCE, context),
						new Constant(getProgram().getTypes().getBooleanType(), false, getLocation()), context, this));
			else {
				TypeTokenType typeToken = new TypeTokenType(Collections.singleton(type));
				BinaryExpression tc = new BinaryExpression(Untyped.INSTANCE, expr,
						new Constant(typeToken, 0, getLocation()),
						TypeCheck.INSTANCE, getLocation());
				// the type is checked only if the value is not null
				AnalysisState<A> notNull = mayBeNull ? when(analysis, state, expr, ComparisonNe.INSTANCE, context) : state;
				result = result.lub(analysis.smallStepSemantics(notNull, tc, context, this));
			}

		return result;
	}

	/**
	 * Yields the given state, where the given value is compared with null
	 * through the given operator (equality or inequality). If the domain cannot
	 * represent the condition, the state is returned unchanged.
	 */
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> when(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression value,
			BinaryOperator operator,
			ScopeId<A> context)
			throws SemanticException {
		BinaryExpression comparison = new BinaryExpression(getProgram().getTypes().getBooleanType(), value,
				new NullConstant(getLocation()), operator, getLocation());
		AnalysisState<A> assumed = analysis.assume(state, comparison, context, this, this);
		return assumed.getExecutionState().isBottom() ? state : assumed;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return "instanceof(" + getSubExpression() + "," + type + ")";
	}
}

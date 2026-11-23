package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

public class JavaCastExpression extends UnaryExpression {

	private final Type type;

	public JavaCastExpression(
			CFG cfg,
			CodeLocation location,
			Expression subExpression,
			Type type) {
		super(cfg, location, "cast", subExpression);
		this.type = type.isInMemoryType() ? new JavaReferenceType(type) : type;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Constant typeConv = new Constant(new TypeTokenType(Collections.singleton(type)), type, getLocation());
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		if (type.isReferenceType()) {
			if (state.getExecutionState().isBottom())
				// unreachable: nothing is computed
				return state.bottomExecution();
			Set<Type> types = analysis.getRuntimeTypesOf(state, expr, context, this);
			// the cast fails only if the value is not null and not an
			// instance of the type (null is never an instance, but its cast
			// succeeds), built as instanceof does
			BinaryExpression instanceOf = new BinaryExpression(Untyped.INSTANCE, expr,
					new Constant(new TypeTokenType(Collections.singleton(type)), 0, getLocation()),
					TypeCheck.INSTANCE, getLocation());
			BinaryExpression notNull = new BinaryExpression(getProgram().getTypes().getBooleanType(), expr,
					new NullConstant(getLocation()), ComparisonNe.INSTANCE, getLocation());
			BinaryExpression notInstanceOf = new BinaryExpression(getProgram().getTypes().getBooleanType(), notNull,
					new it.unive.lisa.symbolic.value.UnaryExpression(getProgram().getTypes().getBooleanType(),
							instanceOf, LogicalNegation.INSTANCE, getLocation()),
					LogicalAnd.INSTANCE, getLocation());

			// with no known type, the cast might both fail and succeed
			boolean safe = types.isEmpty(), unsafe = types.isEmpty();
			for (Type t : types)
				if (t.isReferenceType() && t.asReferenceType().getInnerType().isNullType() || t.canBeAssignedTo(type))
					safe = true;
				else
					unsafe = true;

			// the exception is raised exactly in the executions where the cast
			// fails, and the cast is computed in the others
			JavaExceptions.Raising<A, D> raising = new JavaExceptions.Raising<>(interprocedural, this,
					getSubExpression());
			String cce = "java.lang.ClassCastException";
			AnalysisState<A> succeeding = state;
			if (unsafe && !safe) {
				raising.raise(Satisfiability.SATISFIED, state, cce);
				succeeding = state.bottomExecution();
			} else if (unsafe) {
				AnalysisState<A> failing = analysis.assume(state, notInstanceOf, context, this, this);
				AnalysisState<A> passing = analysis.assume(state,
						new it.unive.lisa.symbolic.value.UnaryExpression(getProgram().getTypes().getBooleanType(),
								notInstanceOf, LogicalNegation.INSTANCE, getLocation()),
						context, this, this);
				if (failing.getExecutionState().isBottom() || passing.getExecutionState().isBottom())
					// the analysis cannot tell the executions apart, although
					// the cast might both fail and succeed
					raising.raise(Satisfiability.UNKNOWN, state, cce);
				else {
					raising.raise(Satisfiability.SATISFIED, failing, cce);
					succeeding = passing;
				}
			}

			AnalysisState<A> result = succeeding;
			if (safe && !succeeding.getExecutionState().isBottom())
				result = analysis.smallStepSemantics(succeeding,
						new BinaryExpression(type, expr, typeConv, TypeCast.INSTANCE, getLocation()), context, this);
			return raising.moveFrom(result);
		} else {
			BinaryExpression castExpression = new BinaryExpression(type, expr, typeConv, TypeConv.INSTANCE,
					getLocation());
			return analysis.smallStepSemantics(state, castExpression, context, this);
		}
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return "(" + type + ") " + getSubExpression();
	}
}

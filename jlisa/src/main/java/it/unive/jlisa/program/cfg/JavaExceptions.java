package it.unive.jlisa.program.cfg;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
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
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helpers to raise Java exceptions from the semantics of statements and
 * natives, so that each exception is raised only in the executions that raise
 * it. An exception raised in a state is raised, for the verification of the
 * warnings, in all the executions of that state: the state given to the error
 * channel must hence be exactly the executions raising it, or the exception
 * must be raised under an unknown condition. The {@link Raising} collector
 * makes each raise state which case it is.
 */
public final class JavaExceptions {

	private JavaExceptions() {
	}

	/**
	 * Yields whether the given runtime type is the one of {@code null}.
	 *
	 * @param type the runtime type
	 *
	 * @return whether it is the type of {@code null}
	 */
	public static boolean isNull(
			Type type) {
		return type.isNullType() || type.isPointerType() && type.asPointerType().getInnerType().isNullType();
	}

	/**
	 * Restricts the given state to the executions of a branch whose condition
	 * is not expressed (e.g., an exception raised when the analysis cannot tell
	 * which executions raise it): the branch happens under an unknown
	 * condition. The analysis is not affected, while the verification of the
	 * warnings never takes the branch for granted.
	 *
	 * @param <A>             the kind of {@link AbstractLattice} of the state
	 * @param <D>             the kind of {@link AbstractDomain} of the analysis
	 * @param interprocedural the interprocedural analysis
	 * @param state           the state
	 * @param pp              the program point of the branch
	 *
	 * @return the restricted state
	 *
	 * @throws SemanticException if the analysis fails
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> maybe(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			Statement pp)
			throws SemanticException {
		return interprocedural.getAnalysis().assume(state, new PushAny(JavaBooleanType.INSTANCE, pp.getLocation()),
				interprocedural.getCurrentContext(), pp, pp);
	}

	/**
	 * Collects the executions raising exceptions in a statement, so that they
	 * are moved to the error channel once the executions that continue normally
	 * are known (see {@link #moveFrom(AnalysisState)}). Each raise states
	 * whether the given state is exactly the executions raising the exception
	 * ({@link Satisfiability#SATISFIED}) or a state where it is raised under an
	 * unknown condition ({@link Satisfiability#UNKNOWN}).
	 *
	 * @param <A> the kind of {@link AbstractLattice} of the states
	 * @param <D> the kind of {@link AbstractDomain} of the analysis
	 */
	public static final class Raising<A extends AbstractLattice<A>, D extends AbstractDomain<A>> {

		private final InterproceduralAnalysis<A, D> interprocedural;

		private final Statement thrower;

		private final Statement source;

		private final Collection<Identifier> consumed;

		// the state of the executions raising each error, not moved yet
		private final Map<AnalysisState.Error, AnalysisState<A>> raised;

		// whether the exceptions raised in all the executions of a state are
		// raised in all of them (see certainOnlyIf)
		private final boolean certain;

		/**
		 * Builds the collector.
		 *
		 * @param interprocedural the interprocedural analysis
		 * @param thrower         the statement raising the exceptions
		 * @param consumed        the identifiers that the executions raising an
		 *                            exception forget (e.g. the meta variables
		 *                            of the sub-expressions of the statement)
		 */
		public Raising(
				InterproceduralAnalysis<A, D> interprocedural,
				Statement thrower,
				Collection<Identifier> consumed) {
			this(interprocedural, thrower, thrower, consumed);
		}

		/**
		 * Builds the collector for a statement that raises the exceptions on
		 * behalf of another one (e.g. a native, on behalf of the call of the
		 * program it implements): the errors are the ones of {@code source}, so
		 * that, e.g., the handlers of the exceptions raised by {@code source}
		 * catch them.
		 *
		 * @param interprocedural the interprocedural analysis
		 * @param thrower         the statement raising the exceptions
		 * @param source          the statement of the program raising them
		 * @param consumed        the identifiers that the executions raising an
		 *                            exception forget
		 */
		public Raising(
				InterproceduralAnalysis<A, D> interprocedural,
				Statement thrower,
				Statement source,
				Collection<Identifier> consumed) {
			this.interprocedural = interprocedural;
			this.thrower = thrower;
			this.source = source == null ? thrower : source;
			this.consumed = consumed;
			this.raised = new LinkedHashMap<>();
			this.certain = true;
		}

		private Raising(
				Raising<A, D> other,
				boolean certain) {
			this.interprocedural = other.interprocedural;
			this.thrower = other.thrower;
			this.source = other.source;
			this.consumed = other.consumed;
			this.raised = other.raised;
			this.certain = other.certain && certain;
		}

		/**
		 * Yields a collector of the same exceptions as this one (moved together
		 * with them) that, unless the given condition holds, raises under an
		 * unknown condition also the exceptions that this would raise in all
		 * the executions of a state. This is used with states that might have
		 * more executions than the ones where the exceptions are raised (e.g.,
		 * the executions computing a combination of the operands of a
		 * statement, if they are not known exactly, see
		 * {@link it.unive.lisa.program.cfg.statement.NaryExpression#restrictsExactly}).
		 *
		 * @param exact whether the states are made exactly of the executions
		 *                  where the exceptions are raised
		 *
		 * @return the collector
		 */
		public Raising<A, D> certainOnlyIf(
				boolean exact) {
			return exact ? this : new Raising<>(this, false);
		}

		/**
		 * Builds the collector, where the executions raising an exception
		 * forget the meta variables of the given sub-expressions.
		 *
		 * @param interprocedural the interprocedural analysis
		 * @param thrower         the statement raising the exceptions
		 * @param subExpressions  the sub-expressions of the statement
		 */
		public Raising(
				InterproceduralAnalysis<A, D> interprocedural,
				Statement thrower,
				Expression... subExpressions) {
			this(interprocedural, thrower, metasOf(subExpressions));
		}

		/**
		 * Builds the collector for a native that raises the exceptions on
		 * behalf of the call of the program it implements (see
		 * {@link #Raising(InterproceduralAnalysis, Statement, Statement, Collection)}),
		 * where the executions raising an exception forget the meta variables
		 * of the given sub-expressions.
		 *
		 * @param <A>             the kind of {@link AbstractLattice} of the
		 *                            states
		 * @param <D>             the kind of {@link AbstractDomain} of the
		 *                            analysis
		 * @param interprocedural the interprocedural analysis
		 * @param thrower         the native raising the exceptions
		 * @param originating     the call implemented by the native
		 * @param subExpressions  the sub-expressions of the native
		 *
		 * @return the collector
		 */
		public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Raising<A, D> ofNative(
				InterproceduralAnalysis<A, D> interprocedural,
				Statement thrower,
				Statement originating,
				Expression... subExpressions) {
			return new Raising<>(interprocedural, thrower, originating, metasOf(subExpressions));
		}

		private static Collection<Identifier> metasOf(
				Expression... subExpressions) {
			List<Identifier> metas = new ArrayList<>();
			for (Expression sub : subExpressions)
				metas.addAll(sub.getMetaVariables());
			return metas;
		}

		/**
		 * Yields whether no exception has been raised.
		 *
		 * @return whether no exception has been raised
		 */
		public boolean isEmpty() {
			return raised.isEmpty();
		}

		/**
		 * Raises an exception of the given class in the executions of the given
		 * state where it is raised, according to {@code raises}: in none of
		 * them if it is {@link Satisfiability#NOT_SATISFIED}, in all of them if
		 * it is {@link Satisfiability#SATISFIED} (that is, the state is exactly
		 * the executions raising it), and under an unknown condition otherwise,
		 * so that the verification of the warnings never takes it for granted.
		 *
		 * @param raises    whether the exception is raised
		 * @param state     the state of the executions that might raise the
		 *                      exception
		 * @param className the name of the class of the exception
		 *
		 * @throws SemanticException if the analysis fails
		 */
		public void raise(
				Satisfiability raises,
				AnalysisState<A> state,
				String className)
				throws SemanticException {
			if (raises == Satisfiability.NOT_SATISFIED || raises == Satisfiability.BOTTOM
					|| state.getExecutionState().isBottom())
				return;
			Analysis<A, D> analysis = interprocedural.getAnalysis();
			ScopeId<A> context = interprocedural.getCurrentContext();
			AnalysisState<A> raising = raises == Satisfiability.SATISFIED && certain ? state
					: maybe(interprocedural, state, thrower);
			if (raising.getExecutionState().isBottom())
				return;

			CodeLocation location = thrower.getLocation();
			LibrarySpecificationProvider.importClass(thrower.getProgram(), className);
			JavaClassType type = JavaClassType.lookup(className);
			JavaNewObj call = new JavaNewObj(thrower.getCFG(), location, type.getReference(), new Expression[0]);
			AnalysisState<A> created = call.forwardSemanticsAux(interprocedural, raising, new ExpressionSet[0],
					new StatementStore<>(raising));
			AnalysisState<A> thrown = created.bottomExecution();
			for (SymbolicExpression th : created.getExecutionExpressions()) {
				CFGThrow throwVar = new CFGThrow(thrower.getCFG(), type.getReference(), location);
				AnalysisState<A> tmp = analysis.assign(created, throwVar, th, context, thrower);
				// the receiver of the constructor and the values consumed by
				// the statement
				tmp = tmp.forgetIdentifiers(call.getMetaVariables(), thrower).forgetIdentifiers(consumed, thrower);
				thrown = thrown.lub(tmp.withExecutionExpression(throwVar));
			}
			AnalysisState.Error error = new AnalysisState.Error(type.getReference(), source);
			AnalysisState<A> previous = raised.get(error);
			raised.put(error, previous == null ? thrown : previous.lub(thrown));
		}

		/**
		 * Raises an exception of the given class exactly in the executions of
		 * the given state where the given condition holds, yielding the ones
		 * where it does not, that continue normally. The analysis decides
		 * whether the condition holds (see {@link Analysis#satisfies}): if it
		 * surely does, the exception is raised in all the executions; if it
		 * surely does not, in none of them; otherwise, the state is restricted
		 * to the executions where it holds to raise the exception, and to the
		 * ones where it does not to continue. The executions where the
		 * condition holds surely raise the exception only if {@code surely}
		 * holds (e.g. not if they might raise another exception instead),
		 * otherwise the exception is raised in them under an unknown condition.
		 *
		 * @param condition the condition under which the exception is raised
		 * @param surely    whether the executions where the condition holds
		 *                      surely raise the exception
		 * @param state     the state of the executions that might raise the
		 *                      exception
		 * @param className the name of the class of the exception
		 *
		 * @return the state of the executions where the exception is not raised
		 *
		 * @throws SemanticException if the analysis fails
		 */
		public AnalysisState<A> raiseWhere(
				SymbolicExpression condition,
				boolean surely,
				AnalysisState<A> state,
				String className)
				throws SemanticException {
			if (state.getExecutionState().isBottom())
				return state;
			Analysis<A, D> analysis = interprocedural.getAnalysis();
			ScopeId<A> context = interprocedural.getCurrentContext();
			Satisfiability sat = analysis.satisfies(state, condition, context, thrower);
			if (sat == Satisfiability.NOT_SATISFIED || sat == Satisfiability.BOTTOM)
				return state;
			Satisfiability raises = surely ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
			if (sat == Satisfiability.SATISFIED) {
				raise(raises, state, className);
				return state.bottomExecution();
			}
			raise(raises, analysis.assume(state, condition, context, thrower, thrower), className);
			return analysis.assume(state,
					new UnaryExpression(JavaBooleanType.INSTANCE, condition, LogicalNegation.INSTANCE,
							thrower.getLocation()),
					context, thrower, thrower);
		}

		/**
		 * Raises a {@link NullPointerException} in the executions of the given
		 * state where the given value is {@code null}, yielding the ones where
		 * it is not. When the value surely is {@code null} (all its runtime
		 * types are the one of {@code null}), the exception is raised in all
		 * the executions; when it might be, in the ones where it is (if the
		 * analysis cannot tell them apart, the exception is raised under an
		 * unknown condition, and all the executions also continue normally).
		 *
		 * @param value the value that must not be {@code null}
		 * @param state the state
		 *
		 * @return the state of the executions where the value is not
		 *             {@code null}
		 *
		 * @throws SemanticException if the analysis fails
		 */
		public AnalysisState<A> raiseIfNull(
				SymbolicExpression value,
				AnalysisState<A> state)
				throws SemanticException {
			if (state.getExecutionState().isBottom())
				return state;
			Analysis<A, D> analysis = interprocedural.getAnalysis();
			ScopeId<A> context = interprocedural.getCurrentContext();
			String npe = "java.lang.NullPointerException";
			Set<Type> types = analysis.getRuntimeTypesOf(state, value, context, thrower);
			boolean mightBeNull = types.stream().anyMatch(JavaExceptions::isNull);
			if (!mightBeNull)
				return state;
			if (types.stream().allMatch(JavaExceptions::isNull)) {
				raise(Satisfiability.SATISFIED, state, npe);
				return state.bottomExecution();
			}
			CodeLocation location = thrower.getLocation();
			NullConstant nil = new NullConstant(location);
			AnalysisState<A> isNull = analysis.assume(state,
					new BinaryExpression(JavaBooleanType.INSTANCE, value, nil, ComparisonEq.INSTANCE, location),
					context, thrower, thrower);
			AnalysisState<A> notNull = analysis.assume(state,
					new BinaryExpression(JavaBooleanType.INSTANCE, value, nil, ComparisonNe.INSTANCE, location),
					context, thrower, thrower);
			if (isNull.getExecutionState().isBottom() || notNull.getExecutionState().isBottom()) {
				// the analysis cannot tell the executions apart, although the
				// value might be null or not
				raise(Satisfiability.UNKNOWN, state, npe);
				return state;
			}
			raise(Satisfiability.SATISFIED, isNull, npe);
			return notNull;
		}

		/**
		 * Moves the executions raising the exceptions to the error channel,
		 * given the state of all the executions continuing normally (the moves
		 * are total if there are none).
		 *
		 * @param normal the state of the executions that do not raise any of
		 *                   the exceptions
		 *
		 * @return the state after the moves
		 *
		 * @throws SemanticException if the analysis fails
		 */
		public AnalysisState<A> moveFrom(
				AnalysisState<A> normal)
				throws SemanticException {
			Analysis<A, D> analysis = interprocedural.getAnalysis();
			ScopeId<A> context = interprocedural.getCurrentContext();
			if (raised.isEmpty())
				return normal;
			// no execution continues normally: nothing is computed
			AnalysisState<A> result = normal.getExecutionState().isBottom()
					? normal.withExecution(normal.getExecution().bottom())
					: normal;
			for (Map.Entry<AnalysisState.Error, AnalysisState<A>> entry : raised.entrySet())
				result = analysis.moveExecutionToError(result, entry.getValue(), entry.getKey(), context, thrower);
			return result;
		}
	}
}

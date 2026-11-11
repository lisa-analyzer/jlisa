package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.java.constructs.lists.ListModel;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * The entries of the maps whose keys are not constants (the ones with constant
 * keys are fields named after the key, see {@link MapPut}): the map has
 * {@code $n} entries {@code $key<i>}, {@code $value<i>}, {@code $alive<i>}
 * ({@code $alive<i>} is 0 after the entry is removed), and counts the constant
 * keys written in {@code $c}. A key is surely the one of an entry if both are
 * the same object (a single strong allocation site); otherwise it might be (two
 * strings might be equal), so the result joins the possibilities.
 */
public class MapModel {

	/**
	 * The number of entries with keys that are not constants that are tracked:
	 * after them, the map has unknown keys.
	 */
	public static final int MAX_ENTRIES = 16;

	private MapModel() {
	}

	private static AccessChild field(
			SymbolicExpression map,
			String name,
			Type type,
			CodeLocation location) {
		return new AccessChild(type, new HeapDereference(JavaClassType.lookup("java.util.Map"), map, location),
				new GlobalVariable(Untyped.INSTANCE, name, location), location);
	}

	/**
	 * Returns the access to the size of the map (the field {@code $size}), used
	 * by all the map constructs.
	 *
	 * @param map      the map
	 * @param location the location of the access
	 *
	 * @return the access
	 */
	public static AccessChild size(
			SymbolicExpression map,
			CodeLocation location) {
		return field(map, "$size", JavaIntType.INSTANCE, location);
	}

	/**
	 * Returns the access to the number of entries with keys that are not
	 * constants.
	 *
	 * @param map      the map
	 * @param location the location of the access
	 *
	 * @return the access
	 */
	public static AccessChild count(
			SymbolicExpression map,
			CodeLocation location) {
		return field(map, "$n", JavaIntType.INSTANCE, location);
	}

	private static AccessChild key(
			SymbolicExpression map,
			int i,
			CodeLocation location) {
		return field(map, "$key" + i, new JavaReferenceType(JavaClassType.getObjectType()), location);
	}

	private static AccessChild value(
			SymbolicExpression map,
			int i,
			CodeLocation location) {
		return field(map, "$value" + i, new JavaReferenceType(JavaClassType.getObjectType()), location);
	}

	private static AccessChild alive(
			SymbolicExpression map,
			int i,
			CodeLocation location) {
		return field(map, "$alive" + i, JavaIntType.INSTANCE, location);
	}

	private static BinaryExpression compare(
			SymbolicExpression left,
			int value,
			it.unive.lisa.symbolic.value.operator.binary.BinaryOperator op,
			CodeLocation location) {
		return new BinaryExpression(JavaBooleanType.INSTANCE, left, new Constant(JavaIntType.INSTANCE, value, location),
				op, location);
	}

	private static Constant constant(
			int value,
			CodeLocation location) {
		return new Constant(JavaIntType.INSTANCE, value, location);
	}

	/**
	 * Returns the state where the given map has no entries anymore (for
	 * {@code clear}), if it has the fields of the entries.
	 *
	 * @param <A>      the kind of the abstract state
	 * @param <D>      the kind of the abstract domain
	 * @param analysis the analysis
	 * @param context  the current context
	 * @param state    the state
	 * @param map      the map
	 * @param st       the statement
	 *
	 * @return the state
	 *
	 * @throws SemanticException if the field cannot be assigned
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> clear(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression map,
			Statement st)
			throws SemanticException {
		if (!knows(analysis, context, state, count(map, st.getLocation()), st))
			return state;
		return analysis.assign(state, count(map, st.getLocation()), constant(0, st.getLocation()), context, st);
	}

	/**
	 * Returns whether the entries of the given map are tracked (it was created
	 * by a modeled constructor).
	 *
	 * @param <A>      the kind of the abstract state
	 * @param <D>      the kind of the abstract domain
	 * @param analysis the analysis
	 * @param context  the current context
	 * @param state    the state
	 * @param map      the map
	 * @param st       the statement
	 *
	 * @return whether the entries are tracked
	 *
	 * @throws SemanticException if the field cannot be resolved
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean isTracked(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression map,
			Statement st)
			throws SemanticException {
		return knows(analysis, context, state, size(map, st.getLocation()), st);
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean knows(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression field,
			Statement st)
			throws SemanticException {
		ExpressionSet ids = analysis.rewrite(state, field, context, st);
		if (ids.isEmpty())
			return false;
		for (SymbolicExpression e : ids)
			if (!(e instanceof Identifier id) || !state.knowsIdentifier(id))
				return false;
		return true;
	}

	// the state where the map has k entries (a map without the field has none)
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> bucket(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression map,
			int k,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();
		if (!knows(analysis, context, state, count(map, location), st))
			return k == 0 ? state : state.bottomExecution();
		return analysis.assume(state, compare(count(map, location), k,
				k == MAX_ENTRIES ? ComparisonGe.INSTANCE : ComparisonEq.INSTANCE, location), context, st, st);
	}

	/**
	 * Returns whether the two values surely denote the same object: they point
	 * to the same single strong allocation site (or, if they are not
	 * references, their values are surely equal). Since {@code equals} is
	 * reflexive, the two values are then surely equal also for the collections.
	 *
	 * @param <A>      the kind of the abstract state
	 * @param <D>      the kind of the abstract domain
	 * @param analysis the analysis
	 * @param context  the current context
	 * @param state    the state
	 * @param a        the first value
	 * @param b        the second value
	 * @param st       the statement
	 *
	 * @return whether the two values surely denote the same object
	 *
	 * @throws SemanticException if the values cannot be resolved
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean sameObject(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression a,
			SymbolicExpression b,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();
		// keys that are not references (e.g., int keys, boxed by Java) are the
		// same if their values are surely equal
		if (!isReference(analysis, context, state, a, st) || !isReference(analysis, context, state, b, st))
			return analysis.satisfies(state, new BinaryExpression(JavaBooleanType.INSTANCE, a, b,
					ComparisonEq.INSTANCE, location), context, st) == it.unive.lisa.lattices.Satisfiability.SATISFIED;
		Type object = JavaClassType.getObjectType();
		ExpressionSet left = analysis.rewrite(state, new HeapDereference(object, a, location), context, st);
		ExpressionSet right = analysis.rewrite(state, new HeapDereference(object, b, location), context, st);
		if (left.size() != 1 || right.size() != 1)
			return false;
		SymbolicExpression l = left.iterator().next();
		SymbolicExpression r = right.iterator().next();
		return l instanceof Identifier li && r instanceof Identifier ri && !li.isWeak() && !ri.isWeak()
				&& li.equals(ri);
	}

	// whether all the runtime types of the expression are references
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean isReference(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression e,
			Statement st)
			throws SemanticException {
		java.util.Set<Type> types = analysis.getRuntimeTypesOf(state, e, context, st);
		if (types.isEmpty())
			return false;
		for (Type t : types)
			if (!t.isPointerType() && !t.isNullType())
				return false;
		return true;
	}

	// whether the entry might be (not) alive in the state
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> withAlive(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression map,
			int i,
			int value,
			Statement st)
			throws SemanticException {
		return analysis.assume(state, compare(alive(map, i, st.getLocation()), value, ComparisonEq.INSTANCE,
				st.getLocation()), context, st, st);
	}

	// the state where the map (with k entries) surely has no keys that are not
	// in the entries: its size is at most the number of entries that are
	// surely alive, and its keys are tracked
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> noOtherKeys(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression map,
			int k,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();
		int surelyAlive = 0;
		for (int i = 0; i < Math.min(k, MAX_ENTRIES); i++)
			if (withAlive(analysis, context, state, map, i, 0, st).getExecutionState().isBottom())
				surelyAlive++;
		AnalysisState<A> none = analysis.assume(state, new BinaryExpression(JavaBooleanType.INSTANCE,
				constant(surelyAlive, location), size(map, location), ComparisonGe.INSTANCE, location), context, st,
				st);
		if (!knows(analysis, context, state, MapGet.unknownKeys(map, location), st))
			return none;
		return analysis.assume(none, new BinaryExpression(JavaBooleanType.INSTANCE, MapGet.unknownKeys(map, location),
				new Constant(JavaBooleanType.INSTANCE, false, location), ComparisonEq.INSTANCE, location), context, st,
				st);
	}

	// the state where the map (with k entries) might have keys that are not in
	// the entries (constant keys, or keys that are not tracked): its size is
	// larger than the number of entries that are surely alive
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> otherKeys(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression map,
			int k,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();
		int surelyAlive = 0;
		for (int i = 0; i < Math.min(k, MAX_ENTRIES); i++)
			if (withAlive(analysis, context, state, map, i, 0, st).getExecutionState().isBottom())
				surelyAlive++;
		AnalysisState<A> others = analysis.assume(state, compare(size(map, location), surelyAlive,
				ComparisonGt.INSTANCE, location), context, st, st);
		if (!knows(analysis, context, state, MapGet.unknownKeys(map, location), st))
			return others;
		return others.lub(analysis.assume(state, new BinaryExpression(JavaBooleanType.INSTANCE,
				MapGet.unknownKeys(map, location), new Constant(JavaBooleanType.INSTANCE, true, location),
				ComparisonEq.INSTANCE, location), context, st, st));
	}

	/**
	 * Returns the state where the size of the given map changes by the given
	 * amount (-1 or 1) or stays the same: the new size is an unknown value
	 * between the two, not a join of the two cases (that the model checker
	 * would take as two executions).
	 *
	 * @param <A>      the kind of the abstract state
	 * @param <D>      the kind of the abstract domain
	 * @param analysis the analysis
	 * @param context  the current context
	 * @param state    the state
	 * @param map      the map
	 * @param delta    the change of the size, -1 or 1
	 * @param st       the statement
	 *
	 * @return the state
	 *
	 * @throws SemanticException if the size cannot be assigned
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> maybeResize(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression map,
			int delta,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();
		AccessChild size = size(map, location);
		AnalysisState<A> result = state.bottomExecution();
		int max = ListModel.MAX_TRACKED_ELEMENTS;
		for (int k = 0; k <= max; k++) {
			AnalysisState<A> before = analysis.assume(state,
					compare(size, k, k == max ? ComparisonGe.INSTANCE : ComparisonEq.INSTANCE, location), context, st,
					st);
			if (before.getExecutionState().isBottom())
				continue;
			if (delta < 0 && k == 0)
				result = result.lub(before);
			else if (k == max)
				result = result.lub(analysis.assign(before, size, ListModel.between(size, max - 1, Integer.MAX_VALUE,
						location), context, st));
			else
				result = result.lub(analysis.assign(before, size,
						ListModel.between(size, Math.min(k, k + delta), Math.max(k, k + delta), location), context,
						st));
		}
		return result;
	}

	/**
	 * Returns the state where unknown keys are added to the given map (e.g.,
	 * {@code putAll}): its keys are not tracked anymore, and its size does not
	 * decrease.
	 *
	 * @param <A>      the kind of the abstract state
	 * @param <D>      the kind of the abstract domain
	 * @param analysis the analysis
	 * @param context  the current context
	 * @param state    the state
	 * @param map      the map
	 * @param st       the statement
	 *
	 * @return the state
	 *
	 * @throws SemanticException if the fields cannot be assigned
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> addUnknownKeys(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression map,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();
		AnalysisState<A> tmp = analysis.assign(state, MapGet.unknownKeys(map, location),
				new Constant(JavaBooleanType.INSTANCE, true, location), context, st);
		AccessChild size = size(map, location);
		AnalysisState<A> result = tmp.bottomExecution();
		int max = ListModel.MAX_TRACKED_ELEMENTS;
		for (int k = 0; k <= max; k++) {
			AnalysisState<A> before = analysis.assume(tmp,
					compare(size, k, k == max ? ComparisonGe.INSTANCE : ComparisonEq.INSTANCE, location), context, st,
					st);
			if (!before.getExecutionState().isBottom())
				result = result.lub(analysis.assign(before, size,
						ListModel.between(size, Math.min(k, max), Integer.MAX_VALUE, location), context, st));
		}
		return result;
	}

	/**
	 * Returns the given result with a single expression: if it has more than
	 * one (e.g., an exact value and an unknown one), they are replaced by a
	 * single unknown value of the given type, as the checkers take a constant
	 * among the expressions of a result as a value that surely occurs.
	 *
	 * @param <A>      the kind of the abstract state
	 * @param <D>      the kind of the abstract domain
	 * @param analysis the analysis
	 * @param context  the current context
	 * @param result   the result
	 * @param type     the type of the unknown value
	 * @param st       the statement
	 *
	 * @return the result with a single expression
	 *
	 * @throws SemanticException if the value cannot be computed
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> single(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> result,
			Type type,
			Statement st)
			throws SemanticException {
		if (result.getExecutionExpressions().size() <= 1)
			return result;
		boolean anyUnknown = false;
		for (SymbolicExpression e : result.getExecutionExpressions())
			if (e instanceof PushAny)
				anyUnknown = true;
		if (!anyUnknown)
			return result;
		return analysis.smallStepSemantics(result, new PushAny(type, st.getLocation()), context, st);
	}

	/**
	 * Returns the given result of a method of the given map, joined with the
	 * state where a {@code NullPointerException} is raised if the map does not
	 * allow null keys (a {@code ConcurrentMap}) and the given key might be
	 * null.
	 *
	 * @param <A>             the kind of the abstract state
	 * @param <D>             the kind of the abstract domain
	 * @param interprocedural the interprocedural analysis
	 * @param state           the state before the method
	 * @param done            the result of the method
	 * @param map             the map
	 * @param key             the key
	 * @param st              the statement
	 * @param subExpressions  the sub-expressions of the statement
	 * @param expressions     the statement store
	 *
	 * @return the result, possibly with the exception
	 *
	 * @throws SemanticException if the exception cannot be raised
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> nullKey(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			AnalysisState<A> done,
			SymbolicExpression map,
			SymbolicExpression key,
			Statement st,
			Expression[] subExpressions,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		String concurrentMap = "java.util.concurrent.ConcurrentMap";
		if (!JavaClassType.hasType(concurrentMap))
			return done;
		JavaClassType concurrent = JavaClassType.lookup(concurrentMap);
		boolean noNullKeys = false;
		for (Type t : analysis.getRuntimeTypesOf(state, map, context, st))
			if (t instanceof JavaReferenceType ref && ref.getInnerType().canBeAssignedTo(concurrent))
				noNullKeys = true;
		if (!noNullKeys || !analysis.getRuntimeTypesOf(state, key, context, st)
				.contains(new JavaReferenceType(it.unive.lisa.type.NullType.INSTANCE)))
			return done;
		String exc = "java.lang.NullPointerException";
		AnalysisState<A> raised = ListModel.raise(interprocedural, state, exc, st, subExpressions, expressions);
		AnalysisState.Error error = new AnalysisState.Error(JavaClassType.lookup(exc).getReference(), st);
		if (done.getExecutionState().isBottom())
			return analysis.moveExecutionToError(raised, error, context, st);
		return analysis.moveExecutionToError(done, raised, error, context, st);
	}

	/**
	 * Returns the value of the given key in the given map (that is tracked),
	 * for {@code get}: the value of the last entry that has surely the key,
	 * joined with the ones that might have it, or {@code null} if no entry has
	 * surely the key; an unknown value if the map might have other keys.
	 *
	 * @param <A>             the kind of the abstract state
	 * @param <D>             the kind of the abstract domain
	 * @param interprocedural the interprocedural analysis
	 * @param state           the state
	 * @param map             the map
	 * @param key             the key
	 * @param st              the statement
	 *
	 * @return the state with the value
	 *
	 * @throws SemanticException if the fields cannot be read
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> get(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression map,
			SymbolicExpression key,
			Statement st)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = st.getLocation();
		AnalysisState<A> result = state.bottomExecution();
		for (int k = 0; k <= MAX_ENTRIES; k++) {
			AnalysisState<A> bucket = bucket(analysis, context, state, map, k, st);
			if (bucket.getExecutionState().isBottom())
				continue;
			if (k == MAX_ENTRIES) {
				result = result.lub(analysis.smallStepSemantics(bucket, new PushAny(Untyped.INSTANCE, location),
						context, st));
				continue;
			}
			AnalysisState<A> other = otherKeys(analysis, context, bucket, map, k, st);
			if (!other.getExecutionState().isBottom())
				result = result.lub(analysis.smallStepSemantics(other, new PushAny(Untyped.INSTANCE, location),
						context, st));
			AnalysisState<A> rest = bucket;
			for (int i = k - 1; i >= 0 && !rest.getExecutionState().isBottom(); i--) {
				AnalysisState<A> alive = withAlive(analysis, context, rest, map, i, 1, st);
				if (alive.getExecutionState().isBottom())
					continue;
				result = result.lub(analysis.smallStepSemantics(alive, value(map, i, location), context, st));
				if (sameObject(analysis, context, rest, key, key(map, i, location), st))
					// surely this entry, if alive
					rest = withAlive(analysis, context, rest, map, i, 0, st);
			}
			// absent only if the map surely has no other keys
			rest = noOtherKeys(analysis, context, rest, map, k, st);
			if (!rest.getExecutionState().isBottom())
				result = result.lub(analysis.smallStepSemantics(rest, new NullConstant(location), context, st));
		}
		return single(analysis, context, result, Untyped.INSTANCE, st);
	}

	/**
	 * Returns whether the given key is in the given map (that is tracked), for
	 * {@code containsKey}.
	 *
	 * @param <A>             the kind of the abstract state
	 * @param <D>             the kind of the abstract domain
	 * @param interprocedural the interprocedural analysis
	 * @param state           the state
	 * @param map             the map
	 * @param key             the key
	 * @param st              the statement
	 *
	 * @return the state with the boolean result
	 *
	 * @throws SemanticException if the fields cannot be read
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> containsKey(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression map,
			SymbolicExpression key,
			Statement st)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = st.getLocation();
		Constant yes = new Constant(JavaBooleanType.INSTANCE, true, location);
		Constant no = new Constant(JavaBooleanType.INSTANCE, false, location);
		PushAny any = new PushAny(JavaBooleanType.INSTANCE, location);
		AnalysisState<A> result = state.bottomExecution();
		for (int k = 0; k <= MAX_ENTRIES; k++) {
			AnalysisState<A> bucket = bucket(analysis, context, state, map, k, st);
			if (bucket.getExecutionState().isBottom())
				continue;
			if (k == MAX_ENTRIES) {
				result = result.lub(analysis.smallStepSemantics(bucket, any, context, st));
				continue;
			}
			AnalysisState<A> other = otherKeys(analysis, context, bucket, map, k, st);
			if (!other.getExecutionState().isBottom())
				result = result.lub(analysis.smallStepSemantics(other, any, context, st));
			AnalysisState<A> rest = bucket;
			for (int i = k - 1; i >= 0 && !rest.getExecutionState().isBottom(); i--) {
				AnalysisState<A> alive = withAlive(analysis, context, rest, map, i, 1, st);
				if (alive.getExecutionState().isBottom())
					continue;
				if (sameObject(analysis, context, rest, key, key(map, i, location), st)) {
					result = result.lub(analysis.smallStepSemantics(alive, yes, context, st));
					rest = withAlive(analysis, context, rest, map, i, 0, st);
				} else
					result = result.lub(analysis.smallStepSemantics(alive, any, context, st));
			}
			// absent only if the map surely has no other keys
			rest = noOtherKeys(analysis, context, rest, map, k, st);
			if (!rest.getExecutionState().isBottom())
				result = result.lub(analysis.smallStepSemantics(rest, no, context, st));
		}
		return single(analysis, context, result, JavaBooleanType.INSTANCE, st);
	}

	/**
	 * Returns the state where the given value is put with the given key in the
	 * given map (that is tracked), for {@code put}: an entry that has surely
	 * the key is replaced, otherwise a new entry is added, and the size grows
	 * by one if the key surely is new (between 0 and 1 otherwise).
	 *
	 * @param <A>             the kind of the abstract state
	 * @param <D>             the kind of the abstract domain
	 * @param interprocedural the interprocedural analysis
	 * @param state           the state
	 * @param map             the map
	 * @param key             the key
	 * @param value           the value
	 * @param valueExpr       the expression of the value
	 * @param st              the statement
	 * @param expressions     the statement store
	 *
	 * @return the state where the value is put (the returned value is unknown)
	 *
	 * @throws SemanticException if the fields cannot be assigned
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> put(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression map,
			SymbolicExpression key,
			SymbolicExpression value,
			Expression valueExpr,
			Expression st,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = st.getLocation();
		AccessChild size = size(map, location);
		AccessChild count = count(map, location);
		AnalysisState<A> boxedAll = ListModel.box(interprocedural, state, value, valueExpr, st, expressions);
		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression boxed : boxedAll.getExecutionExpressions())
			for (int k = 0; k <= MAX_ENTRIES; k++) {
				AnalysisState<A> bucket = bucket(analysis, context, boxedAll, map, k, st);
				if (bucket.getExecutionState().isBottom())
					continue;
				if (k == MAX_ENTRIES) {
					// too many entries: the keys are not tracked anymore
					AnalysisState<A> tmp = analysis.assign(bucket, MapGet.unknownKeys(map, location),
							new Constant(JavaBooleanType.INSTANCE, true, location), context, st);
					result = result.lub(analysis.assign(tmp, size, ListModel.between(size, 0, Integer.MAX_VALUE,
							location), context, st));
					continue;
				}

				// an entry that has surely the key and is alive is replaced
				AnalysisState<A> rest = bucket;
				boolean possiblyThere = false;
				for (int i = k - 1; i >= 0 && !rest.getExecutionState().isBottom(); i--) {
					AnalysisState<A> alive = withAlive(analysis, context, rest, map, i, 1, st);
					if (alive.getExecutionState().isBottom())
						continue;
					if (sameObject(analysis, context, rest, key, key(map, i, location), st)) {
						result = result.lub(analysis.assign(alive, value(map, i, location), boxed, context, st));
						rest = withAlive(analysis, context, rest, map, i, 0, st);
					} else
						possiblyThere = true;
				}
				if (rest.getExecutionState().isBottom())
					continue;

				// otherwise, a new entry
				AnalysisState<A> tmp = analysis.assign(rest, key(map, k, location), key, context, st);
				tmp = analysis.assign(tmp, value(map, k, location), boxed, context, st);
				tmp = analysis.assign(tmp, alive(map, k, location), constant(1, location), context, st);
				tmp = analysis.assign(tmp, count, constant(k + 1, location), context, st);
				AnalysisState<A> other = otherKeys(analysis, context, rest, map, k, st);
				if (possiblyThere || !other.getExecutionState().isBottom())
					// the key might already be in the map
					result = result.lub(maybeResize(analysis, context, tmp, map, 1, st));
				else
					result = result.lub(analysis.assign(tmp, size, new BinaryExpression(JavaIntType.INSTANCE, size,
							constant(1, location), NumericNonOverflowingAdd.INSTANCE, location), context, st));
			}
		return analysis.smallStepSemantics(result, new PushAny(Untyped.INSTANCE, location), context, st);
	}

	/**
	 * Returns the state where the given key is removed from the given map (that
	 * is tracked), for {@code remove}: an entry that has surely the key is
	 * removed, and the size decreases by one; the entries that might have it
	 * might be removed. The returned value is the one of {@link #get}.
	 *
	 * @param <A>             the kind of the abstract state
	 * @param <D>             the kind of the abstract domain
	 * @param interprocedural the interprocedural analysis
	 * @param state           the state
	 * @param map             the map
	 * @param key             the key
	 * @param st              the statement
	 *
	 * @return the state where the key is removed
	 *
	 * @throws SemanticException if the fields cannot be assigned
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> remove(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression map,
			SymbolicExpression key,
			Statement st)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = st.getLocation();
		AccessChild size = size(map, location);
		AnalysisState<A> got = get(interprocedural, state, map, key, st);
		AnalysisState<A> result = got.bottomExecution();
		for (SymbolicExpression returned : got.getExecutionExpressions())
			for (int k = 0; k <= MAX_ENTRIES; k++) {
				AnalysisState<A> bucket = bucket(analysis, context, got, map, k, st);
				if (bucket.getExecutionState().isBottom())
					continue;
				AnalysisState<A> rest = bucket;
				boolean possibly = k == MAX_ENTRIES
						|| !otherKeys(analysis, context, bucket, map, k, st).getExecutionState().isBottom();
				for (int i = Math.min(k, MAX_ENTRIES) - 1; i >= 0 && !rest.getExecutionState().isBottom(); i--) {
					AnalysisState<A> alive = withAlive(analysis, context, rest, map, i, 1, st);
					if (alive.getExecutionState().isBottom())
						continue;
					if (sameObject(analysis, context, rest, key, key(map, i, location), st)) {
						AnalysisState<A> tmp = analysis.assign(alive, alive(map, i, location), constant(0, location),
								context, st);
						tmp = analysis.assign(tmp, size, new BinaryExpression(JavaIntType.INSTANCE, size,
								constant(1, location), NumericNonOverflowingSub.INSTANCE, location), context, st);
						result = result.lub(tmp.withExecutionExpression(returned));
						rest = withAlive(analysis, context, rest, map, i, 0, st);
					} else {
						// it might be removed
						possibly = true;
						rest = analysis.assign(rest, alive(map, i, location),
								ListModel.between(alive(map, i, location), 0, 1, location), context, st);
					}
				}
				if (rest.getExecutionState().isBottom())
					continue;
				if (possibly)
					rest = maybeResize(analysis, context, rest, map, -1, st);
				result = result.lub(rest.withExecutionExpression(returned));
			}
		return result;
	}
}

package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.BoundsAndEqualities;
import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.lattices.ConstantValueIntInterval;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The product of {@link ConstantPropagationWithIntervals}, of
 * {@link JavaUpperBounds} (relations {@code x < y}, e.g., between the index of
 * a loop and the length of an array) and of the identifiers that are surely
 * equal to each identifier, because one was copied into the other (e.g., the
 * length of an array created with size {@code n}, and {@code n}). When a
 * comparison is checked, each identifier also stands for the identifiers equal
 * to it. A condition is checked by the values and by the upper bounds, keeping
 * the most precise answer. Unlike the pentagons of LiSA, the relations implied
 * by the intervals are not stored (the intervals already answer for them): this
 * avoids a closure, quadratic in the number of identifiers, after each
 * assignment.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class ConstantPropagationWithRelations
		implements
		ValueDomain<ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
				BoundsAndEqualities>> {

	private final ConstantPropagationWithIntervals values = new ConstantPropagationWithIntervals();

	private final JavaUpperBounds bounds = new JavaUpperBounds();

	@Override
	public ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
			BoundsAndEqualities> makeLattice() {
		return new ValueLatticeProduct<>(values.makeLattice(),
				new BoundsAndEqualities(bounds.makeLattice(), bounds.makeLattice()));
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
			BoundsAndEqualities> assign(
					ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
							BoundsAndEqualities> state,
					Identifier id,
					ValueExpression expression,
					ProgramPoint pp,
					SemanticOracle oracle)
					throws SemanticException {
		if (state.isBottom())
			return state;
		ValueEnvironment<DefiniteIdSet> ub = state.second.first;
		ValueEnvironment<DefiniteIdSet> eq = state.second.second;

		ValueEnvironment<
				ConstantValueIntInterval> v = values.assign(refineOperand(state.first, ub, expression, pp, oracle), id,
						expression, pp, oracle);
		// the relations of id still hold for an identifier equal to it
		ValueEnvironment<DefiniteIdSet> kept = moveRelations(ub, eq, id);
		ValueEnvironment<DefiniteIdSet> b;
		if (mayOverflow(state.first, id, expression))
			// only forgets the relations of id
			b = bounds.assign(kept, id, new PushAny(id.getStaticType(), id.getCodeLocation()), pp, oracle);
		else
			b = bounds.assign(kept, id, expression, pp, oracle);
		b = keepAfterSelfUpdate(ub, b, state.first, id, expression);
		ValueEnvironment<DefiniteIdSet> e = assignEqualities(eq, id, expression);
		return mk(state, v, b, e);
	}

	// x = x - c (or x = x + c, c > 0) without overflow: x is smaller (or
	// greater) than before, so it keeps its upper bounds (or stays an upper
	// bound of the identifiers below it), that the upper bounds forget as x is
	// assigned (e.g., j-- in a loop where j < a.length)
	private static ValueEnvironment<DefiniteIdSet> keepAfterSelfUpdate(
			ValueEnvironment<DefiniteIdSet> before,
			ValueEnvironment<DefiniteIdSet> after,
			ValueEnvironment<ConstantValueIntInterval> values,
			Identifier id,
			ValueExpression expression)
			throws SemanticException {
		if (after.isBottom() || before.isBottom() || id.isWeak() || !(expression instanceof BinaryExpression))
			return after;
		BinaryExpression be = (BinaryExpression) expression;
		boolean add = be.getOperator() instanceof AdditionOperator;
		if (!add && !(be.getOperator() instanceof SubtractionOperator))
			return after;
		if (!be.getLeft().equals(id) || !(be.getRight() instanceof Constant))
			return after;
		Object c = ((Constant) be.getRight()).getValue();
		if (!(c instanceof Integer || c instanceof Long || c instanceof Short || c instanceof Byte))
			return after;
		long delta = ((Number) c).longValue();
		if (delta == 0 || delta == Long.MIN_VALUE)
			return after;
		delta = add ? delta : -delta;
		if (mayOverflowBy(values, id, delta, id.getStaticType()))
			return after;

		if (delta < 0) {
			DefiniteIdSet own = before.getState(id);
			return own.elements.isEmpty() ? after : after.putState(id, after.getState(id).glb(own));
		}
		ValueEnvironment<DefiniteIdSet> result = after;
		for (Entry<Identifier, DefiniteIdSet> entry : before)
			if (entry.getValue().contains(id))
				result = result.putState(entry.getKey(), result.getState(entry.getKey()).add(id));
		return result;
	}

	// the upper bounds where the relations of id are also given to an
	// identifier equal to id (if any), as id is about to be assigned
	private static ValueEnvironment<DefiniteIdSet> moveRelations(
			ValueEnvironment<DefiniteIdSet> ub,
			ValueEnvironment<DefiniteIdSet> eq,
			Identifier id)
			throws SemanticException {
		if (ub.isBottom() || eq.isBottom())
			return ub;
		Set<Identifier> group = eq.getState(id).elements;
		if (group.isEmpty())
			return ub;
		Identifier other = group.iterator().next();
		ValueEnvironment<DefiniteIdSet> result = ub;
		DefiniteIdSet own = ub.getState(id);
		if (!own.elements.isEmpty())
			result = result.putState(other, result.getState(other).glb(own));
		for (Entry<Identifier, DefiniteIdSet> entry : ub)
			if (entry.getValue().contains(id) && !entry.getKey().equals(other))
				result = result.putState(entry.getKey(), result.getState(entry.getKey()).add(other));
		return result;
	}

	// x + 1 (or x - 1) cannot overflow if x has an upper bound (or a lower
	// bound) in the relations: the interval of x is restricted accordingly,
	// as the intervals give top to an operation that might overflow (e.g., the
	// increment of the index of a loop, whose interval is [0, +Inf])
	private static ValueEnvironment<ConstantValueIntInterval> refineOperand(
			ValueEnvironment<ConstantValueIntInterval> state,
			ValueEnvironment<DefiniteIdSet> ub,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (!(expression instanceof BinaryExpression))
			return state;
		BinaryExpression be = (BinaryExpression) expression;
		boolean add = be.getOperator() instanceof AdditionOperator;
		if (!add && !(be.getOperator() instanceof SubtractionOperator))
			return state;
		if (!(be.getLeft() instanceof Identifier) || !(be.getRight() instanceof Constant))
			return state;
		Object c = ((Constant) be.getRight()).getValue();
		if (!(c instanceof Integer || c instanceof Long) || ((Number) c).longValue() != 1)
			return state;
		Identifier x = (Identifier) be.getLeft();
		Type type = x.getStaticType();
		long min, max;
		if (type instanceof JavaIntType) {
			min = Integer.MIN_VALUE;
			max = Integer.MAX_VALUE;
		} else if (type instanceof JavaLongType) {
			min = Long.MIN_VALUE;
			max = Long.MAX_VALUE;
		} else
			return state;

		IntInterval range;
		if (add && hasUpperBound(ub, x, type, pp, oracle))
			// x < y <= max
			range = new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(max - 1));
		else if (!add && hasLowerBound(ub, x, type, pp, oracle))
			// min <= y < x
			range = new IntInterval(new MathNumber(min + 1), MathNumber.PLUS_INFINITY);
		else
			return state;
		try {
			ConstantValueIntInterval current = state.getState(x);
			if (current.isBottom())
				return state;
			return state.putState(x, current.glb(new ConstantValueIntInterval(ConstantValue.TOP, range)));
		} catch (SemanticException e) {
			return state;
		}
	}

	// whether x < y for some y whose values fit in the given type
	private static boolean hasUpperBound(
			ValueEnvironment<DefiniteIdSet> ub,
			Identifier x,
			Type type,
			ProgramPoint pp,
			SemanticOracle oracle) {
		for (Identifier y : ub.getState(x).elements)
			if (fitsIn(y, type, pp, oracle))
				return true;
		return false;
	}

	// whether y < x for some y whose values fit in the given type
	private static boolean hasLowerBound(
			ValueEnvironment<DefiniteIdSet> ub,
			Identifier x,
			Type type,
			ProgramPoint pp,
			SemanticOracle oracle) {
		for (Entry<Identifier, DefiniteIdSet> entry : ub)
			if (entry.getValue().contains(x) && fitsIn(entry.getKey(), type, pp, oracle))
				return true;
		return false;
	}

	// whether the values of the given identifier fit in the given type: from
	// its static type, or, if it has none (e.g., the length of an array), from
	// its runtime types
	private static boolean fitsIn(
			Identifier bound,
			Type type,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (fitsIn(bound.getStaticType(), type))
			return true;
		if (!bound.getStaticType().isUntyped() || oracle == null)
			return false;
		try {
			Set<Type> runtime = oracle.getRuntimeTypesOf(bound, pp);
			if (runtime == null || runtime.isEmpty())
				return false;
			for (Type t : runtime)
				if (!fitsIn(t, type))
					return false;
			return true;
		} catch (SemanticException e) {
			return false;
		}
	}

	private static boolean fitsIn(
			Type bound,
			Type type) {
		if (type instanceof JavaLongType)
			return bound instanceof JavaIntType || bound instanceof JavaLongType;
		return bound instanceof JavaIntType;
	}

	// whether expression is y + c or y - c (c a constant), the form that
	// gives a relation between id and y in the upper bounds, and the
	// operation might overflow (or the values are not integers), making the
	// relation wrong
	private static boolean mayOverflow(
			ValueEnvironment<ConstantValueIntInterval> state,
			Identifier id,
			ValueExpression expression) {
		if (!(expression instanceof BinaryExpression))
			return false;
		BinaryExpression be = (BinaryExpression) expression;
		boolean add = be.getOperator() instanceof AdditionOperator;
		if (!add && !(be.getOperator() instanceof SubtractionOperator))
			return false;
		if (!(be.getLeft() instanceof Identifier) || be.getLeft().equals(id) || !(be.getRight() instanceof Constant))
			return false;

		Object c = ((Constant) be.getRight()).getValue();
		if (!(c instanceof Integer || c instanceof Long || c instanceof Short || c instanceof Byte))
			return true;
		long delta = ((Number) c).longValue();
		if (delta == Long.MIN_VALUE)
			return true;
		delta = add ? delta : -delta;

		return mayOverflowBy(state, (Identifier) be.getLeft(), delta, id.getStaticType());
	}

	// whether y + delta might not fit in the given type (or the type is not
	// int or long)
	private static boolean mayOverflowBy(
			ValueEnvironment<ConstantValueIntInterval> state,
			Identifier y,
			long delta,
			Type type) {
		long min, max;
		if (type instanceof JavaIntType) {
			min = Integer.MIN_VALUE;
			max = Integer.MAX_VALUE;
		} else if (type instanceof JavaLongType) {
			min = Long.MIN_VALUE;
			max = Long.MAX_VALUE;
		} else
			return true;

		IntInterval interval = state.getState(y).getIntInterval();
		if (interval == null || interval.isBottom())
			return true;
		// y + delta within [min, max]: an infinite bound of y stands for the
		// bound of its type, and only the side moved by delta can overflow
		if (delta > 0)
			return interval.getHigh().compareTo(new MathNumber(max - delta)) > 0;
		if (delta < 0)
			return interval.getLow().compareTo(new MathNumber(min - delta)) < 0;
		return false;
	}

	// the identifiers equal to each identifier after id = expression: id
	// leaves its group, and joins the group of y if expression is y
	private static ValueEnvironment<DefiniteIdSet> assignEqualities(
			ValueEnvironment<DefiniteIdSet> eq,
			Identifier id,
			ValueExpression expression)
			throws SemanticException {
		if (eq.isBottom())
			return eq;
		ValueEnvironment<DefiniteIdSet> result = eq.putState(id, new DefiniteIdSet(Collections.emptySet()));
		// the groups are symmetric: the identifiers equal to id are the ones
		// whose group contains id
		for (Identifier member : eq.getState(id).elements) {
			Set<Identifier> others = new HashSet<>(eq.getState(member).elements);
			others.remove(id);
			result = result.putState(member, new DefiniteIdSet(others));
		}

		if (expression instanceof Identifier && !expression.equals(id)) {
			Identifier y = (Identifier) expression;
			Set<Identifier> group = new HashSet<>(result.getState(y).elements);
			group.add(y);
			group.remove(id);
			ValueEnvironment<DefiniteIdSet> joined = result.putState(id, new DefiniteIdSet(group));
			for (Identifier z : group)
				joined = joined.putState(z, result.getState(z).add(id));
			result = joined;
		}

		// a weak identifier stands for more than one location: the equalities
		// must also hold for the locations that are not assigned
		if (id.isWeak())
			result = BoundsAndEqualities.join(eq, result);
		return result;
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
			BoundsAndEqualities> smallStepSemantics(
					ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
							BoundsAndEqualities> state,
					ValueExpression expression,
					ProgramPoint pp,
					SemanticOracle oracle)
					throws SemanticException {
		if (state.isBottom())
			return state;
		return mk(state,
				values.smallStepSemantics(state.first, expression, pp, oracle),
				state.second.first,
				state.second.second);
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
			BoundsAndEqualities> assume(
					ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
							BoundsAndEqualities> state,
					ValueExpression expression,
					ProgramPoint src,
					ProgramPoint dest,
					SemanticOracle oracle)
					throws SemanticException {
		if (state.isBottom())
			return state;
		ValueEnvironment<DefiniteIdSet> ub = state.second.first;
		ValueEnvironment<DefiniteIdSet> eq = state.second.second;
		// a condition that the relations already know to be false
		if (relations(ub, eq, expression, src, oracle) == Satisfiability.NOT_SATISFIED)
			return state.bottom();
		return mk(state,
				shareIntervals(values.assume(state.first, expression, src, dest, oracle), eq, expression),
				bounds.assume(ub, expression, src, dest, oracle),
				eq);
	}

	// the intervals where the identifiers of the expression and the
	// identifiers equal to them have the same (most precise) interval, as they
	// have the same value (e.g., N >= 2 also gives a.length >= 2 if a was
	// created with size N)
	private static ValueEnvironment<ConstantValueIntInterval> shareIntervals(
			ValueEnvironment<ConstantValueIntInterval> values,
			ValueEnvironment<DefiniteIdSet> eq,
			ValueExpression expression)
			throws SemanticException {
		if (values.isBottom() || eq.isBottom())
			return values;
		Set<Identifier> ids = new HashSet<>();
		collectIdentifiers(expression, ids);
		ValueEnvironment<ConstantValueIntInterval> result = values;
		for (Identifier id : ids) {
			Set<Identifier> group = eq.getState(id).elements;
			if (group.isEmpty())
				continue;
			ConstantValueIntInterval shared = result.getState(id);
			for (Identifier other : group)
				shared = shared.glb(result.getState(other));
			if (shared.isBottom())
				return result.bottom();
			result = result.putState(id, shared);
			for (Identifier other : group)
				result = result.putState(other, shared);
		}
		return result;
	}

	private static void collectIdentifiers(
			SymbolicExpression expression,
			Set<Identifier> ids) {
		if (expression instanceof Identifier)
			ids.add((Identifier) expression);
		else if (expression instanceof BinaryExpression) {
			collectIdentifiers(((BinaryExpression) expression).getLeft(), ids);
			collectIdentifiers(((BinaryExpression) expression).getRight(), ids);
		} else if (expression instanceof UnaryExpression)
			collectIdentifiers(((UnaryExpression) expression).getExpression(), ids);
	}

	@Override
	public Satisfiability satisfies(
			ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
					BoundsAndEqualities> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return Satisfiability.BOTTOM;
		// the connectives are split here: each comparison is checked by both
		// parts (e.g., in i < 0 || i >= a.length, the intervals know the first
		// and the upper bounds the second)
		if (expression instanceof UnaryExpression
				&& ((UnaryExpression) expression).getOperator() == LogicalNegation.INSTANCE
				&& ((UnaryExpression) expression).getExpression() instanceof ValueExpression)
			return satisfies(state, (ValueExpression) ((UnaryExpression) expression).getExpression(), pp, oracle)
					.negate();
		if (expression instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression) expression;
			if ((be.getOperator() == LogicalAnd.INSTANCE || be.getOperator() == LogicalOr.INSTANCE)
					&& be.getLeft() instanceof ValueExpression && be.getRight() instanceof ValueExpression) {
				Satisfiability l = satisfies(state, (ValueExpression) be.getLeft(), pp, oracle);
				Satisfiability r = satisfies(state, (ValueExpression) be.getRight(), pp, oracle);
				return be.getOperator() == LogicalAnd.INSTANCE ? l.and(r) : l.or(r);
			}
		}
		return values.satisfies(state.first, expression, pp, oracle)
				.glb(relations(state.second.first, state.second.second, expression, pp, oracle));
	}

	// the satisfiability of a comparison between two identifiers given by the
	// upper bounds, where each identifier also stands for the identifiers
	// equal to it (e.g., i < n gives i < a.length if a was created with size
	// n); other expressions are checked by the upper bounds alone
	private Satisfiability relations(
			ValueEnvironment<DefiniteIdSet> ub,
			ValueEnvironment<DefiniteIdSet> eq,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (ub.isBottom() || eq.isBottom())
			return Satisfiability.BOTTOM;
		if (!(expression instanceof BinaryExpression))
			return bounds.satisfies(ub, expression, pp, oracle);
		BinaryExpression be = (BinaryExpression) expression;
		if (!(be.getLeft() instanceof Identifier) || !(be.getRight() instanceof Identifier))
			return bounds.satisfies(ub, expression, pp, oracle);
		Identifier x = (Identifier) be.getLeft();
		Identifier y = (Identifier) be.getRight();
		boolean lt = below(ub, eq, x, y);
		boolean gt = below(ub, eq, y, x);
		if (!lt && !gt)
			return Satisfiability.UNKNOWN;
		Object op = be.getOperator();
		if (op == ComparisonLt.INSTANCE || op == ComparisonLe.INSTANCE)
			return lt ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		if (op == ComparisonGt.INSTANCE || op == ComparisonGe.INSTANCE)
			return gt ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		if (op == ComparisonEq.INSTANCE)
			return Satisfiability.NOT_SATISFIED;
		if (op == ComparisonNe.INSTANCE)
			return Satisfiability.SATISFIED;
		return Satisfiability.UNKNOWN;
	}

	// whether x < y, for x or an identifier equal to it, and y or an
	// identifier equal to it
	private static boolean below(
			ValueEnvironment<DefiniteIdSet> ub,
			ValueEnvironment<DefiniteIdSet> eq,
			Identifier x,
			Identifier y) {
		Set<Identifier> ys = new HashSet<>(eq.getState(y).elements);
		ys.add(y);
		if (!Collections.disjoint(ub.getState(x).elements, ys))
			return true;
		for (Identifier other : eq.getState(x).elements)
			if (!Collections.disjoint(ub.getState(other).elements, ys))
				return true;
		return false;
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
					BoundsAndEqualities> state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return values.constraints(requesting, state.first, e, pp, oracle);
	}

	@Override
	public boolean canProcess(
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return values.canProcess(e, pp, oracle);
	}

	private static ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
			BoundsAndEqualities> mk(
					ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>,
							BoundsAndEqualities> state,
					ValueEnvironment<ConstantValueIntInterval> v,
					ValueEnvironment<DefiniteIdSet> b,
					ValueEnvironment<DefiniteIdSet> e) {
		if (v.isBottom() || b.isBottom() || e.isBottom())
			return state.bottom();
		return new ValueLatticeProduct<>(v, new BoundsAndEqualities(b, e));
	}
}

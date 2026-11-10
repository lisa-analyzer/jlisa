package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.UpperBoundsLattice;
import it.unive.jlisa.program.type.JavaNumericType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.UpperBounds;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.heap.allocations.AllocationSite;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The upper bounds of {@link UpperBounds} (for each identifier, the identifiers
 * that are surely greater than it, the relational part of the Pentagons of
 * Logozzo and Fähndrich) on the integral values of Java: with the intervals,
 * they give the Pentagons. By default, as in the Pentagons, only the relations
 * {@code x < y} are kept, and {@code x <= y} and {@code x == y} only pass the
 * bounds of an identifier to the other; optionally, the relations
 * {@code x <= y} are kept too. It differs from the domain of LiSA in that:
 * <ul>
 * <li>it processes only integral values: with floating point values, NaN breaks
 * the comparisons and the rounding breaks {@code x + c > x};</li>
 * <li>{@code x = y + c} relates {@code x} and {@code y} only when the sum
 * cannot overflow, which the other domains must guarantee through their
 * constraints;</li>
 * <li>a weak identifier (e.g., a summary of the arrays created at the same
 * point) keeps only the relations that hold before and after an
 * assignment;</li>
 * <li>its state is {@link UpperBoundsLattice}, whose least upper bound keeps
 * only the relations holding on both sides, and whose scopes rename the
 * identifiers inside the relations;</li>
 * <li>{@link #satisfies} also answers {@link Satisfiability#NOT_SATISFIED}, and
 * handles conjunctions, disjunctions and negations, and {@link #assume} handles
 * conjunctions and the negations of comparisons.</li>
 * </ul>
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaUpperBounds
		implements
		ValueDomain<UpperBoundsLattice> {

	// the domain of LiSA, used for the transfer functions on the relations
	private final UpperBounds bounds = new UpperBounds();

	// whether the relations x <= y are kept too (otherwise, only the strict
	// ones of the Pentagons)
	private final boolean nonStrict;

	/**
	 * Builds the domain, with the strict relations only (the upper bounds of
	 * the Pentagons).
	 */
	public JavaUpperBounds() {
		this(false);
	}

	/**
	 * Builds the domain.
	 *
	 * @param nonStrict whether the relations {@code x <= y} are kept too
	 */
	public JavaUpperBounds(
			boolean nonStrict) {
		this.nonStrict = nonStrict;
	}

	@Override
	public UpperBoundsLattice makeLattice() {
		return new UpperBoundsLattice(bounds.makeLattice());
	}

	// whether the type is an integral one (byte, short, char, int, long)
	private static boolean isIntegral(
			Type type) {
		return type instanceof JavaNumericType numType && numType.isIntegral();
	}

	@Override
	public boolean canProcess(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (!isStable(expression))
			return false;
		if (expression instanceof PushInv)
			return isIntegral(expression.getStaticType());
		Set<Type> rts;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp);
		} catch (SemanticException e) {
			return false;
		}
		return rts != null && !rts.isEmpty() && rts.stream().allMatch(JavaUpperBounds::isIntegral);
	}

	// whether the identifier keeps naming the same value until it is assigned:
	// a location of the heap does only if it is the length of an array (a
	// cell read through a non-constant index, e.g., a[j], is named after the
	// index, and names another cell when the index changes)
	private static boolean isStable(
			ValueExpression expression) {
		return !(expression instanceof AllocationSite site) || "length".equals(site.getField());
	}

	@Override
	public UpperBoundsLattice assign(
			UpperBoundsLattice state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		ValueEnvironment<DefiniteIdSet> before = state.getBounds();
		ValueEnvironment<DefiniteIdSet> le = state.getAtMost();
		if (!id.isWeak()) {
			if (nonStrict) {
				// a chain through the old value of id with a strict relation
				// stays strict (e.g., x < z and z <= r give x < r)
				before = closeMixed(before, le, id);
				le = closeThrough(le, id);
			}
			// the relations through the old value of id stay true without it
			// (e.g., x < z and z < r give x < r, before z = r)
			before = closeThrough(before, id);
		}
		// the domain of LiSA removes the relations of id, and relates x and y
		// for x = y + c without checking for overflows: it is given an unknown
		// value, to keep only the removal
		ValueEnvironment<DefiniteIdSet> after = bounds.assign(before, id,
				new PushAny(expression.getStaticType(), expression.getCodeLocation()), pp, oracle);
		after = relateSum(before, after, id, expression, pp, oracle);
		if (!id.isWeak() && expression instanceof Identifier y && !y.equals(id) && !y.isWeak() && isStable(y)
				&& canProcess(y, pp, oracle))
			// id = y: id has the relations of y (the rule of the Pentagons for
			// x == y)
			after = transfer(after, y, id);
		ValueEnvironment<DefiniteIdSet> leSum = null;
		if (!id.isWeak() && expression instanceof BinaryExpression sum && sum.getLeft() instanceof Identifier a
				&& sum.getRight() instanceof Identifier b && !a.equals(id) && !b.equals(id)
				&& expression.getStaticType() instanceof JavaNumericType type && type.isIntegral()) {
			// id = a + b with b >= 1 gives a < id, and id = a - b with b >= 1
			// gives id < a, if the operation cannot overflow
			BigDecimal[] ra = range(a, pp, oracle), rb = range(b, pp, oracle);
			boolean add = sum.getOperator() instanceof AdditionOperator;
			if (ra != null && rb != null && (add || sum.getOperator() instanceof SubtractionOperator)
					&& fits(add ? ra[0].add(rb[0]) : ra[0].subtract(rb[1]),
							add ? ra[1].add(rb[1]) : ra[1].subtract(rb[0]), type)) {
				if (add)
					after = growing(growing(after, a, rb, id), b, ra, id);
				else if (!a.isWeak() && rb[0].compareTo(BigDecimal.ONE) >= 0)
					after = after.putState(id, after.getState(id).add(a));
				if (nonStrict)
					// with b >= 0: a <= id for a + b, id <= a for a - b
					leSum = add ? atLeast(atLeast(drop(le, id), a, rb, id), b, ra, id)
							: !a.isWeak() && rb[0].signum() >= 0 ? put(drop(le, id), id, a) : null;
			}
		}
		ValueEnvironment<DefiniteIdSet> leAfter = state.getAtMost();
		if (nonStrict) {
			leAfter = leSum != null ? leSum : drop(le, id);
			if (!id.isWeak() && expression instanceof Identifier y && !y.equals(id) && !y.isWeak() && isStable(y)
					&& canProcess(y, pp, oracle))
				// id = y: id <= y and y <= id, and id has the relations of y
				leAfter = put(put(transfer(leAfter, y, id), id, y), y, id);
		}
		if (id.isWeak()) {
			// a weak identifier stands for more than one location: the
			// relations must also hold for the locations that are not
			// assigned
			after = UpperBoundsLattice.join(before, after);
			leAfter = UpperBoundsLattice.join(le, leAfter);
		}
		return new UpperBoundsLattice(after, leAfter);
	}

	// for id = y + c and id = y - c (c a non-zero integral constant), the
	// relation between id and y, if the operation cannot overflow; for
	// id = id - c (id = id + c), the relations where the old value of id is
	// the smaller (the greater) side still hold
	private ValueEnvironment<DefiniteIdSet> relateSum(
			ValueEnvironment<DefiniteIdSet> before,
			ValueEnvironment<DefiniteIdSet> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(expression instanceof BinaryExpression)
				|| !(expression.getStaticType() instanceof JavaNumericType type) || !type.isIntegral())
			return state;
		Offset sum = linear(expression, type, pp, oracle);
		if (sum == null || sum.id() == null || sum.id().isWeak())
			return state;
		Identifier y = sum.id();
		long value = sum.c();
		if (value == 0 || value == Long.MIN_VALUE)
			return state;
		boolean increases = value > 0;
		long step = Math.abs(value);
		// the bounds of y from the other domains: the result must stay in the
		// range of the type
		BigDecimal[] range = range(y, pp, oracle);
		IntInterval typeRange = JavaNumericInterval.typeBounds(type);
		BigDecimal max, min;
		try {
			max = new BigDecimal(typeRange.getHigh().toLong());
			min = new BigDecimal(typeRange.getLow().toLong());
		} catch (Exception e) {
			return state;
		}
		if (range == null)
			return state;
		if (increases) {
			if (range[1].add(BigDecimal.valueOf(step)).compareTo(max) > 0)
				return state;
			if (y.equals(id)) {
				// x < old id < id
				ValueEnvironment<DefiniteIdSet> result = state;
				if (!before.isBottom())
					for (Identifier x : before.getKeys())
						if (!x.equals(id) && before.getState(x).contains(id))
							result = result.putState(x, result.getState(x).add(id));
				return result;
			}
			// y < id
			return state.putState(y, state.getState(y).add(id));
		}
		if (range[0].subtract(BigDecimal.valueOf(step)).compareTo(min) < 0)
			return state;
		if (y.equals(id))
			// id < old id < every bound of the old id
			return before.isBottom() || !before.getKeys().contains(id) ? state
					: state.putState(id, before.getState(id));
		// id < y, and id < every bound of y
		return state.putState(id, state.getState(y).add(y));
	}

	// the given expression as an identifier plus a constant (a null identifier
	// for a constant), through additions and subtractions of constants and of
	// identifiers whose value the other domains know (e.g., x - y - 4 with
	// y == 3 is x - 7), or null. On integers of the given type the result is
	// exact up to the overflow of the final sum: the operations are modulo the
	// size of the type, and the intermediate ones may wrap freely
	private Offset linear(
			SymbolicExpression e,
			JavaNumericType type,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (e instanceof Constant k) {
			if (!(k.getValue() instanceof Number n) || n instanceof Double || n instanceof Float)
				return null;
			return new Offset(null, n.longValue());
		}
		if (e instanceof Identifier id)
			return canProcess(id, pp, oracle) ? new Offset(id, 0) : null;
		if (!(e instanceof BinaryExpression be) || !(be.getStaticType() instanceof JavaNumericType t)
				|| !t.equals(type))
			return null;
		boolean add = be.getOperator() instanceof AdditionOperator;
		if (!add && !(be.getOperator() instanceof SubtractionOperator))
			return null;
		Offset l = linear(be.getLeft(), type, pp, oracle);
		Offset r = linear(be.getRight(), type, pp, oracle);
		if (l == null || r == null)
			return null;
		if (l.id() != null && r.id() != null) {
			// two identifiers: one of them must have a known value (an
			// identifier alone keeps its relations, e.g., j = i + 1 with i ==
			// 0)
			Offset folded = constant(r, pp, oracle);
			if (folded != null)
				r = folded;
			else if ((folded = constant(l, pp, oracle)) != null)
				l = folded;
			else
				return null;
		}
		if (r.id() != null && !add)
			// the identifier is never subtracted
			return null;
		try {
			long c = add ? Math.addExact(l.c(), r.c()) : Math.subtractExact(l.c(), r.c());
			return new Offset(l.id() != null ? l.id() : r.id(), c);
		} catch (ArithmeticException ex) {
			return null;
		}
	}

	// the given identifier plus a constant as a constant, if the other domains
	// know the value of the identifier, or null
	private Offset constant(
			Offset o,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BigDecimal[] range = range(o.id(), pp, oracle);
		if (range == null || range[0].compareTo(range[1]) != 0)
			return null;
		try {
			return new Offset(null, Math.addExact(range[0].longValueExact(), o.c()));
		} catch (ArithmeticException e) {
			return null;
		}
	}

	// the range of the value of e given by the constraints of the other
	// domains, as {low, high}, or null if both bounds are not known. A range is
	// used only with both bounds (a single bound is read in opposite
	// directions by ValueDomain.makeRangeConstraints and Interval.generate)
	private BigDecimal[] range(
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!oracle.hasWholeValueAnlysis())
			return null;
		Set<BinaryExpression> constraints = oracle.constraints(this, e, pp);
		if (constraints == null)
			return null;
		BigDecimal ge = null, le = null;
		for (BinaryExpression constraint : constraints)
			if (constraint.getLeft() instanceof Constant c && c.getValue() instanceof Number n
					&& !(n instanceof Double || n instanceof Float)) {
				BigDecimal v = BigDecimal.valueOf(n.longValue());
				if (constraint.getOperator() == ComparisonEq.INSTANCE)
					return new BigDecimal[] { v, v };
				if (constraint.getOperator() == ComparisonGe.INSTANCE)
					ge = v;
				else if (constraint.getOperator() == ComparisonLe.INSTANCE)
					le = v;
			}
		if (ge == null || le == null)
			return null;
		return new BigDecimal[] { ge.min(le), ge.max(le) };
	}

	@Override
	public UpperBoundsLattice smallStepSemantics(
			UpperBoundsLattice state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return state;
	}

	@Override
	public UpperBoundsLattice assume(
			UpperBoundsLattice state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		Satisfiability sat = satisfies(state, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return state.bottom();
		if (sat == Satisfiability.SATISFIED)
			return state;
		if (expression instanceof BinaryExpression and && and.getOperator() == LogicalAnd.INSTANCE
				&& and.getLeft() instanceof ValueExpression l && and.getRight() instanceof ValueExpression r)
			return assume(assume(state, l, src, dest, oracle), r, src, dest, oracle);
		if (expression instanceof UnaryExpression u && u.getOperator() == LogicalNegation.INSTANCE
				&& u.getExpression() instanceof UnaryExpression inner && inner.getOperator() == LogicalNegation.INSTANCE
				&& inner.getExpression() instanceof ValueExpression e)
			// !!e is e
			return assume(state, e, src, dest, oracle);
		if (expression instanceof UnaryExpression u && u.getOperator() == LogicalNegation.INSTANCE) {
			BinaryExpression negated = negate(u.getExpression(), src, oracle);
			return negated == null ? state : assumeComparison(state, negated, src, dest, oracle);
		}
		if (expression instanceof BinaryExpression comparison)
			return assumeComparison(state, comparison, src, dest, oracle);
		return state;
	}

	private UpperBoundsLattice assumeComparison(
			UpperBoundsLattice state,
			BinaryExpression comparison,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(comparison.getLeft() instanceof Identifier left) || !(comparison.getRight() instanceof Identifier right)
				|| !canProcess(left, src, oracle) || !canProcess(right, src, oracle))
			return state;
		if (left.isWeak() || right.isWeak())
			// a weak identifier stands for more than one location (e.g., the
			// lengths of all the arrays created at the same point): the
			// comparison holds for one of them only
			return state;
		// the rules of the Pentagons: x < y gives y and the bounds of y to x,
		// x <= y the bounds of y to x, and x == y the bounds of each to both
		ValueEnvironment<DefiniteIdSet> strict = bounds.assume(state.getBounds(), comparison, src, dest, oracle);
		ValueEnvironment<DefiniteIdSet> le = state.getAtMost();
		if (nonStrict) {
			BinaryOperator op = comparison.getOperator();
			if (op == ComparisonLe.INSTANCE || op == ComparisonLt.INSTANCE || op == ComparisonEq.INSTANCE)
				le = put(le, left, right);
			if (op == ComparisonGe.INSTANCE || op == ComparisonGt.INSTANCE || op == ComparisonEq.INSTANCE)
				le = put(le, right, left);
		}
		return new UpperBoundsLattice(strict, le);
	}

	// the comparison equivalent to the negation of the given one, if it is a
	// comparison between integral values (with floating point values, both
	// x < y and x >= y are false when x or y is NaN), or null
	private BinaryExpression negate(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (!(expression instanceof BinaryExpression comparison)
				|| !(comparison.getLeft() instanceof ValueExpression left)
				|| !(comparison.getRight() instanceof ValueExpression right)
				|| !canProcess(left, pp, oracle) || !canProcess(right, pp, oracle))
			return null;
		BinaryOperator op = comparison.getOperator();
		BinaryOperator opposite;
		if (op == ComparisonLt.INSTANCE)
			opposite = ComparisonGe.INSTANCE;
		else if (op == ComparisonLe.INSTANCE)
			opposite = ComparisonGt.INSTANCE;
		else if (op == ComparisonGt.INSTANCE)
			opposite = ComparisonLe.INSTANCE;
		else if (op == ComparisonGe.INSTANCE)
			opposite = ComparisonLt.INSTANCE;
		else if (op == ComparisonNe.INSTANCE)
			opposite = ComparisonEq.INSTANCE;
		else
			return null;
		return new BinaryExpression(comparison.getStaticType(), left, right, opposite,
				comparison.getCodeLocation());
	}

	@Override
	public Satisfiability satisfies(
			UpperBoundsLattice state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return Satisfiability.BOTTOM;
		if (expression instanceof UnaryExpression u && u.getOperator() == LogicalNegation.INSTANCE)
			return u.getExpression() instanceof ValueExpression arg
					? satisfies(state, arg, pp, oracle).negate()
					: Satisfiability.UNKNOWN;
		if (!(expression instanceof BinaryExpression bexp))
			return Satisfiability.UNKNOWN;
		BinaryOperator op = bexp.getOperator();
		if (op == LogicalAnd.INSTANCE || op == LogicalOr.INSTANCE) {
			if (!(bexp.getLeft() instanceof ValueExpression l) || !(bexp.getRight() instanceof ValueExpression r))
				return Satisfiability.UNKNOWN;
			Satisfiability ls = satisfies(state, l, pp, oracle);
			Satisfiability rs = satisfies(state, r, pp, oracle);
			return op == LogicalAnd.INSTANCE ? ls.and(rs) : ls.or(rs);
		}
		ValueEnvironment<DefiniteIdSet> env = state.getBounds();
		ValueEnvironment<DefiniteIdSet> le = state.getAtMost();
		return satisfiesComparison(env, le, bexp, pp, oracle);
	}

	// the satisfiability of a comparison from the relations alone
	private Satisfiability satisfiesComparison(
			ValueEnvironment<DefiniteIdSet> env,
			ValueEnvironment<DefiniteIdSet> le,
			BinaryExpression bexp,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator op = bexp.getOperator();
		Satisfiability offset = satisfiesWithOffset(env, bexp, pp, oracle);
		if (offset != Satisfiability.UNKNOWN)
			return offset;
		if (!(bexp.getLeft() instanceof Identifier left) || !(bexp.getRight() instanceof Identifier right)
				|| !canProcess(left, pp, oracle) || !canProcess(right, pp, oracle))
			return Satisfiability.UNKNOWN;
		if (nonStrict)
			return satisfiesChains(env, le, op, left, right);
		// left < right or right < left, as known by the upper bounds
		boolean lt = env.getState(left).contains(right);
		boolean gt = env.getState(right).contains(left);
		if (!lt && !gt)
			return Satisfiability.UNKNOWN;
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

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			UpperBoundsLattice state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return null;
		if (e instanceof Identifier id && requesting instanceof JavaNumericInterval && canProcess(id, pp, oracle))
			return bounds(state, id, pp, oracle);
		if (e instanceof BinaryExpression b && b.getLeft() instanceof Identifier
				&& b.getRight() instanceof Identifier) {
			// e.g., true == (i < n) when i < n is known
			Satisfiability sat = satisfies(state, e, pp, oracle);
			if (sat == Satisfiability.SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
			if (sat == Satisfiability.NOT_SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
		}
		return Collections.emptySet();
	}

	/**
	 * Returns the bounds of the given identifier that follow from its relations
	 * and from the bounds of the related identifiers given by the other
	 * domains: {@code h - 1 >= id} if {@code id < y} and {@code y <= h}, and
	 * {@code l + 1 <= id} if {@code x < id} and {@code x >= l}. Each constraint
	 * is meant literally (the constant compared with the identifier).
	 */
	private Set<BinaryExpression> bounds(
			UpperBoundsLattice state,
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ValueEnvironment<DefiniteIdSet> env = state.getBounds();
		Set<Identifier> keys = env.isBottom() ? Collections.emptySet() : env.getKeys();
		// greater than id: its bounds
		Set<Identifier> greater = keys.contains(id) ? env.getState(id).elements : Collections.emptySet();
		// smaller than id: the identifiers bounded by it
		Set<Identifier> smaller = new HashSet<>();
		for (Identifier x : keys)
			if (env.getState(x).contains(id))
				smaller.add(x);
		BigDecimal high = null, low = null;
		for (Identifier y : greater) {
			BigDecimal[] range = range(y, pp, oracle);
			if (range != null)
				high = high == null ? range[1] : high.min(range[1]);
		}
		for (Identifier x : smaller) {
			BigDecimal[] range = range(x, pp, oracle);
			if (range != null)
				low = low == null ? range[0] : low.max(range[0]);
		}
		Set<BinaryExpression> result = new HashSet<>();
		Type type = pp.getProgram().getTypes().getIntegerType();
		if (high != null)
			result.addAll(ValueDomain.makeConstraint(type, high.subtract(BigDecimal.ONE).longValueExact(),
					ComparisonGe.INSTANCE, id, pp));
		if (low != null)
			result.addAll(ValueDomain.makeConstraint(type, low.add(BigDecimal.ONE).longValueExact(),
					ComparisonLe.INSTANCE, id, pp));
		return result;
	}

	// the maximum length of the chains of relations followed by
	// satisfiesWithOffset
	private static final int MAX_CHAIN = 8;

	// whether x + c may overflow, as far as the range of x given by the other
	// domains tells (e.g., not i - 1 with i >= 1)
	private boolean mayOverflow(
			Identifier x,
			long c,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BigDecimal[] range = range(x, pp, oracle);
		if (range == null)
			return true;
		Set<Type> types = oracle.getRuntimeTypesOf(x, pp);
		if (types.isEmpty())
			return true;
		BigDecimal low = range[0].add(BigDecimal.valueOf(c)), high = range[1].add(BigDecimal.valueOf(c));
		for (Type type : types) {
			if (!(type instanceof JavaNumericType numType) || !numType.isIntegral())
				return true;
			IntInterval bounds = JavaNumericInterval.typeBounds(numType);
			if (bounds.getLow().compareTo(new MathNumber(low)) > 0
					|| bounds.getHigh().compareTo(new MathNumber(high)) < 0)
				return true;
		}
		return false;
	}

	/**
	 * Returns the satisfiability of a comparison {@code x + c op y} (or
	 * {@code y op x + c}, or with {@code x - c}) between integral identifiers,
	 * from the chains of relations: on integers, a chain {@code x < ... < y} of
	 * {@code k} steps gives {@code y - x >= k}, and a chain {@code y < ... < x}
	 * of {@code k} steps gives {@code y - x <= -k} (e.g., {@code j < i} and
	 * {@code i < n} give {@code j + 1 < n}). The comparison is decided only if
	 * {@code x + c} cannot overflow, which the chains guarantee when they are
	 * long enough.
	 */
	private Satisfiability satisfiesWithOffset(
			ValueEnvironment<DefiniteIdSet> env,
			BinaryExpression comparison,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator op = comparison.getOperator();
		if (!(op == ComparisonLt.INSTANCE || op == ComparisonLe.INSTANCE || op == ComparisonGt.INSTANCE
				|| op == ComparisonGe.INSTANCE || op == ComparisonEq.INSTANCE || op == ComparisonNe.INSTANCE))
			return Satisfiability.UNKNOWN;
		Offset left = offset(comparison.getLeft()), right = offset(comparison.getRight());
		if (left == null || right == null)
			return Satisfiability.UNKNOWN;
		if (left.c() == 0 && right.c() != 0) {
			// y op x + c is x + c op' y
			Offset t = left;
			left = right;
			right = t;
			op = mirror(op);
		}
		Identifier x = left.id(), y = right.id();
		if (!canProcess(x, pp, oracle) || !canProcess(y, pp, oracle))
			return Satisfiability.UNKNOWN;
		boolean same = x.equals(y);
		long c = left.c();
		boolean exact = false;
		if (right.c() != 0 || same) {
			// x + c op y + d: if neither side overflows, it is x + (c - d) op
			// y on the integers (e.g., x + 1 > x + 2 is 1 > 2)
			if (c != 0 && mayOverflow(x, c, pp, oracle)
					|| right.c() != 0 && mayOverflow(y, right.c(), pp, oracle))
				return Satisfiability.UNKNOWN;
			try {
				c = Math.subtractExact(c, right.c());
			} catch (ArithmeticException e) {
				return Satisfiability.UNKNOWN;
			}
			exact = true;
		}
		Long lo, hi;
		if (same)
			// y - x = 0
			lo = hi = 0L;
		else {
			// bounds of y - x from the chains
			int up = longestChain(env, x, y, MAX_CHAIN, new HashSet<>());
			int down = longestChain(env, y, x, MAX_CHAIN, new HashSet<>());
			lo = up > 0 ? (long) up : null; // y - x >= up
			hi = down > 0 ? (long) -down : null; // y - x <= -down
			if (lo == null && hi == null)
				return Satisfiability.UNKNOWN;
			// x + c does not overflow: x <= y - up <= MAX - up (c <= up), or
			// x >= y + down >= MIN + down (-c <= down)
			if (!exact && (c > 0 && (lo == null || c > lo) || c < 0 && (hi == null || -c > -hi))
					&& mayOverflow(x, c, pp, oracle))
				return Satisfiability.UNKNOWN;
		}
		// x + c op y, with d = y - x in [lo, hi]
		if (op == ComparisonLt.INSTANCE)
			// d > c
			return decide(lo != null && lo > c, hi != null && hi <= c);
		if (op == ComparisonLe.INSTANCE)
			// d >= c
			return decide(lo != null && lo >= c, hi != null && hi < c);
		if (op == ComparisonGt.INSTANCE)
			// d < c
			return decide(hi != null && hi < c, lo != null && lo >= c);
		if (op == ComparisonGe.INSTANCE)
			// d <= c
			return decide(hi != null && hi <= c, lo != null && lo > c);
		if (op == ComparisonEq.INSTANCE)
			return decide(false, lo != null && lo > c || hi != null && hi < c);
		// !=
		return decide(lo != null && lo > c || hi != null && hi < c, false);
	}

	// an integral identifier plus a constant: x, x + c or x - c
	private record Offset(
			Identifier id,
			long c) {
	}

	// the given expression as an identifier plus a constant, or null
	private static Offset offset(
			SymbolicExpression e) {
		if (e instanceof Identifier id)
			return new Offset(id, 0);
		if (!(e instanceof BinaryExpression sum) || !(sum.getLeft() instanceof Identifier id)
				|| !(sum.getRight() instanceof Constant k) || !(k.getValue() instanceof Number n)
				|| n instanceof Double || n instanceof Float)
			return null;
		if (sum.getOperator() instanceof AdditionOperator)
			return new Offset(id, n.longValue());
		if (sum.getOperator() instanceof SubtractionOperator && n.longValue() != Long.MIN_VALUE)
			return new Offset(id, -n.longValue());
		return null;
	}

	private static Satisfiability decide(
			boolean surelyTrue,
			boolean surelyFalse) {
		return surelyTrue ? Satisfiability.SATISFIED
				: surelyFalse ? Satisfiability.NOT_SATISFIED : Satisfiability.UNKNOWN;
	}

	// the comparison with the operands swapped (e.g., > for <)
	private static BinaryOperator mirror(
			BinaryOperator op) {
		if (op == ComparisonLt.INSTANCE)
			return ComparisonGt.INSTANCE;
		if (op == ComparisonLe.INSTANCE)
			return ComparisonGe.INSTANCE;
		if (op == ComparisonGt.INSTANCE)
			return ComparisonLt.INSTANCE;
		if (op == ComparisonGe.INSTANCE)
			return ComparisonLe.INSTANCE;
		return op;
	}

	// the length of the longest chain from < ... < to of at most the given
	// length, or 0 if there is none
	private static int longestChain(
			ValueEnvironment<DefiniteIdSet> env,
			Identifier from,
			Identifier to,
			int max,
			Set<Identifier> visited) {
		if (max == 0 || !visited.add(from))
			return 0;
		int best = 0;
		for (Identifier next : env.getState(from).elements) {
			int length = next.equals(to) ? 1 : 0;
			int rest = longestChain(env, next, to, max - 1, visited);
			if (rest > 0)
				length = Math.max(length, rest + 1);
			best = Math.max(best, length);
		}
		visited.remove(from);
		return best;
	}

	// the relations with x < id if the added value is at least 1
	private static ValueEnvironment<DefiniteIdSet> growing(
			ValueEnvironment<DefiniteIdSet> bounds,
			Identifier x,
			BigDecimal[] added,
			Identifier id)
			throws SemanticException {
		if (!x.isWeak() && added[0].compareTo(BigDecimal.ONE) >= 0)
			return bounds.putState(x, bounds.getState(x).add(id));
		return bounds;
	}

	// whether the values from low to high are in the range of the given type
	private static boolean fits(
			BigDecimal low,
			BigDecimal high,
			JavaNumericType type) {
		IntInterval bounds = JavaNumericInterval.typeBounds(type);
		return bounds.getLow().compareTo(new MathNumber(low)) <= 0
				&& bounds.getHigh().compareTo(new MathNumber(high)) >= 0;
	}

	// the relations where to also stands for from: to has the bounds of from,
	// and each k < from has k < to
	private static ValueEnvironment<DefiniteIdSet> transfer(
			ValueEnvironment<DefiniteIdSet> bounds,
			Identifier from,
			Identifier to)
			throws SemanticException {
		if (bounds.isBottom() || bounds.isTop())
			return bounds;
		ValueEnvironment<DefiniteIdSet> result = bounds;
		if (bounds.getKeys().contains(from)) {
			Set<Identifier> all = new HashSet<>(bounds.getState(from).elements);
			if (bounds.getKeys().contains(to))
				all.addAll(bounds.getState(to).elements);
			all.remove(to);
			if (!all.isEmpty())
				result = result.putState(to, new DefiniteIdSet(all));
		}
		for (Identifier k : bounds.getKeys())
			if (!k.equals(to) && bounds.getState(k).contains(from))
				result = result.putState(k, result.getState(k).add(to));
		return result;
	}

	// the comparison decided by the chains of relations between left and
	// right, with at least one strict relation or none
	private static Satisfiability satisfiesChains(
			ValueEnvironment<DefiniteIdSet> strict,
			ValueEnvironment<DefiniteIdSet> le,
			BinaryOperator op,
			Identifier left,
			Identifier right) {
		int lr = chain(strict, le, left, right), rl = chain(strict, le, right, left);
		boolean lt = lr == 2, leq = lr > 0, gt = rl == 2, geq = rl > 0;
		if (op == ComparisonLt.INSTANCE)
			return decide(lt, geq);
		if (op == ComparisonLe.INSTANCE)
			return decide(leq, gt);
		if (op == ComparisonGt.INSTANCE)
			return decide(gt, leq);
		if (op == ComparisonGe.INSTANCE)
			return decide(geq, lt);
		if (op == ComparisonEq.INSTANCE)
			return decide(leq && geq, lt || gt);
		if (op == ComparisonNe.INSTANCE)
			return decide(lt || gt, leq && geq);
		return Satisfiability.UNKNOWN;
	}

	// 2 if a chain from from to to has a strict relation, 1 if it has only
	// non-strict ones, 0 if there is none (chains of at most MAX_CHAIN
	// relations)
	private static int chain(
			ValueEnvironment<DefiniteIdSet> strict,
			ValueEnvironment<DefiniteIdSet> le,
			Identifier from,
			Identifier to) {
		if (strict.isBottom() || le.isBottom())
			return 0;
		// the identifiers reached, with whether a strict relation was used
		Map<Identifier, Boolean> seen = new HashMap<>();
		List<Identifier> frontier = List.of(from);
		seen.put(from, false);
		int best = 0;
		for (int step = 0; step < MAX_CHAIN && !frontier.isEmpty(); step++) {
			List<Identifier> next = new ArrayList<>();
			for (Identifier n : frontier) {
				boolean s = seen.get(n);
				for (int kind = 0; kind < 2; kind++) {
					ValueEnvironment<DefiniteIdSet> env = kind == 0 ? strict : le;
					if (!env.getKeys().contains(n))
						continue;
					boolean ns = s || kind == 0;
					for (Identifier m : env.getState(n).elements) {
						if (m.equals(to)) {
							if (ns)
								return 2;
							best = 1;
							continue;
						}
						Boolean old = seen.get(m);
						if (old == null || (!old && ns)) {
							seen.put(m, ns);
							next.add(m);
						}
					}
				}
			}
			frontier = next;
		}
		return best;
	}

	// the strict relations k < t for each k <= id < t and k < id <= t
	private static ValueEnvironment<DefiniteIdSet> closeMixed(
			ValueEnvironment<DefiniteIdSet> strict,
			ValueEnvironment<DefiniteIdSet> le,
			Identifier id)
			throws SemanticException {
		if (strict.isBottom() || le.isBottom() || le.getKeys().isEmpty())
			return strict;
		Set<Identifier> strictAbove = strict.getKeys().contains(id) ? strict.getState(id).elements
				: Collections.emptySet();
		Set<Identifier> leAbove = le.getKeys().contains(id) ? le.getState(id).elements : Collections.emptySet();
		ValueEnvironment<DefiniteIdSet> result = strict;
		for (Identifier k : le.getKeys())
			if (!k.equals(id) && le.getState(k).contains(id))
				// k <= id < t
				result = putAll(result, k, strictAbove);
		for (Identifier k : strict.getKeys())
			if (!k.equals(id) && strict.getState(k).contains(id))
				// k < id <= t
				result = putAll(result, k, leAbove);
		return result;
	}

	// env with the relations of id removed, also where id is the greater side
	private static ValueEnvironment<DefiniteIdSet> drop(
			ValueEnvironment<DefiniteIdSet> env,
			Identifier id)
			throws SemanticException {
		if (env.isBottom() || env.isTop())
			return env;
		ValueEnvironment<DefiniteIdSet> result = env.forgetIdentifier(id, null);
		for (Identifier k : result.getKeys())
			if (result.getState(k).contains(id)) {
				Set<Identifier> rest = new HashSet<>(result.getState(k).elements);
				rest.remove(id);
				result = rest.isEmpty() ? result.forgetIdentifier(k, null)
						: result.putState(k, new DefiniteIdSet(rest));
			}
		return result;
	}

	// env with the relation x <= y (x < y) added
	private static ValueEnvironment<DefiniteIdSet> put(
			ValueEnvironment<DefiniteIdSet> env,
			Identifier x,
			Identifier y)
			throws SemanticException {
		if (env.isBottom() || x.equals(y))
			return env;
		Set<Identifier> all = new HashSet<>(env.getState(x).elements);
		all.add(y);
		return env.putState(x, new DefiniteIdSet(all));
	}

	// env with the relations x < t (x <= t) for each t in ts
	private static ValueEnvironment<DefiniteIdSet> putAll(
			ValueEnvironment<DefiniteIdSet> env,
			Identifier x,
			Set<Identifier> ts)
			throws SemanticException {
		Set<Identifier> all = new HashSet<>(env.getState(x).elements);
		all.addAll(ts);
		all.remove(x);
		return all.isEmpty() ? env : env.putState(x, new DefiniteIdSet(all));
	}

	// the relation x <= id if the added value is at least 0
	private static ValueEnvironment<DefiniteIdSet> atLeast(
			ValueEnvironment<DefiniteIdSet> env,
			Identifier x,
			BigDecimal[] added,
			Identifier id)
			throws SemanticException {
		if (x.isWeak() || added[0].signum() < 0)
			return env;
		return put(env, x, id);
	}

	// the relations k < t for each k < id and id < t
	private static ValueEnvironment<DefiniteIdSet> closeThrough(
			ValueEnvironment<DefiniteIdSet> bounds,
			Identifier id)
			throws SemanticException {
		if (bounds.isBottom() || bounds.isTop() || !bounds.getKeys().contains(id))
			return bounds;
		Set<Identifier> above = new HashSet<>(bounds.getState(id).elements);
		above.remove(id);
		if (above.isEmpty())
			return bounds;
		ValueEnvironment<DefiniteIdSet> result = bounds;
		for (Identifier k : bounds.getKeys()) {
			if (k.equals(id) || !bounds.getState(k).contains(id))
				continue;
			// k < id
			Set<Identifier> all = new HashSet<>(result.getState(k).elements);
			all.addAll(above);
			all.remove(k);
			result = result.putState(k, new DefiniteIdSet(all));
		}
		return result;
	}
}

package it.unive.jlisa.analysis.value;

import apron.Abstract1;
import apron.ApronException;
import apron.Scalar;
import it.unive.jlisa.lattices.ConstantValueIntInterval;
import it.unive.jlisa.lattices.ScopedApron;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.apron.Apron;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
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
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

/**
 * The product of {@link ConstantPropagationWithIntervals} and of a numerical
 * domain of Apron (e.g., polyhedra). Apron computes over the integers (or the
 * rationals) without bounds, and divides as the rationals do: it only receives
 * the operations on integer identifiers that give the same result in Java, that
 * is, additions, subtractions, multiplications and comparisons that cannot
 * overflow (checked with the intervals of the first domain), with constants
 * that are exact as doubles. Any other assignment forgets the assigned
 * identifier in Apron, and any other condition is not used by Apron. A
 * condition is checked by both domains, keeping the most precise answer.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class ConstantPropagationWithApron
		implements
		ValueDomain<ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron>> {

	// the maximum number of variables in Apron: a polyhedron can grow
	// exponentially with them
	private static final int MAX_VARIABLES = 16;

	// the constants of type long that Apron converts exactly
	private static final long MAX_EXACT = 1L << 53;

	private final ConstantPropagationWithIntervals values = new ConstantPropagationWithIntervals();

	private final Apron apron = new Apron();

	@Override
	public ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> makeLattice() {
		return new ValueLatticeProduct<>(values.makeLattice(), new ScopedApron());
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> assign(
			ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		NativeMemory.operation();
		if (state.isBottom())
			return state;
		ValueEnvironment<ConstantValueIntInterval> v = values.assign(state.first, id, expression, pp, oracle);
		ScopedApron a = state.second;
		Set<Identifier> used = new HashSet<>();
		if (NativeMemory.exhausted())
			a = a.isTop() ? a : a.top();
		else if (isInteger(id, pp, oracle)
				&& exact(state.first, state.second.getApron(), expression, pp, oracle, used)) {
			Apron assigned = apron.assign(a.getApron(), id, normalize(expression), pp, oracle);
			if (id.isWeak())
				// a weak identifier might keep its old value
				assigned = assigned.lub(a.getApron());
			used.add(id);
			a = a.with(assigned, used).limit(used, MAX_VARIABLES);
		} else
			a = a.forgetIdentifier(id, pp);
		return mk(state, v, a);
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> smallStepSemantics(
			ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		return mk(state, values.smallStepSemantics(state.first, expression, pp, oracle), state.second);
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> assume(
			ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		NativeMemory.operation();
		if (state.isBottom())
			return state;
		ValueEnvironment<ConstantValueIntInterval> v = values.assume(state.first, expression, src, dest, oracle);
		ScopedApron a = state.second;
		Set<Identifier> used = new HashSet<>();
		if (!NativeMemory.exhausted() && exact(state.first, state.second.getApron(), expression, src, oracle, used)
				&& known(a.getApron(), used))
			a = a.with(apron.assume(a.getApron(), normalize(expression), src, dest, oracle), used)
					.limit(used, MAX_VARIABLES);
		return mk(state, v, a);
	}

	@Override
	public Satisfiability satisfies(
			ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		NativeMemory.operation();
		if (state.isBottom())
			return Satisfiability.BOTTOM;
		// the connectives are split here: each comparison is checked by both
		// domains (e.g., in i < 0 || i >= n, the intervals know the first and
		// Apron the second)
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
		Satisfiability sat = values.satisfies(state.first, expression, pp, oracle);
		if (sat == Satisfiability.SATISFIED || sat == Satisfiability.NOT_SATISFIED || sat == Satisfiability.BOTTOM)
			return sat;
		Set<Identifier> used = new HashSet<>();
		if (NativeMemory.exhausted()
				|| !exact(state.first, state.second.getApron(), expression, pp, oracle, used)
				|| !known(state.second.getApron(), used))
			return sat;
		return sat.glb(apron.satisfies(state.second.getApron(), normalize(expression), pp, oracle));
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> state,
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

	// whether Apron computes expression as Java does: integer identifiers,
	// exact constants, additions, subtractions, multiplications and negations
	// whose values fit in an int (or a long, for an expression of type long),
	// comparisons and connectives; the identifiers are collected in used
	private boolean exact(
			ValueEnvironment<ConstantValueIntInterval> state,
			Apron numbers,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle,
			Set<Identifier> used)
			throws SemanticException {
		if (expression instanceof Identifier) {
			used.add((Identifier) expression);
			return isInteger((Identifier) expression, pp, oracle);
		}
		if (expression instanceof Constant) {
			Object c = ((Constant) expression).getValue();
			if (c instanceof Integer || c instanceof Short || c instanceof Byte || c instanceof Character)
				return true;
			return c instanceof Long && Math.abs((Long) c) <= MAX_EXACT;
		}
		if (expression instanceof UnaryExpression) {
			UnaryExpression ue = (UnaryExpression) expression;
			if (ue.getOperator() == LogicalNegation.INSTANCE)
				return exact(state, numbers, ue.getExpression(), pp, oracle, used);
			if (ue.getOperator() == NumericNegation.INSTANCE)
				return exact(state, numbers, ue.getExpression(), pp, oracle, used) && fits(state, numbers, ue);
			return false;
		}
		if (!(expression instanceof BinaryExpression))
			return false;
		BinaryExpression be = (BinaryExpression) expression;
		Object op = be.getOperator();
		boolean arithmetic = op instanceof AdditionOperator
				|| op instanceof SubtractionOperator
				|| op instanceof MultiplicationOperator;
		boolean condition = op == ComparisonEq.INSTANCE
				|| op == ComparisonNe.INSTANCE
				|| op == ComparisonLt.INSTANCE
				|| op == ComparisonLe.INSTANCE
				|| op == ComparisonGt.INSTANCE
				|| op == ComparisonGe.INSTANCE
				|| op == LogicalAnd.INSTANCE
				|| op == LogicalOr.INSTANCE;
		if (!arithmetic && !condition)
			return false;
		if (!exact(state, numbers, be.getLeft(), pp, oracle, used)
				|| !exact(state, numbers, be.getRight(), pp, oracle, used))
			return false;
		return !arithmetic || fits(state, numbers, be);
	}

	// whether the values of the given expression fit in an int (or in a long,
	// for an expression of type long): each identifier is bounded by its
	// interval and by Apron (e.g., j in a loop where j = 2 * i and i < n,
	// that the intervals widen to +oo), and the operations are computed
	// exactly
	private static boolean fits(
			ValueEnvironment<ConstantValueIntInterval> state,
			Apron numbers,
			ValueExpression expression) {
		BigInteger[] range = range(state, numbers, expression);
		if (range == null || range[0] == null || range[1] == null)
			return false;
		boolean isLong = expression.getStaticType() instanceof JavaLongType;
		BigInteger min = BigInteger.valueOf(isLong ? Long.MIN_VALUE : Integer.MIN_VALUE);
		BigInteger max = BigInteger.valueOf(isLong ? Long.MAX_VALUE : Integer.MAX_VALUE);
		return range[0].compareTo(min) >= 0 && range[1].compareTo(max) <= 0;
	}

	// the bounds of the values of the given expression (null for an infinite
	// bound), or null if the expression is not supported
	private static BigInteger[] range(
			ValueEnvironment<ConstantValueIntInterval> state,
			Apron numbers,
			SymbolicExpression expression) {
		if (expression instanceof Constant) {
			Object c = ((Constant) expression).getValue();
			if (c instanceof Character)
				c = (int) (Character) c;
			if (!(c instanceof Number))
				return null;
			BigInteger v = BigInteger.valueOf(((Number) c).longValue());
			return new BigInteger[] { v, v };
		}
		if (expression instanceof Identifier)
			return bounds(state, numbers, (Identifier) expression);
		if (expression instanceof UnaryExpression
				&& ((UnaryExpression) expression).getOperator() == NumericNegation.INSTANCE) {
			BigInteger[] r = range(state, numbers, ((UnaryExpression) expression).getExpression());
			if (r == null)
				return null;
			return new BigInteger[] { r[1] == null ? null : r[1].negate(), r[0] == null ? null : r[0].negate() };
		}
		if (!(expression instanceof BinaryExpression))
			return null;
		BinaryExpression be = (BinaryExpression) expression;
		BigInteger[] l = range(state, numbers, be.getLeft());
		BigInteger[] r = range(state, numbers, be.getRight());
		if (l == null || r == null)
			return null;
		Object op = be.getOperator();
		if (op instanceof AdditionOperator)
			return new BigInteger[] { add(l[0], r[0]), add(l[1], r[1]) };
		if (op instanceof SubtractionOperator)
			return new BigInteger[] { sub(l[0], r[1]), sub(l[1], r[0]) };
		if (op instanceof MultiplicationOperator) {
			if (l[0] == null || l[1] == null || r[0] == null || r[1] == null)
				return new BigInteger[] { null, null };
			BigInteger a = l[0].multiply(r[0]), b = l[0].multiply(r[1]);
			BigInteger c = l[1].multiply(r[0]), d = l[1].multiply(r[1]);
			return new BigInteger[] { a.min(b).min(c).min(d), a.max(b).max(c).max(d) };
		}
		return null;
	}

	private static BigInteger add(
			BigInteger a,
			BigInteger b) {
		return a == null || b == null ? null : a.add(b);
	}

	private static BigInteger sub(
			BigInteger a,
			BigInteger b) {
		return a == null || b == null ? null : a.subtract(b);
	}

	// the bounds of an identifier: its interval, restricted by its bounds in
	// Apron
	private static BigInteger[] bounds(
			ValueEnvironment<ConstantValueIntInterval> state,
			Apron numbers,
			Identifier id) {
		BigInteger low = null, high = null;
		IntInterval interval = state.getState(id).getIntInterval();
		if (interval != null && !interval.isBottom())
			try {
				if (interval.getLow().isFinite())
					low = BigInteger.valueOf(interval.getLow().toLong());
				if (interval.getHigh().isFinite())
					high = BigInteger.valueOf(interval.getHigh().toLong());
			} catch (MathNumberConversionException e) {
				// a bound beyond the longs: Apron alone
			}
		try {
			Abstract1 abs = numbers.getApronState();
			if (abs.getEnvironment().hasVar(id.getName())) {
				apron.Interval bound = abs.getBound(abs.getCreationManager(), id.getName());
				BigInteger l = toInteger(bound.inf(), false);
				BigInteger h = toInteger(bound.sup(), true);
				if (l != null)
					low = low == null ? l : low.max(l);
				if (h != null)
					high = high == null ? h : high.min(h);
			}
		} catch (ApronException | RuntimeException e) {
			// only the interval
		}
		return new BigInteger[] { low, high };
	}

	// a bound of Apron as an integer (rounded up for an upper bound, down for
	// a lower one), or null if it is infinite or not readable
	private static BigInteger toInteger(
			Scalar bound,
			boolean upper) {
		if (bound.isInfty() != 0)
			return null;
		String text = bound.toString();
		try {
			BigDecimal value;
			int slash = text.indexOf('/');
			if (slash >= 0)
				value = new BigDecimal(new BigInteger(text.substring(0, slash).trim()))
						.divide(new BigDecimal(new BigInteger(text.substring(slash + 1).trim())), 10,
								upper ? RoundingMode.CEILING : RoundingMode.FLOOR);
			else
				value = new BigDecimal(text.trim());
			return value.setScale(0, upper ? RoundingMode.CEILING : RoundingMode.FLOOR).toBigInteger();
		} catch (NumberFormatException | ArithmeticException e) {
			return null;
		}
	}

	// whether Apron has a variable for each of the given identifiers (a
	// condition on an identifier that Apron does not have, e.g., assigned
	// with a division, is left to the intervals)
	private static boolean known(
			Apron numbers,
			Set<Identifier> ids) {
		for (Identifier id : ids)
			if (!numbers.getApronState().getEnvironment().hasVar(id.getName()))
				return false;
		return true;
	}

	// whether the values of the given identifier are integers: from its static
	// type, or, if it has none (e.g., the length of an array), from its
	// runtime types
	private static boolean isInteger(
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (isInteger(id.getStaticType()))
			return true;
		if (!id.getStaticType().isUntyped() || oracle == null)
			return false;
		try {
			Set<Type> runtime = oracle.getRuntimeTypesOf(id, pp);
			if (runtime == null || runtime.isEmpty())
				return false;
			for (Type t : runtime)
				if (!isInteger(t))
					return false;
			return true;
		} catch (SemanticException e) {
			return false;
		}
	}

	private static boolean isInteger(
			Type type) {
		return type instanceof JavaIntType
				|| type instanceof JavaLongType
				|| type instanceof JavaShortType
				|| type instanceof JavaByteType
				|| type instanceof JavaCharType;
	}

	// the constants of type short, byte and char become constants of type int,
	// that Apron converts
	private static ValueExpression normalize(
			ValueExpression expression) {
		if (expression instanceof Constant) {
			Constant c = (Constant) expression;
			Object value = c.getValue();
			if (value instanceof Short || value instanceof Byte)
				return new Constant(JavaIntType.INSTANCE, ((Number) value).intValue(), c.getCodeLocation());
			if (value instanceof Character)
				return new Constant(JavaIntType.INSTANCE, (int) (Character) value, c.getCodeLocation());
			return c;
		}
		if (expression instanceof UnaryExpression) {
			UnaryExpression ue = (UnaryExpression) expression;
			ValueExpression inner = normalize((ValueExpression) ue.getExpression());
			if (inner == ue.getExpression())
				return ue;
			return new UnaryExpression(ue.getStaticType(), inner, ue.getOperator(), ue.getCodeLocation());
		}
		if (expression instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression) expression;
			ValueExpression l = normalize((ValueExpression) be.getLeft());
			ValueExpression r = normalize((ValueExpression) be.getRight());
			if (l == be.getLeft() && r == be.getRight())
				return be;
			return new BinaryExpression(be.getStaticType(), l, r, be.getOperator(), be.getCodeLocation());
		}
		return expression;
	}

	private static ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> mk(
			ValueLatticeProduct<ValueEnvironment<ConstantValueIntInterval>, ScopedApron> state,
			ValueEnvironment<ConstantValueIntInterval> v,
			ScopedApron a) {
		if (v.isBottom() || a.isBottom())
			return state.bottom();
		return new ValueLatticeProduct<>(v, a);
	}
}

package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.FPFlags;
import it.unive.jlisa.lattices.FPFlags.Kind;
import it.unive.jlisa.program.operator.JavaMathAbsOperator;
import it.unive.jlisa.program.operator.JavaMathAtanOperator;
import it.unive.jlisa.program.operator.JavaMathCosOperator;
import it.unive.jlisa.program.operator.JavaMathExpOperator;
import it.unive.jlisa.program.operator.JavaMathFloorOperator;
import it.unive.jlisa.program.operator.JavaMathMax;
import it.unive.jlisa.program.operator.JavaMathMin;
import it.unive.jlisa.program.operator.JavaMathSinOperator;
import it.unive.jlisa.program.operator.JavaMathTanOperator;
import it.unive.jlisa.program.operator.JavaMathToRadiansOperator;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaNumericType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
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
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The kinds of value of the floating point values ({@code float} and
 * {@code double}): finite number, NaN, plus infinity, minus infinity (see
 * {@link FPFlags}). It complements the intervals, which describe where a value
 * is on the line but cannot say whether it may be NaN: it answers the
 * constraint {@code true == (x == x)} when {@code x} is not NaN, which lets the
 * intervals refine on negated comparisons (see {@link JavaNumericInterval}).
 * Before saying that an operation may overflow to an infinity, it asks the
 * other domains for the bounds of the result.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaFPFlags
		implements
		BaseNonRelationalValueDomain<FPFlags> {

	private static final Set<Kind> INFINITIES = Collections.unmodifiableSet(EnumSet.of(Kind.PLUS_INF, Kind.MINUS_INF));

	@Override
	public FPFlags top() {
		return FPFlags.TOP;
	}

	@Override
	public FPFlags bottom() {
		return FPFlags.BOTTOM;
	}

	// whether the type is float or double
	private static boolean isFloatingPoint(
			Type type) {
		return type instanceof JavaNumericType numType && !numType.isIntegral();
	}

	@Override
	public boolean canProcess(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression instanceof PushInv)
			return isFloatingPoint(expression.getStaticType());
		Set<Type> rts;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp);
		} catch (SemanticException e) {
			return false;
		}
		return rts != null && rts.stream().anyMatch(JavaFPFlags::isFloatingPoint);
	}

	@Override
	public FPFlags evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Double || constant.getValue() instanceof Float)
			return FPFlags.of(((Number) constant.getValue()).doubleValue());
		if (constant.getValue() instanceof Number)
			return FPFlags.ONLY_FINITE;
		return top();
	}

	@Override
	public FPFlags evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return top();
	}

	@Override
	public FPFlags evalIdentifier(
			Identifier id,
			ValueEnvironment<FPFlags> environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (JavaNumericInterval.isIntegral(id, pp, oracle))
			// an integral value converted to floating point (e.g., x + i)
			return FPFlags.ONLY_FINITE;
		return environment.getState(id);
	}

	@Override
	public FPFlags evalUnaryExpression(
			UnaryExpression expression,
			FPFlags arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (arg.isBottom())
			return arg;
		Object operator = expression.getOperator();
		if (operator == NumericNegation.INSTANCE)
			return swapInfinities(arg);
		if (operator instanceof JavaMathFloorOperator || operator instanceof JavaMathToRadiansOperator)
			return arg;
		EnumSet<Kind> kinds = EnumSet.of(Kind.FINITE);
		if (arg.contains(Kind.NAN))
			kinds.add(Kind.NAN);
		if (operator instanceof JavaMathAbsOperator) {
			if (!arg.contains(Kind.FINITE))
				kinds.remove(Kind.FINITE);
			if (arg.mayBeInfinite())
				kinds.add(Kind.PLUS_INF);
			return FPFlags.of(kinds);
		}
		if (operator instanceof JavaMathSinOperator || operator instanceof JavaMathCosOperator
				|| operator instanceof JavaMathTanOperator) {
			// finite for finite arguments, NaN for infinities
			if (arg.mayBeInfinite())
				kinds.add(Kind.NAN);
			return FPFlags.of(kinds);
		}
		if (operator instanceof JavaMathAtanOperator)
			return FPFlags.of(kinds);
		if (operator instanceof JavaMathExpOperator) {
			// exp(-Inf) is 0, exp of a large value is +Inf
			kinds.add(Kind.PLUS_INF);
			return FPFlags.of(kinds);
		}
		return top();
	}

	private static FPFlags swapInfinities(
			FPFlags flags) {
		EnumSet<Kind> kinds = EnumSet.noneOf(Kind.class);
		for (Kind kind : flags)
			kinds.add(kind == Kind.PLUS_INF ? Kind.MINUS_INF : kind == Kind.MINUS_INF ? Kind.PLUS_INF : kind);
		return FPFlags.of(kinds);
	}

	@Override
	public FPFlags evalBinaryExpression(
			BinaryExpression expression,
			FPFlags left,
			FPFlags right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isBottom() || right.isBottom())
			return bottom();
		BinaryOperator operator = expression.getOperator();
		if (operator instanceof SubtractionOperator)
			return sum(expression, left, swapInfinities(right), pp, oracle);
		if (operator instanceof AdditionOperator)
			return sum(expression, left, right, pp, oracle);
		EnumSet<Kind> kinds = EnumSet.noneOf(Kind.class);
		if (left.contains(Kind.NAN) || right.contains(Kind.NAN))
			kinds.add(Kind.NAN);
		boolean finite = left.contains(Kind.FINITE) && right.contains(Kind.FINITE);
		if (operator instanceof MultiplicationOperator) {
			if (finite) {
				kinds.add(Kind.FINITE);
				if (mayOverflow(expression, pp, oracle))
					kinds.addAll(INFINITIES);
			}
			if (left.mayBeInfinite() || right.mayBeInfinite()) {
				// the sign is not tracked, and 0 times an infinity is NaN
				kinds.addAll(INFINITIES);
				if (left.contains(Kind.FINITE) || right.contains(Kind.FINITE))
					kinds.add(Kind.NAN);
			}
			return FPFlags.of(kinds);
		}
		if (operator instanceof DivisionOperator) {
			if (finite) {
				kinds.add(Kind.FINITE);
				if (mayBeZero(expression.getRight(), pp, oracle)) {
					kinds.add(Kind.NAN);
					kinds.addAll(INFINITIES);
				}
				if (mayOverflow(expression, pp, oracle))
					kinds.addAll(INFINITIES);
			}
			if (left.contains(Kind.FINITE) && right.mayBeInfinite())
				kinds.add(Kind.FINITE);
			if (left.mayBeInfinite() && right.contains(Kind.FINITE))
				kinds.addAll(INFINITIES);
			if (left.mayBeInfinite() && right.mayBeInfinite())
				kinds.add(Kind.NAN);
			return FPFlags.of(kinds);
		}
		if (operator instanceof RemainderOperator || operator instanceof ModuloOperator) {
			kinds.add(Kind.FINITE);
			if (left.mayBeInfinite() || right.contains(Kind.FINITE) && mayBeZero(expression.getRight(), pp, oracle))
				kinds.add(Kind.NAN);
			return FPFlags.of(kinds);
		}
		if (operator instanceof JavaMathMax || operator instanceof JavaMathMin)
			return left.lub(right);
		return top();
	}

	// the kinds of left + right
	private FPFlags sum(
			BinaryExpression expression,
			FPFlags left,
			FPFlags right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		EnumSet<Kind> kinds = EnumSet.noneOf(Kind.class);
		if (left.contains(Kind.NAN) || right.contains(Kind.NAN)
				|| left.contains(Kind.PLUS_INF) && right.contains(Kind.MINUS_INF)
				|| left.contains(Kind.MINUS_INF) && right.contains(Kind.PLUS_INF))
			kinds.add(Kind.NAN);
		if (left.contains(Kind.PLUS_INF) || right.contains(Kind.PLUS_INF))
			kinds.add(Kind.PLUS_INF);
		if (left.contains(Kind.MINUS_INF) || right.contains(Kind.MINUS_INF))
			kinds.add(Kind.MINUS_INF);
		if (left.contains(Kind.FINITE) && right.contains(Kind.FINITE)) {
			kinds.add(Kind.FINITE);
			if (mayOverflow(expression, pp, oracle))
				kinds.addAll(INFINITIES);
		}
		return FPFlags.of(kinds);
	}

	// whether the result of the operation, computed on finite operands, may
	// exceed the largest value of its type: false only when the other domains
	// bound it
	private boolean mayOverflow(
			BinaryExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		double max = oracle.getRuntimeTypesOf(expression, pp).stream().allMatch(t -> t == JavaFloatType.INSTANCE)
				? Float.MAX_VALUE
				: Double.MAX_VALUE;
		Double[] bounds = bounds(expression, pp, oracle);
		return bounds == null || bounds[0] == null || bounds[1] == null || bounds[0] < -max || bounds[1] > max;
	}

	// whether the value may be zero: false only when the other domains bound
	// it away from zero
	private boolean mayBeZero(
			Object expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(expression instanceof ValueExpression e))
			return true;
		if (e instanceof Constant c && c.getValue() instanceof Number n)
			return n.doubleValue() == 0;
		Double[] bounds = bounds(e, pp, oracle);
		return bounds == null || !(bounds[0] != null && bounds[0] > 0 || bounds[1] != null && bounds[1] < 0);
	}

	// the bounds of the value given by the constraints of the other domains,
	// as {low, high} (null when unknown), or null if no other domain is
	// available. A range is used only when both of its bounds are given: the
	// constraints of a single bound are written as c >= e by
	// ValueDomain.makeRangeConstraints and read as c <= e by Interval.generate
	private Double[] bounds(
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!oracle.hasWholeValueAnlysis())
			return null;
		Set<BinaryExpression> constraints = oracle.constraints(this, e, pp);
		if (constraints == null)
			return null;
		Double ge = null, le = null;
		for (BinaryExpression constraint : constraints)
			if (constraint.getLeft() instanceof Constant c && c.getValue() instanceof Number n) {
				double v = n.doubleValue();
				if (constraint.getOperator() == ComparisonEq.INSTANCE)
					return new Double[] { v, v };
				if (constraint.getOperator() == ComparisonGe.INSTANCE)
					ge = v;
				else if (constraint.getOperator() == ComparisonLe.INSTANCE)
					le = v;
			}
		if (ge == null || le == null)
			return new Double[] { null, null };
		return new Double[] { Math.min(ge, le), Math.max(ge, le) };
	}

	@Override
	public FPFlags evalTypeCast(
			BinaryExpression cast,
			FPFlags left,
			FPFlags right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return convert(cast, left, pp, oracle);
	}

	@Override
	public FPFlags evalTypeConv(
			BinaryExpression conv,
			FPFlags left,
			FPFlags right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return convert(conv, left, pp, oracle);
	}

	private FPFlags convert(
			BinaryExpression conversion,
			FPFlags arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(conversion.getLeft() instanceof ValueExpression operand))
			return top();
		if (JavaNumericInterval.isIntegral(operand, pp, oracle))
			// int and long are finite as float and double
			return FPFlags.ONLY_FINITE;
		if (conversion.getStaticType() == JavaFloatType.INSTANCE && arg.contains(Kind.FINITE)) {
			// a double may be too large for a float
			EnumSet<Kind> kinds = EnumSet.copyOf(arg.elements());
			kinds.addAll(INFINITIES);
			return FPFlags.of(kinds);
		}
		return arg;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			FPFlags left,
			FPFlags right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isBottom() || right.isBottom())
			return Satisfiability.BOTTOM;
		BinaryOperator operator = expression.getOperator();
		boolean nanOperand = left.equals(FPFlags.of(Kind.NAN)) || right.equals(FPFlags.of(Kind.NAN));
		boolean same = expression.getLeft().equals(expression.getRight());
		if (operator == ComparisonEq.INSTANCE) {
			if (nanOperand)
				return Satisfiability.NOT_SATISFIED;
			if (same && !left.contains(Kind.NAN))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}
		if (operator == ComparisonNe.INSTANCE) {
			if (nanOperand)
				return Satisfiability.SATISFIED;
			if (same && !left.contains(Kind.NAN))
				return Satisfiability.NOT_SATISFIED;
			return Satisfiability.UNKNOWN;
		}
		if (isOrdered(operator) && nanOperand)
			// every ordered comparison with NaN is false
			return Satisfiability.NOT_SATISFIED;
		return Satisfiability.UNKNOWN;
	}

	private static boolean isOrdered(
			BinaryOperator operator) {
		return operator == ComparisonLt.INSTANCE || operator == ComparisonLe.INSTANCE
				|| operator == ComparisonGt.INSTANCE || operator == ComparisonGe.INSTANCE;
	}

	@Override
	public ValueEnvironment<FPFlags> assume(
			ValueEnvironment<FPFlags> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (environment.isBottom())
			return environment;
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;
		// the negations are not removed: !(x <= 5) holds for NaN, while x > 5
		// does not
		return assume(environment, expression, true, src, oracle);
	}

	private ValueEnvironment<FPFlags> assume(
			ValueEnvironment<FPFlags> environment,
			ValueExpression expression,
			boolean holds,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (environment.isBottom())
			return environment;
		if (expression instanceof UnaryExpression u && u.getOperator() == LogicalNegation.INSTANCE
				&& u.getExpression() instanceof ValueExpression inner)
			return assume(environment, inner, !holds, pp, oracle);
		if (!(expression instanceof BinaryExpression b) || !(b.getLeft() instanceof ValueExpression left)
				|| !(b.getRight() instanceof ValueExpression right))
			return environment;
		BinaryOperator operator = b.getOperator();
		if (operator == LogicalAnd.INSTANCE && holds || operator == LogicalOr.INSTANCE && !holds)
			// both conjuncts hold (or both disjuncts do not)
			return assume(assume(environment, left, holds, pp, oracle), right, holds, pp, oracle);
		if (holds && (isOrdered(operator) || operator == ComparisonEq.INSTANCE)
				|| !holds && operator == ComparisonNe.INSTANCE) {
			if (holds && isOrdered(operator))
				environment = removeInfinities(environment, operator, left, right, pp, oracle);
			// the comparison holds, hence no operand is NaN
			environment = remove(environment, left, Kind.NAN, pp, oracle);
			return remove(environment, right, Kind.NAN, pp, oracle);
		}
		if (left instanceof Identifier id && left.equals(right)
				&& (holds && operator == ComparisonNe.INSTANCE || !holds && operator == ComparisonEq.INSTANCE)
				&& canProcess(id, pp, oracle)) {
			// x != x holds only for NaN
			FPFlags refined = environment.getState(id).glb(FPFlags.of(Kind.NAN));
			return refined.isBottom() ? environment.bottom() : environment.putState(id, refined);
		}
		return environment;
	}

	// the ordered comparison holds: the smaller operand is not +Inf when the
	// comparison is strict or the greater cannot be +Inf (e.g., x <= 5 or
	// x < y), and the greater is not -Inf in the symmetric case
	private ValueEnvironment<FPFlags> removeInfinities(
			ValueEnvironment<FPFlags> environment,
			BinaryOperator operator,
			ValueExpression left,
			ValueExpression right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		boolean swap = operator == ComparisonGt.INSTANCE || operator == ComparisonGe.INSTANCE;
		boolean strict = operator == ComparisonLt.INSTANCE || operator == ComparisonGt.INSTANCE;
		ValueExpression low = swap ? right : left;
		ValueExpression high = swap ? left : right;
		FPFlags lowFlags = eval(environment, low, pp, oracle);
		FPFlags highFlags = eval(environment, high, pp, oracle);
		if (strict || !highFlags.contains(Kind.PLUS_INF))
			environment = remove(environment, low, Kind.PLUS_INF, pp, oracle);
		if (strict || !lowFlags.contains(Kind.MINUS_INF))
			environment = remove(environment, high, Kind.MINUS_INF, pp, oracle);
		return environment;
	}

	private ValueEnvironment<FPFlags> remove(
			ValueEnvironment<FPFlags> environment,
			ValueExpression operand,
			Kind kind,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (environment.isBottom() || !(operand instanceof Identifier id) || !id.canBeAssigned()
				|| !canProcess(id, pp, oracle))
			return environment;
		FPFlags refined = environment.getState(id).without(kind);
		return refined.isBottom() ? environment.bottom() : environment.putState(id, refined);
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<FPFlags> state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return null;
		if (e instanceof BinaryExpression b && (isOrdered(b.getOperator()) || b.getOperator() == ComparisonEq.INSTANCE
				|| b.getOperator() == ComparisonNe.INSTANCE)) {
			// e.g., true == (x == x) when x is not NaN
			Satisfiability sat = satisfies(state, e, pp, oracle);
			if (sat == Satisfiability.SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
			if (sat == Satisfiability.NOT_SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
		}
		return Collections.emptySet();
	}
}

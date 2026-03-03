package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.JavaFlaggedInterval;
import it.unive.jlisa.lattices.JavaTypeKind;
import it.unive.jlisa.lattices.flags.InfinityFlag;
import it.unive.jlisa.lattices.flags.IntegralityFlag;
import it.unive.jlisa.lattices.flags.NaNFlag;
import it.unive.jlisa.lattices.flags.ParityFlag;
import it.unive.jlisa.lattices.flags.ProvenanceFlag;
import it.unive.jlisa.program.operator.JavaByteCompareOperator;
import it.unive.jlisa.program.operator.JavaDoubleCompareOperator;
import it.unive.jlisa.program.operator.JavaFloatCompareOperator;
import it.unive.jlisa.program.operator.JavaIntegerCompareOperator;
import it.unive.jlisa.program.operator.JavaLongCompareOperator;
import it.unive.jlisa.program.operator.JavaLongRotateRightOperator;
import it.unive.jlisa.program.operator.JavaMathAbsOperator;
import it.unive.jlisa.program.operator.JavaMathAcosOperator;
import it.unive.jlisa.program.operator.JavaMathAsinOperator;
import it.unive.jlisa.program.operator.JavaMathAtanOperator;
import it.unive.jlisa.program.operator.JavaMathCosOperator;
import it.unive.jlisa.program.operator.JavaMathExpOperator;
import it.unive.jlisa.program.operator.JavaMathFloorOperator;
import it.unive.jlisa.program.operator.JavaMathLog10Operator;
import it.unive.jlisa.program.operator.JavaMathLogOperator;
import it.unive.jlisa.program.operator.JavaMathMax;
import it.unive.jlisa.program.operator.JavaMathMin;
import it.unive.jlisa.program.operator.JavaMathRoundOperator;
import it.unive.jlisa.program.operator.JavaMathSinOperator;
import it.unive.jlisa.program.operator.JavaMathSqrtOperator;
import it.unive.jlisa.program.operator.JavaMathTanOperator;
import it.unive.jlisa.program.operator.JavaMathToRadiansOperator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.Set;
import java.util.function.Function;

/**
 * A non-relational value domain that tracks {@link JavaFlaggedInterval}
 * elements — typed intervals with provenance, under-approximation, and flag
 * annotations — replacing the simpler {@link JavaNumericInterval}.
 * <p>
 * Key improvements over the plain interval domain:
 * <ol>
 * <li><b>Type-bounded overflow</b> — arithmetic results are clamped to the Java
 * type's range, preventing silent over-approximation past
 * {@code Integer.MAX_VALUE + 1}.</li>
 * <li><b>Type-refined nondet</b> — {@code nondetInt()} yields
 * {@code [MIN_INT, MAX_INT]} instead of {@code ⊤}.</li>
 * <li><b>Provenance tracking</b> — distinguishes input-exact values from
 * analysis-approximate values, enabling
 * {@link ExistentialSatisfiabilityProvider}.</li>
 * <li><b>Under-approximation</b> — for input-provenance values, tracks which
 * values are guaranteed concretely reachable.</li>
 * <li><b>Flag precision</b> — parity, NaN, infinity, and integrality flags
 * allow more precise satisfiability decisions.</li>
 * </ol>
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class TypedFlaggedJavaInterval
		implements
		BaseNonRelationalValueDomain<JavaFlaggedInterval> {

	@Override
	public JavaFlaggedInterval top() {
		return JavaFlaggedInterval.TOP;
	}

	@Override
	public JavaFlaggedInterval bottom() {
		return JavaFlaggedInterval.BOTTOM;
	}

	// -------------------------------------------------------------------------
	// canProcess
	// -------------------------------------------------------------------------

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		Type t = expression.getStaticType();
		if (t != null && t.isNumericType())
			return true;
		// Also handle untyped / boolean expressions that may carry numeric info
		return t == null || t.isUntyped() || t.isBooleanType();
	}

	// -------------------------------------------------------------------------
	// evalConstant
	// -------------------------------------------------------------------------

	@Override
	public JavaFlaggedInterval evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		Object val = constant.getValue();
		if (!(val instanceof Number))
			return bottom();
		JavaTypeKind kind = JavaTypeKind.fromType(constant.getStaticType());
		double dv = ((Number) val).doubleValue();
		if (kind.isInteger() || kind == JavaTypeKind.UNKNOWN) {
			if (Double.isNaN(dv) || Double.isInfinite(dv))
				return bottom();
			return JavaFlaggedInterval.ofConstant((long) dv, kind);
		}
		// float / double constant
		return JavaFlaggedInterval.ofDoubleConstant(dv, kind);
	}

	// -------------------------------------------------------------------------
	// evalPushAny
	// -------------------------------------------------------------------------

	/**
	 * Evaluates a {@link PushAny} expression.
	 * <p>
	 * Two cases are distinguished:
	 * <ul>
	 * <li>{@link PushFromConstraints} — produced by LiSA for values that carry
	 * structural constraints (e.g., array {@code length ≥ 0}). These are
	 * analysis-level over-approximations: we compute the interval from the
	 * constraints and assign {@link ProvenanceFlag#ANALYSIS} provenance (no
	 * under-approximation, no INPUT flag).</li>
	 * <li>Plain {@link PushAny} — emitted for truly non-deterministic inputs
	 * (e.g., return values of unresolved {@code Verifier.nondetXxx()} open
	 * calls). We assign {@link ProvenanceFlag#INPUT} provenance and the full
	 * type-bounded range, enabling existential satisfiability reasoning in
	 * {@link ExistentialSatisfiabilityProvider}.</li>
	 * </ul>
	 */
	@Override
	public JavaFlaggedInterval evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle) {
		JavaTypeKind kind = typeFromOracle(pushAny, pp, oracle);
		if (pushAny instanceof PushFromConstraints pfc) {
			// Structural constraint (e.g., array length >= 0): compute the
			// constrained interval using LiSA's built-in generator, then
			// wrap it with ANALYSIS provenance and no under-approximation.
			IntInterval constrained;
			try {
				constrained = IntInterval.TOP.generate(pfc.getConstraints(), pp);
			} catch (SemanticException e) {
				constrained = kind.toBoundedInterval();
			}
			return JavaFlaggedInterval.ofAnalysis(constrained, kind);
		}
		// Non-deterministic input: full type-bounded range with INPUT
		// provenance.
		return JavaFlaggedInterval.ofNonDet(kind);
	}

	// -------------------------------------------------------------------------
	// evalUnaryExpression
	// -------------------------------------------------------------------------

	@Override
	public JavaFlaggedInterval evalUnaryExpression(
			UnaryExpression expression,
			JavaFlaggedInterval arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (arg.isTop() || arg.isBottom())
			return arg;

		IntInterval over = arg.getOver();
		Double l = null, h = null;
		try {
			if (!over.lowIsMinusInfinity())
				l = over.getLow().toDouble();
		} catch (MathNumberConversionException e) {
			// left at null
		}
		try {
			if (!over.highIsPlusInfinity())
				h = over.getHigh().toDouble();
		} catch (MathNumberConversionException e) {
			// left at null
		}

		UnaryOperator op = expression.getOperator();

		if (op instanceof JavaMathSinOperator)
			return trigonometric(arg, Math::sin, 4 * Math.PI);
		if (op instanceof JavaMathCosOperator)
			return trigonometric(arg, Math::cos, 4 * Math.PI);
		if (op instanceof JavaMathTanOperator)
			return trigonometric(arg, Math::tan, Math.PI);

		if (op instanceof JavaMathAsinOperator)
			return withOver(arg, evalAsin(over, l, h), NaNFlag.DEFINITELY_NOT_NAN,
					InfinityFlag.DEFINITELY_FINITE, IntegralityFlag.NON_INTEGRAL);
		if (op instanceof JavaMathAcosOperator)
			return withOver(arg, evalAcos(over, l, h), NaNFlag.DEFINITELY_NOT_NAN,
					InfinityFlag.DEFINITELY_FINITE, IntegralityFlag.NON_INTEGRAL);
		if (op instanceof JavaMathAtanOperator)
			return withOver(arg, evalAtan(over, l, h), NaNFlag.DEFINITELY_NOT_NAN,
					InfinityFlag.DEFINITELY_FINITE, IntegralityFlag.NON_INTEGRAL);

		if (op instanceof JavaMathToRadiansOperator)
			return withOver(arg, evalToRadians(over, l, h), NaNFlag.DEFINITELY_NOT_NAN,
					InfinityFlag.DEFINITELY_FINITE, IntegralityFlag.NON_INTEGRAL);

		if (op instanceof JavaMathSqrtOperator) {
			IntInterval sqrtOver = evalSqrt(over);
			NaNFlag nanFlag = (over.getLow().compareTo(MathNumber.ZERO) < 0)
					? NaNFlag.POSSIBLY_NAN
					: NaNFlag.DEFINITELY_NOT_NAN;
			return withOver(arg, sqrtOver, nanFlag, InfinityFlag.DEFINITELY_FINITE,
					IntegralityFlag.UNKNOWN_INTEGRALITY);
		}

		if (op instanceof JavaMathLogOperator)
			return withOver(arg, evalLog(over, l, h, Math::log), NaNFlag.DEFINITELY_NOT_NAN,
					InfinityFlag.POSSIBLY_INFINITE_OR_FINITE, IntegralityFlag.NON_INTEGRAL);
		if (op instanceof JavaMathLog10Operator)
			return withOver(arg, evalLog(over, l, h, Math::log10), NaNFlag.DEFINITELY_NOT_NAN,
					InfinityFlag.POSSIBLY_INFINITE_OR_FINITE, IntegralityFlag.NON_INTEGRAL);

		if (op instanceof JavaMathExpOperator)
			return withOver(arg, evalExp(over, l, h), NaNFlag.DEFINITELY_NOT_NAN,
					InfinityFlag.POSSIBLY_INFINITE_OR_FINITE, IntegralityFlag.NON_INTEGRAL);

		if (op instanceof JavaMathFloorOperator)
			return new JavaFlaggedInterval(over, arg.getUnder(), arg.getType(), arg.getProvenance(),
					IntegralityFlag.INTEGRAL, arg.getNan(), arg.getInfinity(), arg.getParity());
		if (op instanceof JavaMathRoundOperator)
			return new JavaFlaggedInterval(over, arg.getUnder(), arg.getType(), arg.getProvenance(),
					IntegralityFlag.INTEGRAL, arg.getNan(), arg.getInfinity(), ParityFlag.BOTTOM);

		if (op instanceof JavaMathAbsOperator) {
			IntInterval absOver = evalAbs(over);
			return new JavaFlaggedInterval(absOver, null, arg.getType(), arg.getProvenance(),
					arg.getIntegrality(), arg.getNan(), arg.getInfinity(), arg.getParity());
		}

		return top();
	}

	// -------------------------------------------------------------------------
	// evalBinaryExpression
	// -------------------------------------------------------------------------

	@Override
	public JavaFlaggedInterval evalBinaryExpression(
			BinaryExpression expression,
			JavaFlaggedInterval left,
			JavaFlaggedInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return top();
		if (left.isBottom() || right.isBottom())
			return bottom();

		BinaryOperator op = expression.getOperator();

		if (op instanceof JavaMathMax)
			return mergeFlags(left, right,
					new IntInterval(left.getOver().getLow().max(right.getOver().getLow()),
							left.getOver().getHigh().max(right.getOver().getHigh())),
					null);
		if (op instanceof JavaMathMin)
			return mergeFlags(left, right,
					new IntInterval(left.getOver().getLow().min(right.getOver().getLow()),
							left.getOver().getHigh().min(right.getOver().getHigh())),
					null);

		if (op instanceof JavaLongRotateRightOperator)
			return new IntInterval(-1, 1).isSingleton() ? bottom() : intervalResult(-1, 1, left, right);
		if (op instanceof JavaLongCompareOperator
				|| op instanceof JavaFloatCompareOperator
				|| op instanceof JavaDoubleCompareOperator
				|| op instanceof JavaByteCompareOperator
				|| op instanceof JavaIntegerCompareOperator)
			return intervalResult(-1, 1, left, right);

		// Delegate to the parent interval arithmetic (LiSA's Interval logic)
		// by calling evalBinaryOnIntervals and then wrapping with flags
		IntInterval leftOver = left.getOver();
		IntInterval rightOver = right.getOver();

		// Use LiSA's Interval arithmetic via the parent class method
		IntInterval resultOver = evalIntervalBinary(op, leftOver, rightOver);
		if (resultOver == null)
			return top();

		JavaTypeKind resultType = left.getType().lub(right.getType());
		JavaFlaggedInterval result = mergeFlags(left, right, resultOver, null);
		return result.clampToType(resultType);
	}

	// -------------------------------------------------------------------------
	// evalTypeCast / evalTypeConv
	// -------------------------------------------------------------------------

	@Override
	public JavaFlaggedInterval evalTypeCast(
			BinaryExpression cast,
			JavaFlaggedInterval left,
			JavaFlaggedInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isBottom())
			return bottom();
		JavaTypeKind targetKind = JavaTypeKind.fromType(cast.getStaticType());
		if (targetKind == JavaTypeKind.UNKNOWN)
			return left.withType(targetKind);
		// For integer-to-integer casts: modular restriction
		// For integer-to-float casts: integrality = INTEGRAL, nan = NOT_NAN
		JavaFlaggedInterval clamped = left.clampToType(targetKind);
		if (targetKind.isFloat()) {
			return new JavaFlaggedInterval(
					clamped.getOver(),
					clamped.getUnder(),
					targetKind,
					clamped.getProvenance(),
					IntegralityFlag.INTEGRAL,
					NaNFlag.DEFINITELY_NOT_NAN,
					InfinityFlag.DEFINITELY_FINITE,
					ParityFlag.BOTTOM);
		}
		return clamped;
	}

	@Override
	public JavaFlaggedInterval evalTypeConv(
			BinaryExpression conv,
			JavaFlaggedInterval left,
			JavaFlaggedInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return evalTypeCast(conv, left, right, pp, oracle);
	}

	// -------------------------------------------------------------------------
	// satisfiesBinaryExpression
	// -------------------------------------------------------------------------

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			JavaFlaggedInterval left,
			JavaFlaggedInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;
		if (left.isBottom() || right.isBottom())
			return Satisfiability.BOTTOM;

		IntInterval leftOver = left.getOver();
		IntInterval rightOver = right.getOver();

		BinaryOperator op = expression.getOperator();

		if (op == ComparisonEq.INSTANCE) {
			// Check parity shortcut
			if (left.getParity() != ParityFlag.UNKNOWN_PARITY && !left.getParity().isBottom()
					&& right.isSingleton()) {
				try {
					long rv = rightOver.getLow().toLong();
					ParityFlag rightParity = (rv % 2 == 0) ? ParityFlag.EVEN : ParityFlag.ODD;
					if (left.getParity() != rightParity)
						return Satisfiability.NOT_SATISFIED;
				} catch (MathNumberConversionException e) {
					// cannot determine
				}
			}
			// Standard interval check
			try {
				IntInterval glb = leftOver.glb(rightOver);
				if (glb.isBottom())
					return Satisfiability.NOT_SATISFIED;
				if (leftOver.isSingleton() && leftOver.equals(rightOver))
					return Satisfiability.SATISFIED;
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			return Satisfiability.UNKNOWN;
		}

		if (op == ComparisonNe.INSTANCE) {
			try {
				IntInterval glb = leftOver.glb(rightOver);
				if (glb.isBottom())
					return Satisfiability.SATISFIED;
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			return Satisfiability.UNKNOWN;
		}

		if (op == ComparisonGe.INSTANCE)
			return satisfiesBinaryExpression(expression.withOperator(ComparisonLe.INSTANCE), right, left, pp, oracle);
		if (op == ComparisonGt.INSTANCE)
			return satisfiesBinaryExpression(expression.withOperator(ComparisonLt.INSTANCE), right, left, pp, oracle);

		if (op == ComparisonLe.INSTANCE) {
			try {
				IntInterval glb = leftOver.glb(rightOver);
				if (glb.isBottom())
					return Satisfiability.fromBoolean(leftOver.getHigh().compareTo(rightOver.getLow()) <= 0);
				if (glb.isSingleton() && leftOver.getHigh().compareTo(rightOver.getLow()) == 0)
					return Satisfiability.SATISFIED;
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			return Satisfiability.UNKNOWN;
		}

		if (op == ComparisonLt.INSTANCE) {
			// Infinity flag shortcut
			if (left.getInfinity() == InfinityFlag.NEG_INF && !rightOver.isBottom())
				return Satisfiability.SATISFIED; // -∞ < anything finite
			if (left.getInfinity() == InfinityFlag.POS_INF)
				return Satisfiability.NOT_SATISFIED; // +∞ < x → false
			try {
				IntInterval glb = leftOver.glb(rightOver);
				if (glb.isBottom())
					return Satisfiability.fromBoolean(leftOver.getHigh().compareTo(rightOver.getLow()) < 0);
				if (glb.isSingleton() && rightOver.getHigh().compareTo(leftOver.getLow()) == 0)
					return Satisfiability.NOT_SATISFIED;
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			return Satisfiability.UNKNOWN;
		}

		return Satisfiability.UNKNOWN;
	}

	// -------------------------------------------------------------------------
	// assume
	// -------------------------------------------------------------------------

	@Override
	public ValueEnvironment<JavaFlaggedInterval> assume(
			ValueEnvironment<JavaFlaggedInterval> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;
		ValueExpression e = expression.removeNegations();
		if (e instanceof BinaryExpression be) {
			ValueExpression left = (ValueExpression) be.getLeft();
			ValueExpression right = (ValueExpression) be.getRight();
			if (left instanceof Identifier id && (!id.canBeAssigned() || !canProcess(id, src, oracle)))
				return environment;
			else if (right instanceof Identifier id && (!id.canBeAssigned() || !canProcess(id, src, oracle)))
				return environment;
		}
		return BaseNonRelationalValueDomain.super.assume(environment, expression, src, dest, oracle);
	}

	// -------------------------------------------------------------------------
	// Existential satisfiability (under-approximation)
	// -------------------------------------------------------------------------

	/**
	 * Returns {@code true} if there exists at least one concrete value in the
	 * under-approximation of the left operand that satisfies
	 * {@code left == rightConstant}.
	 *
	 * @param env   the current value environment
	 * @param left  the identifier whose value we query
	 * @param right the target constant value (should be a singleton interval)
	 * @param pp    the program point
	 *
	 * @return {@code true} if the equality is existentially satisfiable
	 *
	 * @throws SemanticException if evaluation fails
	 */
	public boolean existentiallySatisfiesEq(
			ValueEnvironment<JavaFlaggedInterval> env,
			Identifier left,
			JavaFlaggedInterval right,
			ProgramPoint pp)
			throws SemanticException {
		JavaFlaggedInterval leftAbstract = env.getState(left);
		if (leftAbstract == null || leftAbstract.isBottom())
			return false;
		if (!right.isSingleton())
			return false;

		IntInterval rightOver = right.getOver();

		// Under-approximation check
		if (leftAbstract.getUnder() != null) {
			try {
				IntInterval glb = leftAbstract.getUnder().glb(rightOver);
				if (!glb.isBottom())
					return true;
			} catch (SemanticException e) {
				// fall through
			}
		}

		// INPUT provenance: over-approx is exact, so containment implies
		// reachability
		if (leftAbstract.getProvenance() == ProvenanceFlag.INPUT) {
			try {
				IntInterval glb = leftAbstract.getOver().glb(rightOver);
				if (!glb.isBottom())
					return true;
			} catch (SemanticException e) {
				// fall through
			}
		}

		return false;
	}

	// -------------------------------------------------------------------------
	// Helpers: interval arithmetic delegation
	// -------------------------------------------------------------------------

	/**
	 * Evaluates the result interval for a binary arithmetic/comparison operator
	 * using LiSA's standard interval rules. Returns {@code null} when the
	 * operator is not handled here (caller should return {@code top}).
	 *
	 * @param op    the operator
	 * @param left  the left operand interval
	 * @param right the right operand interval
	 *
	 * @return the result interval or {@code null}
	 */
	private IntInterval evalIntervalBinary(
			BinaryOperator op,
			IntInterval left,
			IntInterval right) {
		// Arithmetic via MathNumber to mirror LiSA's Interval logic
		if (left.isTop() || right.isTop())
			return IntInterval.TOP;
		if (left.isBottom() || right.isBottom())
			return IntInterval.BOTTOM;

		String name = op.getClass().getSimpleName();
		switch (name) {
		case "NumericNonOverflowingAdd":
			return new IntInterval(left.getLow().add(right.getLow()), left.getHigh().add(right.getHigh()));
		case "NumericNonOverflowingSub":
			return new IntInterval(left.getLow().subtract(right.getHigh()), left.getHigh().subtract(right.getLow()));
		case "NumericNonOverflowingMul": {
			MathNumber ll = left.getLow().multiply(right.getLow());
			MathNumber lh = left.getLow().multiply(right.getHigh());
			MathNumber hl = left.getHigh().multiply(right.getLow());
			MathNumber hh = left.getHigh().multiply(right.getHigh());
			MathNumber lo = ll.min(lh).min(hl).min(hh);
			MathNumber hi = ll.max(lh).max(hl).max(hh);
			return new IntInterval(lo, hi);
		}
		case "NumericNonOverflowingDiv":
			if (right.is(0))
				return IntInterval.BOTTOM;
			if (left.is(0))
				return IntInterval.ZERO;
			return left.div(right, false, false);
		case "NumericNonOverflowingMod":
			return IntInterval.TOP;
		case "BitwiseAnd":
		case "BitwiseOr":
		case "BitwiseXor":
			return IntInterval.TOP;
		case "BitwiseShiftLeft":
		case "BitwiseShiftRight":
		case "BitwiseUnsignedShiftRight":
			return IntInterval.TOP;
		default:
			return null;
		}
	}

	/**
	 * Builds a result element for compare operators that always return
	 * {@code -1}, {@code 0}, or {@code 1}.
	 *
	 * @param lo    low bound
	 * @param hi    high bound
	 * @param left  the left operand (for flag merging)
	 * @param right the right operand (for flag merging)
	 *
	 * @return the result interval
	 */
	private JavaFlaggedInterval intervalResult(
			long lo,
			long hi,
			JavaFlaggedInterval left,
			JavaFlaggedInterval right) {
		IntInterval over = new IntInterval(new MathNumber(lo), new MathNumber(hi));
		ParityFlag par = lo == hi && lo == 0 ? ParityFlag.EVEN : ParityFlag.UNKNOWN_PARITY;
		return new JavaFlaggedInterval(over, null, JavaTypeKind.INT,
				ProvenanceFlag.ANALYSIS, IntegralityFlag.INTEGRAL,
				NaNFlag.DEFINITELY_NOT_NAN, InfinityFlag.DEFINITELY_FINITE, par);
	}

	/**
	 * Merges flag information from two operands to produce a result element
	 * with the given over-approximation.
	 *
	 * @param left        the left operand
	 * @param right       the right operand
	 * @param resultOver  the result over-approximation
	 * @param resultUnder the result under-approximation (may be {@code null})
	 *
	 * @return the merged result element
	 */
	private JavaFlaggedInterval mergeFlags(
			JavaFlaggedInterval left,
			JavaFlaggedInterval right,
			IntInterval resultOver,
			IntInterval resultUnder) {
		JavaTypeKind type = left.getType().lub(right.getType());
		ProvenanceFlag prov = left.getProvenance().lub(right.getProvenance());
		IntegralityFlag integ = left.getIntegrality().lub(right.getIntegrality());
		NaNFlag nan = left.getNan().lub(right.getNan());
		InfinityFlag inf = left.getInfinity().lub(right.getInfinity());
		ParityFlag par = left.getParity().lub(right.getParity());
		return new JavaFlaggedInterval(resultOver, resultUnder, type, prov, integ, nan, inf, par);
	}

	/**
	 * Builds a result element from the given interval, replacing all flags
	 * according to the specified overrides but preserving the original
	 * under-approximation when no clamping occurred.
	 *
	 * @param arg   the source element
	 * @param over  the new over-approximation
	 * @param nan   the NaN flag for the result
	 * @param inf   the infinity flag for the result
	 * @param integ the integrality flag for the result
	 *
	 * @return the result element
	 */
	private JavaFlaggedInterval withOver(
			JavaFlaggedInterval arg,
			IntInterval over,
			NaNFlag nan,
			InfinityFlag inf,
			IntegralityFlag integ) {
		return new JavaFlaggedInterval(over, null, arg.getType(),
				ProvenanceFlag.ANALYSIS, integ, nan, inf, ParityFlag.BOTTOM);
	}

	// -------------------------------------------------------------------------
	// Trigonometric / transcendental interval helpers
	// -------------------------------------------------------------------------

	/**
	 * Evaluates a trigonometric function over an interval.
	 *
	 * @param arg      the argument interval element
	 * @param function the trigonometric function
	 * @param period   the period of the function (used to check if the result
	 *                     spans the entire range)
	 *
	 * @return the result element
	 */
	private JavaFlaggedInterval trigonometric(
			JavaFlaggedInterval arg,
			Function<Double, Double> function,
			double period) {
		IntInterval over = arg.getOver();
		if (over.isBottom())
			return arg;

		IntInterval resultOver;
		if (over.lowIsMinusInfinity() || over.highIsPlusInfinity()) {
			resultOver = new IntInterval(-1, 1);
		} else {
			double a, b;
			try {
				a = over.getLow().toDouble();
				b = over.getHigh().toDouble();
			} catch (MathNumberConversionException e) {
				return top();
			}
			if (b - a >= period) {
				resultOver = new IntInterval(-1, 1);
			} else {
				double pi = Math.PI;
				int kStart = (int) Math.ceil(a / pi);
				int kEnd = (int) Math.floor(b / pi);
				double fa = function.apply(a);
				double fb = function.apply(b);
				double min = Math.min(fa, fb);
				double max = Math.max(fa, fb);
				for (int k = kStart; k <= kEnd; ++k) {
					double x = function.apply(k * pi);
					min = Math.min(min, x);
					max = Math.max(max, x);
				}
				resultOver = new IntInterval((int) Math.floor(min), (int) Math.ceil(max));
			}
		}
		return new JavaFlaggedInterval(resultOver, null, arg.getType(),
				ProvenanceFlag.ANALYSIS, IntegralityFlag.NON_INTEGRAL,
				NaNFlag.DEFINITELY_NOT_NAN, InfinityFlag.DEFINITELY_FINITE, ParityFlag.BOTTOM);
	}

	private IntInterval evalAsin(
			IntInterval i,
			Double l,
			Double h) {
		if (i.lowIsMinusInfinity() || (l != null && l <= -1)) {
			if (i.highIsPlusInfinity() || (h != null && h >= 1))
				return new IntInterval(new MathNumber(Math.asin(-1)), new MathNumber(Math.asin(1)));
			else if (h != null)
				return new IntInterval(new MathNumber(Math.asin(-1)), new MathNumber(Math.asin(h)));
		} else if (i.highIsPlusInfinity() || (h != null && h >= 1)) {
			if (l != null)
				return new IntInterval(new MathNumber(Math.asin(l)), new MathNumber(Math.asin(1)));
		} else if (l != null && h != null)
			return new IntInterval(new MathNumber(Math.asin(l)), new MathNumber(Math.asin(h)));
		return IntInterval.TOP;
	}

	private IntInterval evalAcos(
			IntInterval i,
			Double l,
			Double h) {
		if (i.lowIsMinusInfinity() || (l != null && l <= -1)) {
			if (i.highIsPlusInfinity() || (h != null && h >= 1))
				return new IntInterval(new MathNumber(Math.acos(1)), new MathNumber(Math.acos(-1)));
			else if (h != null)
				return new IntInterval(new MathNumber(Math.acos(h)), new MathNumber(Math.acos(-1)));
		} else if (i.highIsPlusInfinity() || (h != null && h >= 1)) {
			if (l != null)
				return new IntInterval(new MathNumber(Math.acos(1)), new MathNumber(Math.acos(l)));
		} else if (l != null && h != null)
			return new IntInterval(new MathNumber(Math.acos(h)), new MathNumber(Math.acos(l)));
		return IntInterval.TOP;
	}

	private IntInterval evalAtan(
			IntInterval i,
			Double l,
			Double h) {
		if (i.lowIsMinusInfinity()) {
			if (i.highIsPlusInfinity())
				return IntInterval.TOP;
			else if (h != null)
				return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.atan(h)));
		} else if (i.highIsPlusInfinity()) {
			if (l != null)
				return new IntInterval(new MathNumber(Math.atan(l)), MathNumber.PLUS_INFINITY);
		} else if (l != null && h != null)
			return new IntInterval(new MathNumber(Math.atan(l)), new MathNumber(Math.atan(h)));
		return IntInterval.TOP;
	}

	private IntInterval evalToRadians(
			IntInterval i,
			Double l,
			Double h) {
		if (i.lowIsMinusInfinity()) {
			if (i.highIsPlusInfinity())
				return IntInterval.TOP;
			else if (h != null)
				return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.toRadians(h)));
		} else if (i.highIsPlusInfinity()) {
			if (l != null)
				return new IntInterval(new MathNumber(Math.toRadians(l)), MathNumber.PLUS_INFINITY);
		} else if (l != null && h != null)
			return new IntInterval(new MathNumber(Math.toRadians(l)), new MathNumber(Math.toRadians(h)));
		return IntInterval.TOP;
	}

	private IntInterval evalSqrt(
			IntInterval i) {
		if (i.lowIsMinusInfinity() || i.getLow().compareTo(MathNumber.ZERO) <= 0) {
			if (i.getHigh().compareTo(MathNumber.ZERO) <= 0)
				return IntInterval.BOTTOM;
			if (i.highIsPlusInfinity())
				return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
			try {
				return new IntInterval(MathNumber.ZERO, new MathNumber(Math.sqrt(i.getHigh().toDouble())));
			} catch (MathNumberConversionException e) {
				return IntInterval.TOP;
			}
		}
		if (i.highIsPlusInfinity()) {
			try {
				return new IntInterval(new MathNumber(Math.sqrt(i.getLow().toDouble())), MathNumber.PLUS_INFINITY);
			} catch (MathNumberConversionException e) {
				return IntInterval.TOP;
			}
		}
		try {
			return new IntInterval(new MathNumber(Math.sqrt(i.getLow().toDouble())),
					new MathNumber(Math.sqrt(i.getHigh().toDouble())));
		} catch (MathNumberConversionException e) {
			return IntInterval.TOP;
		}
	}

	private IntInterval evalLog(
			IntInterval i,
			Double l,
			Double h,
			Function<Double, Double> logFn) {
		if (i.lowIsMinusInfinity() || i.getLow().compareTo(MathNumber.ZERO) <= 0) {
			if (i.getHigh().compareTo(MathNumber.ZERO) <= 0)
				return IntInterval.BOTTOM;
			if (i.highIsPlusInfinity())
				return IntInterval.TOP;
			if (h != null)
				return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(logFn.apply(h)));
		} else if (i.highIsPlusInfinity()) {
			if (l != null)
				return new IntInterval(new MathNumber(logFn.apply(l)), MathNumber.PLUS_INFINITY);
		} else if (l != null && h != null)
			return new IntInterval(new MathNumber(logFn.apply(l)), new MathNumber(logFn.apply(h)));
		return IntInterval.TOP;
	}

	private IntInterval evalExp(
			IntInterval i,
			Double l,
			Double h) {
		if (i.lowIsMinusInfinity()) {
			if (i.highIsPlusInfinity())
				return IntInterval.TOP;
			if (h != null)
				return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.exp(h)));
		} else if (i.highIsPlusInfinity()) {
			if (l != null)
				return new IntInterval(new MathNumber(Math.exp(l)), MathNumber.PLUS_INFINITY);
		} else if (l != null && h != null)
			return new IntInterval(new MathNumber(Math.exp(l)), new MathNumber(Math.exp(h)));
		return IntInterval.TOP;
	}

	private IntInterval evalAbs(
			IntInterval i) {
		if (i.getLow().compareTo(MathNumber.ZERO) >= 0)
			return i;
		if (i.getHigh().compareTo(MathNumber.ZERO) <= 0)
			return new IntInterval(i.getHigh().multiply(MathNumber.MINUS_ONE),
					i.getLow().multiply(MathNumber.MINUS_ONE));
		if (i.getHigh().compareTo(i.getLow().multiply(MathNumber.MINUS_ONE)) >= 0)
			return new IntInterval(MathNumber.ZERO, i.getHigh());
		return new IntInterval(MathNumber.ZERO, i.getLow().multiply(MathNumber.MINUS_ONE));
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------
	// assumeBinaryExpression — interval narrowing
	// -------------------------------------------------------------------------

	/**
	 * Narrows the abstract value of the identifier in the binary expression
	 * using the comparison constraint. Delegates the narrowing arithmetic to
	 * {@link Interval#updateValue}, which implements standard interval
	 * refinement (e.g. {@code assume(x >= c)} → {@code x = x ∩ [c, +∞]}).
	 * <p>
	 * The under-approximation component is also narrowed by intersecting it
	 * with the new over-approximation interval. All flags are preserved from
	 * the current abstract value.
	 */
	@Override
	public ValueEnvironment<JavaFlaggedInterval> assumeBinaryExpression(
			ValueEnvironment<JavaFlaggedInterval> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator op = expression.getOperator();
		if (op != ComparisonEq.INSTANCE && op != ComparisonGe.INSTANCE
				&& op != ComparisonGt.INSTANCE && op != ComparisonLe.INSTANCE
				&& op != ComparisonLt.INSTANCE && op != ComparisonNe.INSTANCE)
			return environment;

		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();

		Identifier id;
		IntInterval eval;
		boolean rightIsExpr;

		if (left instanceof Identifier leftId) {
			id = leftId;
			eval = evalToInterval(environment, right, src, oracle);
			rightIsExpr = true;
		} else if (right instanceof Identifier rightId) {
			id = rightId;
			eval = evalToInterval(environment, left, src, oracle);
			rightIsExpr = false;
		} else {
			return environment;
		}

		if (eval == null || eval.isBottom())
			return environment;

		JavaFlaggedInterval current = environment.getState(id);
		if (current == null || current.isBottom())
			return environment;

		IntInterval currentOver = current.getOver();

		// ComparisonNe: for integral types, narrow at the interval bound that
		// exactly equals the excluded singleton value (discrete step of ±1).
		if (op == ComparisonNe.INSTANCE) {
			if (current.getIntegrality() == IntegralityFlag.INTEGRAL && eval.isSingleton()) {
				MathNumber c = eval.getLow();
				MathNumber lo = currentOver.getLow();
				MathNumber hi = currentOver.getHigh();
				IntInterval narrowedOver = null;
				if (!lo.isMinusInfinity() && c.compareTo(lo) == 0)
					narrowedOver = new IntInterval(lo.add(MathNumber.ONE), hi);
				else if (!hi.isPlusInfinity() && c.compareTo(hi) == 0)
					narrowedOver = new IntInterval(lo, hi.subtract(MathNumber.ONE));
				if (narrowedOver != null) {
					if (narrowedOver.isBottom())
						return environment.bottom();
					return putNarrowed(environment, id, current, narrowedOver);
				}
			}
			return environment;
		}

		IntInterval narrowedOver = Interval.updateValue(op, rightIsExpr, currentOver, eval);

		if (narrowedOver == null)
			return environment;
		if (narrowedOver.isBottom())
			return environment.bottom();

		return putNarrowed(environment, id, current, narrowedOver);
	}

	/**
	 * Narrows the abstract state by replacing the identifier's abstract value
	 * with a new {@link JavaFlaggedInterval} whose over-approximation is
	 * {@code narrowedOver}. The under-approximation is tightened to the
	 * intersection of the old under with the new over.
	 */
	private ValueEnvironment<JavaFlaggedInterval> putNarrowed(
			ValueEnvironment<JavaFlaggedInterval> environment,
			Identifier id,
			JavaFlaggedInterval current,
			IntInterval narrowedOver)
			throws SemanticException {
		IntInterval newUnder = current.getUnder();
		if (newUnder != null) {
			IntInterval glb = newUnder.glb(narrowedOver);
			newUnder = glb.isBottom() ? null : glb;
		}
		JavaFlaggedInterval narrowed = new JavaFlaggedInterval(
				narrowedOver, newUnder,
				current.getType(),
				current.getProvenance(),
				current.getIntegrality(),
				current.getNan(),
				current.getInfinity(),
				current.getParity());
		return environment.putState(id, narrowed);
	}

	/**
	 * Evaluates {@code expr} in the given environment and returns the
	 * over-approximation interval, or {@code null} if evaluation fails.
	 */
	private IntInterval evalToInterval(
			ValueEnvironment<JavaFlaggedInterval> env,
			ValueExpression expr,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		JavaFlaggedInterval result = eval(env, expr, pp, oracle);
		return (result == null || result.isBottom()) ? null : result.getOver();
	}

	// Type resolution helper
	// -------------------------------------------------------------------------

	/**
	 * Resolves the {@link JavaTypeKind} of the given expression by consulting
	 * the oracle's runtime types first, then falling back to the static type.
	 *
	 * @param expr   the expression
	 * @param pp     the program point
	 * @param oracle the semantic oracle
	 *
	 * @return the resolved type kind
	 */
	private JavaTypeKind typeFromOracle(
			SymbolicExpression expr,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (oracle != null) {
			try {
				Set<Type> rts = oracle.getRuntimeTypesOf(expr, pp);
				if (rts != null && !rts.isEmpty())
					return JavaTypeKind.fromTypes(rts);
			} catch (SemanticException e) {
				// fall through
			}
		}
		return JavaTypeKind.fromType(expr.getStaticType());
	}
}

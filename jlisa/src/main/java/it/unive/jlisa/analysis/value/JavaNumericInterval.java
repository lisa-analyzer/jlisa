package it.unive.jlisa.analysis.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;

public class JavaNumericInterval extends Interval {

	@Override
	public IntInterval evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Number) {
			return fromConstant(constant);
		}
		// If the constant is not a number, return BOTTOM.
		// TOP represents any possible number, but since the constant is not
		// numeric, BOTTOM is more appropriate.
		return IntInterval.BOTTOM;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			IntInterval left,
			IntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		if (operator == ComparisonEq.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.NOT_SATISFIED;
			else if (left.isSingleton() && left.equals(right))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonGe.INSTANCE)
			return satisfiesBinaryExpression(expression.withOperator(ComparisonLe.INSTANCE), right, left, pp, oracle);
		else if (operator == ComparisonGt.INSTANCE)
			return satisfiesBinaryExpression(expression.withOperator(ComparisonLt.INSTANCE), right, left, pp, oracle);
		else if (operator == ComparisonLe.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.getHigh().compareTo(right.getLow()) <= 0);
			// we might have a singleton as glb if the two intervals share a
			// bound
			if (glb.isSingleton() && left.getHigh().compareTo(right.getLow()) == 0)
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonLt.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.getHigh().compareTo(right.getLow()) < 0);
			// TODO: verify the correctness of the following condition.
			// Example: left = [0,1], right = [0,0]
			// In this case, the GLB (greatest lower bound) is [0,0].
			// Here, left is NOT less than right, since left.getLow() == 0 and
			// 0 is also the only possible value in right. Therefore, the
			// comparison
			// should not be satisfied.
			if (glb.isSingleton() && right.getHigh().compareTo(left.getLow()) == 0)
				return Satisfiability.NOT_SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonNe.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			if (glb.isBottom())
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}
		return Satisfiability.UNKNOWN;
	}

	public IntInterval fromConstant(
			Constant constant) {
		double value = ((Number) constant.getValue()).doubleValue();
		if (Double.isNaN(value)) {
			return IntInterval.BOTTOM; // not a number
		}
		if (value == Double.POSITIVE_INFINITY) {
			// return new IntInterval(MathNumber.PLUS_INFINITY,
			// MathNumber.PLUS_INFINITY);
			return IntInterval.BOTTOM;
		}
		if (value == Double.NEGATIVE_INFINITY) {
			// return new IntInterval(MathNumber.MINUS_INFINITY,
			// MathNumber.MINUS_INFINITY);
			return IntInterval.BOTTOM;
		}
		return new IntInterval(new MathNumber(value), new MathNumber(value));
	}
}

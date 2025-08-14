package it.unive.jlisa.analysis;

import java.util.Set;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.program.operator.JavaCharacterIsDigit;
import it.unive.jlisa.program.operator.JavaCharacterIsLetter;
import it.unive.jlisa.program.operator.JavaMathAbs;
import it.unive.jlisa.program.operator.JavaMathAcos;
import it.unive.jlisa.program.operator.JavaMathAsin;
import it.unive.jlisa.program.operator.JavaMathAtan;
import it.unive.jlisa.program.operator.JavaMathAtan2;
import it.unive.jlisa.program.operator.JavaMathCos;
import it.unive.jlisa.program.operator.JavaMathExp;
import it.unive.jlisa.program.operator.JavaMathFloor;
import it.unive.jlisa.program.operator.JavaMathLog;
import it.unive.jlisa.program.operator.JavaMathLog10;
import it.unive.jlisa.program.operator.JavaMathPow;
import it.unive.jlisa.program.operator.JavaMathRound;
import it.unive.jlisa.program.operator.JavaMathSin;
import it.unive.jlisa.program.operator.JavaMathSqrt;
import it.unive.jlisa.program.operator.JavaMathTan;
import it.unive.jlisa.program.operator.JavaMathToRadians;
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringConcatOperator;
import it.unive.jlisa.program.operator.JavaStringContainsOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.operator.JavaStringToLowerCaseOperator;
import it.unive.jlisa.program.operator.JavaStringToUpperCaseOperator;
import it.unive.jlisa.program.operator.JavaStringTrimOperator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;

public class ConstantPropagation implements BaseNonRelationalValueDomain<ConstantValue> {

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression instanceof PushInv)
			// the type approximation of a pushinv is bottom, so the below check
			// will always fail regardless of the kind of value we are tracking
			return expression.getStaticType().isValueType();

		Set<Type> rts = null;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp);
		} catch (SemanticException e) {
			return false;
		}

		if (rts == null || rts.isEmpty())
			// if we have no runtime types, either the type domain has no type
			// information for the given expression (thus it can be anything,
			// also something that we can track) or the computation returned
			// bottom (and the whole state is likely going to go to bottom
			// anyway).
			return true;

		return rts.stream().anyMatch(Type::isValueType) || rts.stream().anyMatch( t -> t.toString().equals("String"));
	}

	@Override
	public ConstantValue evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
					throws SemanticException {
		// FIXME: remove constant
		return new ConstantValue(new Constant(NullType.INSTANCE, null, pp.getLocation()));
	}

	@Override
	public ConstantValue evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
					throws SemanticException {
		return new ConstantValue(constant.getValue());
	}


	@Override
	public ConstantValue evalUnaryExpression(UnaryExpression expression, ConstantValue arg, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		// if arg is top, top is returned
		if (arg.isTop())
			return top();

		UnaryOperator operator = expression.getOperator();
		// char
		if (operator instanceof JavaCharacterIsLetter)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Character.isLetter(v));

		if (operator instanceof JavaCharacterIsDigit)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Character.isDigit(v));

		// numeric
		if (operator instanceof NumericNegation)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(-v);
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(-v);
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(-v);
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(-v);

		if (operator instanceof JavaMathSin)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.sin(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.sin(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.sin(v));

		if (operator instanceof JavaMathCos)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.cos(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.cos(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.cos(v));

		if (operator instanceof JavaMathSqrt)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.sqrt(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.sqrt(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.sqrt(v));

		if (operator instanceof JavaMathTan)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.tan(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.tan(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.tan(v));

		if (operator instanceof JavaMathAtan)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.atan(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.atan(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.atan(v));

		if (operator instanceof JavaMathLog)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.log(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.log(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.log(v));

		if (operator instanceof JavaMathLog10)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.log10(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.log10(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.log10(v));

		if (operator instanceof JavaMathAsin)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.asin(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.asin(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.asin(v));

		if (operator instanceof JavaMathExp)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.exp(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.exp(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.exp(v));

		if (operator instanceof JavaMathAcos)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.acos(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.acos(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.acos(v));

		if (operator instanceof JavaMathFloor)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.floor(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.floor(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.floor(v));

		if (operator instanceof JavaMathRound)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.round(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.round(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.round(v));

		if (operator instanceof JavaMathToRadians)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.toRadians(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.toRadians(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.toRadians(v));

		if (operator instanceof JavaMathAbs)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.abs(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.abs(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.abs(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.abs(v));

		// strings
		if (operator instanceof JavaStringLengthOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.length());

		if (operator instanceof JavaStringToLowerCaseOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.toLowerCase());

		if (operator instanceof JavaStringToUpperCaseOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.toUpperCase());

		if (operator instanceof JavaStringTrimOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.trim());

		return top();
	}

	@Override
	public ConstantValue evalBinaryExpression(
			BinaryExpression expression,
			ConstantValue left,
			ConstantValue right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		// if left or right is top, top is returned
		if (left.isTop() || right.isTop())
			return top();

		BinaryOperator operator = expression.getOperator();
		if (operator instanceof AdditionOperator) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof String || rVal instanceof String) {
			} else if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() + ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() + ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() + ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() + ((Number) rVal).intValue());
			}
		}

		if (operator instanceof SubtractionOperator) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() - ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() - ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() - ((Number) rVal).longValue());
			} else {
				return new ConstantValue(((Number) lVal).intValue() - ((Number) rVal).intValue());
			}
		}

		if (operator instanceof MultiplicationOperator) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() * ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() * ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() * ((Number) rVal).longValue());
			} else {
				return new ConstantValue(((Number) lVal).intValue() * ((Number) rVal).intValue());
			}
		}

		if (operator instanceof DivisionOperator) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() / ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() / ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() / ((Number) rVal).longValue());
			} else {
				return new ConstantValue(((Number) lVal).intValue() / ((Number) rVal).intValue());
			}
		}

		if (operator instanceof JavaMathPow) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double || lVal instanceof Integer || rVal instanceof Integer || lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(Math.pow(((Number) lVal).doubleValue(), ((Number) rVal).doubleValue()));
			}
		}

		if (operator instanceof JavaMathAtan2) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double || lVal instanceof Integer || rVal instanceof Integer || lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(Math.atan2(((Number) lVal).doubleValue(), ((Number) rVal).doubleValue()));
			}
		}

		// strings
		if (operator instanceof JavaStringConcatOperator)
			return new ConstantValue(((String) left.getValue()) + ((String) right.getValue()));

		if (operator instanceof JavaStringContainsOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.contains(rv));			
		}

		if (operator instanceof JavaStringEqualsOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.equals(rv));			
		}

		if (operator instanceof JavaStringCharAtOperator) {
			String lv = ((String) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.charAt(rv));			
		}

		return top();
	}

	@Override
	public Satisfiability satisfiesAbstractValue(ConstantValue value, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		// this method should not be never called
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability satisfiesNullConstant(ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		// this method should not be never called
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(UnaryExpression expression, ConstantValue arg, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		if (arg.isTop())
			return Satisfiability.UNKNOWN;

		UnaryOperator operator = expression.getOperator();
		if (operator instanceof JavaCharacterIsLetter)
			if (arg.getValue() instanceof Integer v)
				return Character.isLetter(v) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		if (operator instanceof JavaCharacterIsDigit)
			if (arg.getValue() instanceof Integer v)
				return Character.isDigit(v) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		return BaseNonRelationalValueDomain.super.satisfiesUnaryExpression(expression, arg, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryExpression expression, ConstantValue left,
			ConstantValue right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (operator instanceof JavaStringContainsOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.contains(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;			
		}

		if (operator instanceof JavaStringEqualsOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.equals(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;			
		}

		return BaseNonRelationalValueDomain.super.satisfiesBinaryExpression(expression, left, right, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(TernaryExpression expression, ConstantValue left,
			ConstantValue middle, ConstantValue right, ProgramPoint pp, SemanticOracle oracle)
					throws SemanticException {
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesTernaryExpression(expression, left, middle, right, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		// this method should not be never called
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ConstantValue top() {
		return ConstantValue.TOP;
	}

	@Override
	public ConstantValue bottom() {
		return ConstantValue.BOTTOM;
	}
}

package it.unive.jlisa.analysis;

import java.util.Set;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.program.operator.JavaCharacterEqualsOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsDigitOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsLetterOperator;
import it.unive.jlisa.program.operator.JavaDoubleToRawLongBitsOperator;
import it.unive.jlisa.program.operator.JavaMathAbsOperator;
import it.unive.jlisa.program.operator.JavaMathAcosOperator;
import it.unive.jlisa.program.operator.JavaMathAsinOperator;
import it.unive.jlisa.program.operator.JavaMathAtan2Operator;
import it.unive.jlisa.program.operator.JavaMathAtanOperator;
import it.unive.jlisa.program.operator.JavaMathCosOperator;
import it.unive.jlisa.program.operator.JavaMathExpOperator;
import it.unive.jlisa.program.operator.JavaMathFloorOperator;
import it.unive.jlisa.program.operator.JavaMathLog10Operator;
import it.unive.jlisa.program.operator.JavaMathLogOperator;
import it.unive.jlisa.program.operator.JavaMathPowOperator;
import it.unive.jlisa.program.operator.JavaMathRoundOperator;
import it.unive.jlisa.program.operator.JavaMathSinOperator;
import it.unive.jlisa.program.operator.JavaMathSqrtOperator;
import it.unive.jlisa.program.operator.JavaMathTanOperator;
import it.unive.jlisa.program.operator.JavaMathToRadiansOperator;
import it.unive.jlisa.program.operator.JavaStringAppendCharOperator;
import it.unive.jlisa.program.operator.JavaStringAppendStringOperator;
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringConcatOperator;
import it.unive.jlisa.program.operator.JavaStringContainsOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.operator.JavaStringInsertCharOperator;
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
import it.unive.lisa.symbolic.value.operator.binary.BitwiseAnd;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseOr;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftLeft;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseUnsignedShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseXor;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
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
		if (operator instanceof JavaCharacterIsLetterOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Character.isLetter(v));

		if (operator instanceof JavaCharacterIsDigitOperator)
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

		if (operator instanceof JavaMathSinOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.sin(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.sin(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.sin(v));

		if (operator instanceof JavaMathCosOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.cos(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.cos(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.cos(v));

		if (operator instanceof JavaMathSqrtOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.sqrt(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.sqrt(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.sqrt(v));

		if (operator instanceof JavaMathTanOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.tan(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.tan(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.tan(v));

		if (operator instanceof JavaMathAtanOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.atan(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.atan(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.atan(v));

		if (operator instanceof JavaMathLogOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.log(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.log(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.log(v));

		if (operator instanceof JavaMathLog10Operator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.log10(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.log10(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.log10(v));

		if (operator instanceof JavaMathAsinOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.asin(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.asin(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.asin(v));

		if (operator instanceof JavaMathExpOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.exp(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.exp(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.exp(v));

		if (operator instanceof JavaMathAcosOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.acos(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.acos(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.acos(v));

		if (operator instanceof JavaMathFloorOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.floor(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.floor(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.floor(v));

		if (operator instanceof JavaMathRoundOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.round(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.round(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.round(v));

		if (operator instanceof JavaMathToRadiansOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.toRadians(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.toRadians(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.toRadians(v));

		if (operator instanceof JavaMathAbsOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.abs(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.abs(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.abs(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.abs(v));

		if (operator instanceof JavaDoubleToRawLongBitsOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Double.doubleToRawLongBits(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Double.doubleToRawLongBits(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Double.doubleToRawLongBits(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Double.doubleToRawLongBits(v));

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

			if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() + ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() + ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() + ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() + ((Number) rVal).intValue());
			}
		}

		if (operator instanceof BitwiseOr) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() | ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() | ((Number) rVal).intValue());
			}
		}
		
		if (operator instanceof BitwiseShiftRight) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();
			
			if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() >> ((Number) rVal).intValue());
			}
		}
		
		if (operator instanceof BitwiseUnsignedShiftRight) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();
			
			if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() >>> ((Number) rVal).intValue());
			}
		}
		
		if (operator instanceof BitwiseShiftLeft) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();
			
			if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() << ((Number) rVal).intValue());
			}
		}
		
		if (operator instanceof BitwiseXor) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() ^ ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() ^ ((Number) rVal).intValue());
			}
		}
		
		if (operator instanceof BitwiseAnd) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() & ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() & ((Number) rVal).intValue());
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

		if (operator instanceof JavaMathPowOperator) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double || lVal instanceof Integer || rVal instanceof Integer || lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(Math.pow(((Number) lVal).doubleValue(), ((Number) rVal).doubleValue()));
			}
		}

		if (operator instanceof JavaMathAtan2Operator) {
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

		if (operator instanceof JavaStringAppendCharOperator) {
			String lv = ((String) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv + ((char) rv.intValue()));			
		}	

		if (operator instanceof JavaStringAppendStringOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv + rv);			
		}

		// char
		if (operator instanceof JavaCharacterEqualsOperator) {
			Integer lv = ((Integer) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.equals(rv));			
		}

		return top();
	}

	@Override
	public ConstantValue evalTernaryExpression(TernaryExpression expression, ConstantValue left, ConstantValue middle,
			ConstantValue right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		// if left, middle or right is top, top is returned
		if (left.isTop() || middle.isTop() || right.isTop())
			return top();

		TernaryOperator operator = expression.getOperator();
		if (operator instanceof JavaStringInsertCharOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), (char)rv.intValue()).toString());
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
		if (operator instanceof JavaCharacterIsLetterOperator)
			if (arg.getValue() instanceof Integer v)
				return Character.isLetter(v) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		if (operator instanceof JavaCharacterIsDigitOperator)
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

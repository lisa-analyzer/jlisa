package it.unive.jlisa.analysis.value;

import java.util.Set;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.program.operator.JavaCharacterDigitOperator;
import it.unive.jlisa.program.operator.JavaCharacterEqualsOperator;
import it.unive.jlisa.program.operator.JavaCharacterForDigitOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsDefinedOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsDigitOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsJavaIdentifierPartOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsJavaIdentifierStartOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsLetterOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsLetterOrDigitOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsLowerCaseOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsUpperCaseOperator;
import it.unive.jlisa.program.operator.JavaCharacterToLowerCaseOperator;
import it.unive.jlisa.program.operator.JavaCharacterToUpperCaseOperator;
import it.unive.jlisa.program.operator.JavaDoubleLongBitsToDoubleOperator;
import it.unive.jlisa.program.operator.JavaDoubleParseDoubleOperator;
import it.unive.jlisa.program.operator.JavaDoubleToRawLongBitsOperator;
import it.unive.jlisa.program.operator.JavaDoubleToStringOperator;
import it.unive.jlisa.program.operator.JavaLongIntValueOperator;
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
import it.unive.jlisa.program.operator.JavaStringCompareToOperator;
import it.unive.jlisa.program.operator.JavaStringConcatOperator;
import it.unive.jlisa.program.operator.JavaStringContainsOperator;
import it.unive.jlisa.program.operator.JavaStringEndsWithOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsIgnoreCaseOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.operator.JavaStringGetBytesOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfCharFromIndexOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfStringFromIndexOperator;
import it.unive.jlisa.program.operator.JavaStringInsertCharOperator;
import it.unive.jlisa.program.operator.JavaStringLastIndexOfCharFromIndexOperator;
import it.unive.jlisa.program.operator.JavaStringLastIndexOfOperator;
import it.unive.jlisa.program.operator.JavaStringLastIndexOfStringFromIndexOperator;
import it.unive.jlisa.program.operator.JavaStringLastIndexOfStringOperator;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.operator.JavaStringMatchesOperator;
import it.unive.jlisa.program.operator.JavaStringReplaceAllOperator;
import it.unive.jlisa.program.operator.JavaStringReplaceOperator;
import it.unive.jlisa.program.operator.JavaStringStartsWithFromIndexOperator;
import it.unive.jlisa.program.operator.JavaStringStartsWithOperator;
import it.unive.jlisa.program.operator.JavaStringSubstringFromToOperator;
import it.unive.jlisa.program.operator.JavaStringSubstringOperator;
import it.unive.jlisa.program.operator.JavaStringToLowerCaseOperator;
import it.unive.jlisa.program.operator.JavaStringToUpperCaseOperator;
import it.unive.jlisa.program.operator.JavaStringTrimOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfFloatOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfIntOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfLongOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfObjectOperator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseAnd;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseOr;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftLeft;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseUnsignedShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseXor;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
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

		return rts.stream().anyMatch(Type::isValueType) || rts.stream().anyMatch( t -> t.isStringType());
	}

	@Override
	public ConstantValue evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
					throws SemanticException {
		throw new SemanticException("null value is not handled by the constant propagation domain");
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

		if (operator instanceof JavaCharacterIsDefinedOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Character.isDefined(v));

		if (operator instanceof JavaCharacterToLowerCaseOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue( (char) Character.toLowerCase(v));

		if (operator instanceof JavaCharacterToUpperCaseOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue( (char) Character.toUpperCase(v));

		if (operator instanceof JavaCharacterIsJavaIdentifierPartOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Character.isJavaIdentifierPart(v));

		if (operator instanceof JavaCharacterIsJavaIdentifierStartOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Character.isJavaIdentifierStart(v));

		if (operator instanceof JavaCharacterIsLetterOrDigitOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Character.isLetterOrDigit(v));

		if (operator instanceof JavaCharacterIsLowerCaseOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Character.isLowerCase(v));

		if (operator instanceof JavaCharacterIsUpperCaseOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Character.isUpperCase(v));

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

		if (operator instanceof JavaDoubleLongBitsToDoubleOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Double.longBitsToDouble(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Double.longBitsToDouble(v));

		if (operator instanceof JavaDoubleToStringOperator)
			if(arg.getValue() instanceof Double d)
				return new ConstantValue(d.toString());

		if (operator instanceof JavaDoubleParseDoubleOperator)
			if(arg.getValue() instanceof String s)
				return new ConstantValue(Double.parseDouble(s));

		// strings
		if (operator instanceof JavaStringLengthOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.length());

		if (operator instanceof JavaStringToLowerCaseOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.toLowerCase());

		if (operator instanceof JavaStringToUpperCaseOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.toUpperCase());

		if (operator instanceof JavaStringTrimOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.trim());

		if (operator instanceof JavaStringValueOfLongOperator && arg.getValue() instanceof Integer l)
			return new ConstantValue(String.valueOf(l));

		if (operator instanceof JavaStringValueOfBooleanOperator && arg.getValue() instanceof Boolean b)
			return new ConstantValue(String.valueOf(b));

		if (operator instanceof JavaStringValueOfDoubleOperator && arg.getValue() instanceof Double d)
			return new ConstantValue(String.valueOf(d));

		if (operator instanceof JavaStringValueOfFloatOperator && arg.getValue() instanceof Float f)
			return new ConstantValue(String.valueOf(f));

		if (operator instanceof JavaStringValueOfIntOperator && arg.getValue() instanceof Integer i)
			return new ConstantValue(String.valueOf(i));

		if (operator instanceof JavaStringValueOfCharOperator && arg.getValue() instanceof Character c)
			return new ConstantValue(String.valueOf(c));

		if (operator instanceof JavaStringValueOfObjectOperator && arg.getValue() instanceof Object o)
			return new ConstantValue(String.valueOf(o));

		if (operator instanceof JavaStringGetBytesOperator && arg.getValue() instanceof String s)
			return new ConstantValue(s.getBytes());

		if (operator instanceof JavaLongIntValueOperator && arg.getValue() instanceof Long l)
			return new ConstantValue(l.intValue());

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

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() >> ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() >> ((Number) rVal).intValue());
			}
		}

		if (operator instanceof BitwiseUnsignedShiftRight) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() >>> ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() >>> ((Number) rVal).intValue());
			} 
		}

		if (operator instanceof BitwiseShiftLeft) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() << ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
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

		if (operator instanceof ModuloOperator) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() % ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() % ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() % ((Number) rVal).longValue());
			} else {
				return new ConstantValue(((Number) lVal).intValue() % ((Number) rVal).intValue());
			}
		}

		if (operator instanceof ComparisonLt) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() < ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() < ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() < ((Number) rVal).longValue());
			} else {
				return new ConstantValue(((Number) lVal).intValue() < ((Number) rVal).intValue());
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

		if (operator instanceof JavaStringStartsWithOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.startsWith(rv));			
		}

		if (operator instanceof JavaStringEndsWithOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.endsWith(rv));			
		}

		if (operator instanceof JavaStringMatchesOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.matches(rv));			
		}

		if (operator instanceof JavaStringSubstringOperator) {
			String lv = ((String) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.substring(rv));			
		}

		if (operator instanceof JavaStringCompareToOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.compareTo(rv));			
		}

		if (operator instanceof JavaStringIndexOfOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.indexOf(rv));			
		}

		if (operator instanceof JavaStringIndexOfCharOperator) {
			String lv = ((String) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.indexOf(rv));			
		}

		if (operator instanceof JavaStringLastIndexOfOperator) {
			String lv = ((String) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.lastIndexOf(rv));
		}

		if (operator instanceof JavaStringLastIndexOfStringOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.lastIndexOf(rv));
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

		if (operator instanceof JavaStringEqualsIgnoreCaseOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.equalsIgnoreCase(rv));			
		}

		// char
		if (operator instanceof JavaCharacterEqualsOperator) {
			Integer lv = ((Integer) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.equals(rv));
		}

		if (operator instanceof JavaCharacterForDigitOperator) {
			Integer lv = ((Integer) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(Character.forDigit(lv,rv));		
		}

		if (operator instanceof JavaCharacterDigitOperator) {
			Integer lv = ((Integer) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(Character.digit(lv,rv));	
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

		if (operator instanceof JavaStringReplaceAllOperator) {
			String lv = ((String) left.getValue());
			String mv = ((String) middle.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.replaceAll(mv,rv));	
		}

		if (operator instanceof JavaStringReplaceOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.replace( (char) mv.intValue(),(char) rv.intValue()));	
		}

		if (operator instanceof JavaStringIndexOfCharFromIndexOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.indexOf( (char) mv.intValue(), rv));	
		}

		if (operator instanceof JavaStringLastIndexOfCharFromIndexOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.lastIndexOf( (char) mv.intValue(), rv));	
		}

		if (operator instanceof JavaStringLastIndexOfStringFromIndexOperator) {
			String lv = ((String) left.getValue());
			String mv = ((String) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.lastIndexOf(mv, rv));	
		}

		if (operator instanceof JavaStringSubstringFromToOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.substring(mv, rv));	
		}

		if (operator instanceof JavaStringStartsWithFromIndexOperator) {
			String lv = ((String) left.getValue());
			String mv = ((String) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.startsWith(mv, rv));	
		}

		if (operator instanceof JavaStringIndexOfStringFromIndexOperator) {
			String lv = ((String) left.getValue());
			String mv = ((String) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.indexOf(mv, rv));	
		}

		return top();

	}

	@Override
	public ConstantValue evalValueExpression(
			ValueExpression expression,
			ConstantValue[] subExpressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
				
		if(subExpressions.length == 4) {
			
		} else if (subExpressions.length == 5) {
			
		}
		
		return top();
	}
	
	@Override
	public Satisfiability satisfiesAbstractValue(ConstantValue value, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		if (value.getValue() instanceof Boolean)
			return ((Boolean) value.getValue()).booleanValue() ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

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
		
		if (operator instanceof JavaCharacterIsDefinedOperator)
			if (arg.getValue() instanceof Integer v)
				return Character.isDefined(v) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		
		if (operator instanceof JavaCharacterIsLetterOrDigitOperator)
			if (arg.getValue() instanceof Integer v)
				return Character.isLetterOrDigit(v) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		
		if (operator instanceof JavaCharacterIsLowerCaseOperator)
			if (arg.getValue() instanceof Integer v)
				return Character.isLowerCase(v) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		if (operator instanceof JavaCharacterIsUpperCaseOperator)
			if (arg.getValue() instanceof Integer v)
				return Character.isUpperCase(v) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		
		if (operator instanceof JavaCharacterIsJavaIdentifierStartOperator)
			if (arg.getValue() instanceof Integer v)
				return Character.isJavaIdentifierStart(v) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		
		if (operator instanceof JavaCharacterIsJavaIdentifierPartOperator)
			if (arg.getValue() instanceof Integer v)
				return Character.isJavaIdentifierPart(v) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		
		return BaseNonRelationalValueDomain.super.satisfiesUnaryExpression(expression, arg, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryExpression expression, ConstantValue left,
			ConstantValue right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		BinaryOperator operator = expression.getOperator();
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;
		
		// character
			
		if (operator instanceof JavaCharacterEqualsOperator) {
			Integer lv = ((Integer) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return lv.equals(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}
		
		// string
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
		
		if (operator instanceof JavaStringMatchesOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.matches(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;			
		}
		
		if (operator instanceof JavaStringEndsWithOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.endsWith(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;			
		}
		
		if (operator instanceof JavaStringStartsWithOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.startsWith(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;			
		}
		
		if (operator instanceof JavaStringEqualsIgnoreCaseOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.equalsIgnoreCase(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;			
		}

		if (operator instanceof ComparisonEq) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Number || rVal instanceof Number)
				if (lVal instanceof Double || rVal instanceof Double) {
					return ((Number) lVal).doubleValue() == ((Number) rVal).doubleValue() ?  Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;	
				} else if (lVal instanceof Float || rVal instanceof Float) {
					return ((Number) lVal).floatValue() == ((Number) rVal).floatValue() ?  Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;	
				} else if (lVal instanceof Long || rVal instanceof Long) {
					return ((Number) lVal).longValue() == ((Number) rVal).longValue() ?  Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;	
				} else {
					return ((Number) lVal).intValue() == ((Number) rVal).intValue() ?  Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;	
				}
			else if (lVal instanceof Boolean && rVal instanceof Boolean)
				return ((Boolean) lVal).booleanValue() == ((Boolean) rVal).booleanValue() ?  Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;	
			else if (lVal instanceof Character && rVal instanceof Character)
				return ((Character) lVal).charValue() == ((Character) rVal).charValue() ?  Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;	
		}

		if (operator instanceof ComparisonLt) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return ((Number) lVal).doubleValue() < ((Number) rVal).doubleValue() ?  Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;	
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return ((Number) lVal).floatValue() < ((Number) rVal).floatValue() ?  Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;	
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return ((Number) lVal).longValue() < ((Number) rVal).longValue() ?  Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;	
			} else {
				return ((Number) lVal).intValue() < ((Number) rVal).intValue() ?  Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;	
			}	
		}

		if (operator instanceof JavaStringStartsWithOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.startsWith(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;			
		}

		if (operator instanceof JavaStringEndsWithOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.endsWith(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;			
		}

		if (operator instanceof JavaStringMatchesOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.matches(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;			
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
		if (constant.getValue() instanceof Boolean)
			return ((Boolean) constant.getValue()).booleanValue() ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;


		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<ConstantValue> assume(ValueEnvironment<ConstantValue> environment,
			ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle)
					throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, dest, oracle);
		if (sat == Satisfiability.SATISFIED || sat == Satisfiability.UNKNOWN)
			return environment;
		else 
			return new ValueEnvironment<>(new ConstantValue()).bottom();			
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

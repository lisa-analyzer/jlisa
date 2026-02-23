package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.program.operator.*;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
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
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import java.util.Set;

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

		return rts.stream().anyMatch(Type::isValueType) || rts.stream().anyMatch(t -> t.isStringType());
	}

	@Override
	public ConstantValue evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValue(constant.getValue());
	}

	@Override
	public ConstantValue evalUnaryExpression(
			UnaryExpression expression,
			ConstantValue arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
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
				return new ConstantValue((char) Character.toLowerCase(v));

		if (operator instanceof JavaCharacterToUpperCaseOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue((char) Character.toUpperCase(v));

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
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.sin(v));

		if (operator instanceof JavaMathCosOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.cos(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.cos(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.cos(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.cos(v));

		if (operator instanceof JavaMathSqrtOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.sqrt(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.sqrt(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.sqrt(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.sqrt(v));

		if (operator instanceof JavaMathTanOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.tan(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.tan(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.tan(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.tan(v));

		if (operator instanceof JavaMathAtanOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.atan(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.atan(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.atan(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.atan(v));

		if (operator instanceof JavaMathLogOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.log(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.log(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.log(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.log(v));

		if (operator instanceof JavaMathLog10Operator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.log10(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.log10(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.log10(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.log10(v));

		if (operator instanceof JavaMathAsinOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.asin(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.asin(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.asin(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.asin(v));

		if (operator instanceof JavaMathExpOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.exp(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.exp(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.exp(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.exp(v));

		if (operator instanceof JavaMathAcosOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.acos(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.acos(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.acos(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.acos(v));

		if (operator instanceof JavaMathFloorOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.floor(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.floor(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.floor(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.floor(v));

		if (operator instanceof JavaMathRoundOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.round(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.round(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.round(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Math.round(v));

		if (operator instanceof JavaMathToRadiansOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Math.toRadians(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Math.toRadians(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Math.toRadians(v));
			else if (arg.getValue() instanceof Long v)
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

		if (operator instanceof JavaStrictMathSinOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.sin(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.sin(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.sin(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.sin(v));

		if (operator instanceof JavaStrictMathCosOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.cos(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.cos(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.cos(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.cos(v));

		if (operator instanceof JavaStrictMathSqrtOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.sqrt(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.sqrt(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.sqrt(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.sqrt(v));

		if (operator instanceof JavaStrictMathTanOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.tan(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.tan(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.tan(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.tan(v));

		if (operator instanceof JavaStrictMathAtanOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.atan(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.atan(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.atan(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.atan(v));

		if (operator instanceof JavaStrictMathLogOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.log(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.log(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.log(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.log(v));

		if (operator instanceof JavaStrictMathLog10Operator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.log10(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.log10(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.log10(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.log10(v));

		if (operator instanceof JavaStrictMathAsinOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.asin(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.asin(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.asin(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.asin(v));

		if (operator instanceof JavaStrictMathExpOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.exp(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.exp(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.exp(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.exp(v));

		if (operator instanceof JavaStrictMathAcosOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.acos(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.acos(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.acos(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.acos(v));

		if (operator instanceof JavaStrictMathFloorOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.floor(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.floor(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.floor(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.floor(v));

		if (operator instanceof JavaStrictMathRoundOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.round(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.round(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.round(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.round(v));

		if (operator instanceof JavaStrictMathToRadiansOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.toRadians(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.toRadians(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.toRadians(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.toRadians(v));

		if (operator instanceof JavaStrictMathAbsOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.abs(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.abs(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.abs(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.abs(v));

		if (operator instanceof JavaStrictMathGetExponentOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(StrictMath.getExponent(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(StrictMath.getExponent(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(StrictMath.getExponent(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(StrictMath.getExponent(v));

		if (operator instanceof JavaDoubleToRawLongBitsOperator)
			if (arg.getValue() instanceof Double v)
				return new ConstantValue(Double.doubleToRawLongBits(v));
			else if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Double.doubleToRawLongBits(v));
			else if (arg.getValue() instanceof Float v)
				return new ConstantValue(Double.doubleToRawLongBits(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Double.doubleToRawLongBits(v));
			else if (arg.getValue() instanceof Number d)
				return new ConstantValue(Double.doubleToRawLongBits(d.doubleValue()));

		if (operator instanceof JavaDoubleLongBitsToDoubleOperator)
			if (arg.getValue() instanceof Integer v)
				return new ConstantValue(Double.longBitsToDouble(v));
			else if (arg.getValue() instanceof Long v)
				return new ConstantValue(Double.longBitsToDouble(v));

		if (operator instanceof JavaDoubleStaticToStringOperator)
			if (arg.getValue() instanceof Double d)
				return new ConstantValue(d.toString());
		if (operator instanceof JavaFloatStaticToStringOperator)
			if (arg.getValue() instanceof Float d)
				return new ConstantValue(d.toString());

		if (operator instanceof JavaFloatIsFiniteOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Float.isFinite(d.floatValue()));
		if (operator instanceof JavaDoubleIsFiniteOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Double.isFinite(d.doubleValue()));

		if (operator instanceof JavaFloatIsInfiniteOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Float.isInfinite(d.floatValue()));
		if (operator instanceof JavaDoubleIsInfiniteOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Double.isInfinite(d.doubleValue()));

		if (operator instanceof JavaFloatIsNaNOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Float.isNaN(d.floatValue()));
		if (operator instanceof JavaDoubleIsNaNOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Double.isNaN(d.doubleValue()));

		if (operator instanceof JavaDoubleToLongBitsOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Double.doubleToLongBits(d.doubleValue()));

		if (operator instanceof JavaFloatToIntBitsOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Float.floatToIntBits(d.floatValue()));
		if (operator instanceof JavaFloatToRawIntBitsOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Float.floatToRawIntBits(d.floatValue()));

		if (operator instanceof JavaDoubleToHexStringOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Double.toHexString(d.doubleValue()));

		if (operator instanceof JavaIntToBinaryStringOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Integer.toBinaryString(d.intValue()));
		if (operator instanceof JavaIntToOctalStringOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Integer.toOctalString(d.intValue()));
		if (operator instanceof JavaIntToHexStringOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Integer.toHexString(d.intValue()));

		if (operator instanceof JavaByteStaticToStringOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Byte.toString(d.byteValue()));

		if (operator instanceof JavaByteToUnsignedLongOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Byte.toUnsignedLong(d.byteValue()));

		if (operator instanceof JavaByteToUnsignedIntOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Byte.toUnsignedInt(d.byteValue()));

		if (operator instanceof JavaLongStaticToStringOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Long.toString(d.longValue()));
		if (operator instanceof JavaFloatStaticToStringOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Float.toString(d.floatValue()));
		if (operator instanceof JavaDoubleStaticToStringOperator)
			if (arg.getValue() instanceof Number d)
				return new ConstantValue(Double.toString(d.doubleValue()));

		if (operator instanceof JavaDoubleParseDoubleOperator)
			if (arg.getValue() instanceof String s)
				return new ConstantValue(Double.parseDouble(s));

		if (operator instanceof JavaFloatParseFloatOperator)
			if (arg.getValue() instanceof String s)
				return new ConstantValue(Float.parseFloat(s));

		if (operator instanceof JavaLongBitCountOperator)
			if (arg.getValue() instanceof Long l)
				return new ConstantValue(Long.bitCount(l));

		// strings
		if (operator instanceof JavaStringLengthOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.length());

		if (operator instanceof JavaStringToLowerCaseOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.toLowerCase());

		if (operator instanceof JavaStringToUpperCaseOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.toUpperCase());

		if (operator instanceof JavaStringTrimOperator && arg.getValue() instanceof String str)
			return new ConstantValue(str.trim());

		if (operator instanceof JavaStringValueOfLongOperator && arg.getValue() instanceof Long l)
			return new ConstantValue(String.valueOf(l));

		if (operator instanceof JavaStringValueOfBooleanOperator && arg.getValue() instanceof Boolean b)
			return new ConstantValue(String.valueOf(b));

		if (operator instanceof JavaStringValueOfDoubleOperator && arg.getValue() instanceof Double d)
			return new ConstantValue(String.valueOf(d));

		if (operator instanceof JavaStringValueOfFloatOperator && arg.getValue() instanceof Float f)
			return new ConstantValue(String.valueOf(f));

		if (operator instanceof JavaStringValueOfIntOperator && arg.getValue() instanceof Integer i)
			return new ConstantValue(String.valueOf(i));

		if (operator instanceof JavaStringValueOfCharOperator && arg.getValue() instanceof Integer i)
			return new ConstantValue(String.valueOf((char) i.intValue()));

		if (operator instanceof JavaStringValueOfObjectOperator && arg.getValue() instanceof Object o)
			return new ConstantValue(String.valueOf(o));

		if (operator instanceof JavaStringGetBytesOperator && arg.getValue() instanceof String s)
			return new ConstantValue(s.getBytes());

		if (operator instanceof JavaStringReverseOperator && arg.getValue() instanceof String s)
			return new ConstantValue(new StringBuilder(s).reverse().toString());

		if (operator instanceof JavaNumberIntValueOperator && arg.getValue() instanceof Long l)
			return new ConstantValue(l.intValue());

		// boolean
		if (operator instanceof LogicalNegation && arg.getValue() instanceof Boolean b)
			return new ConstantValue(!b);

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
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

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
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() | ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() | ((Number) rVal).intValue());
			}
		}

		if (operator instanceof BitwiseShiftRight) {
			Object lVal = left.getValue();
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() >> ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() >> ((Number) rVal).intValue());
			}
		}

		if (operator instanceof BitwiseUnsignedShiftRight) {
			Object lVal = left.getValue();
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() >>> ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() >>> ((Number) rVal).intValue());
			}
		}

		if (operator instanceof BitwiseShiftLeft) {
			Object lVal = left.getValue();
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() << ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() << ((Number) rVal).intValue());
			}
		}

		if (operator instanceof BitwiseXor) {
			Object lVal = left.getValue();
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() ^ ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() ^ ((Number) rVal).intValue());
			}
		}

		if (operator instanceof BitwiseAnd) {
			Object lVal = left.getValue();
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() & ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() & ((Number) rVal).intValue());
			}
		}

		if (operator instanceof SubtractionOperator) {
			Object lVal = left.getValue();
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

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
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

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
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

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
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

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
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

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
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Double || rVal instanceof Double || lVal instanceof Integer || rVal instanceof Integer
					|| lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(Math.pow(((Number) lVal).doubleValue(), ((Number) rVal).doubleValue()));
			}
		}

		if (operator instanceof JavaMathMax) {
			Object lVal = left.getValue();
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Double || rVal instanceof Double || lVal instanceof Integer || rVal instanceof Integer
					|| lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(Math.max(((Number) lVal).doubleValue(), ((Number) rVal).doubleValue()));
			}
		}

		if (operator instanceof JavaMathMin) {
			Object lVal = left.getValue();
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Double || rVal instanceof Double || lVal instanceof Integer || rVal instanceof Integer
					|| lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(Math.max(((Number) lVal).doubleValue(), ((Number) rVal).doubleValue()));
			}
		}

		if (operator instanceof JavaMathAtan2Operator) {
			Object lVal = left.getValue();
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			Object rVal = right.getValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Double || rVal instanceof Double || lVal instanceof Integer || rVal instanceof Integer
					|| lVal instanceof Float || rVal instanceof Float) {
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
			if (right.getValue() instanceof Character) {
				Character rv = ((Character) right.getValue());
				return new ConstantValue(lv + (rv.charValue()));
			} else if (right.getValue() instanceof Integer) {
				Integer rv = ((Integer) right.getValue());
				return new ConstantValue(lv + ((char) rv.intValue()));
			}
		}

		if (operator instanceof JavaStringAppendIntOperator) {
			String lv = ((String) left.getValue());
			if (right.getValue() instanceof Integer) {
				Integer rv = ((Integer) right.getValue());
				return new ConstantValue(lv + rv.intValue());
			}
		}

		if (operator instanceof JavaStringAppendLongOperator) {
			String lv = ((String) left.getValue());
			if (right.getValue() instanceof Long) {
				Long rv = ((Long) right.getValue());
				return new ConstantValue(lv + rv.longValue());
			} else if (right.getValue() instanceof Integer) {
				Integer rv = ((Integer) right.getValue());
				return new ConstantValue(lv + ((long) rv.intValue()));
			}
		}

		if (operator instanceof JavaStringAppendFloatOperator) {
			String lv = ((String) left.getValue());
			if (right.getValue() instanceof Float) {
				Float rv = ((Float) right.getValue());
				return new ConstantValue(lv + rv.floatValue());
			} else if (right.getValue() instanceof Integer) {
				Integer rv = ((Integer) right.getValue());
				return new ConstantValue(lv + ((float) rv.intValue()));
			}
		}

		if (operator instanceof JavaStringAppendDoubleOperator) {
			String lv = ((String) left.getValue());
			if (right.getValue() instanceof Double) {
				Double rv = ((Double) right.getValue());
				return new ConstantValue(lv + rv.doubleValue());
			} else if (right.getValue() instanceof Integer) {
				Integer rv = ((Integer) right.getValue());
				return new ConstantValue(lv + ((double) rv.intValue()));
			}
		}

		if (operator instanceof JavaStringAppendBooleanOperator) {
			String lv = ((String) left.getValue());
			Boolean rv = ((Boolean) right.getValue());
			return new ConstantValue(lv + rv.booleanValue());
		}

		if (operator instanceof JavaStringAppendStringOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv + rv);
		}

		if (operator instanceof JavaStringAppendCharArrayOperator) {
			return ConstantValue.TOP;
		}

		if (operator instanceof JavaStringAppendObjectOperator) {
			String lv = ((String) left.getValue());
			Object rv = right.getValue();
			return new ConstantValue(lv + rv.toString());
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
			return new ConstantValue(Character.forDigit(lv, rv));
		}

		if (operator instanceof JavaCharacterDigitOperator) {
			Integer lv = ((Integer) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(Character.digit(lv, rv));
		}

		if (operator instanceof JavaStringDeleteCharAtOperator) {
			String lv = ((String) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(new StringBuffer(lv).deleteCharAt(rv.intValue()).toString());
		}

		// long
		if (operator instanceof JavaLongRotateRightOperator) {
			Long lv = ((Long) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(Long.rotateRight(lv, rv));
		}

		if (operator instanceof JavaLongCompareOperator) {
			Long lv = ((Long) left.getValue());
			Long rv = ((Long) right.getValue());
			return new ConstantValue(Long.compare(lv, rv));
		}

		if (operator instanceof JavaFloatCompareOperator) {
			Float lv = ((Float) left.getValue());
			Float rv = ((Float) right.getValue());
			return new ConstantValue(Float.compare(lv, rv));
		}

		if (operator instanceof JavaDoubleCompareOperator) {
			Double lv = ((Double) left.getValue());
			Double rv = ((Double) right.getValue());
			return new ConstantValue(Double.compare(lv, rv));
		}
		if (operator instanceof JavaFloatCompareOperator) {
			Float lv = ((Float) left.getValue());
			Float rv = ((Float) right.getValue());
			return new ConstantValue(Float.compare(lv, rv));
		}
		if (operator instanceof JavaByteCompareOperator) {
			Byte lv = ((Byte) left.getValue());
			Byte rv = ((Byte) right.getValue());
			return new ConstantValue(Byte.compare(lv, rv));
		}

		if (operator instanceof JavaIntegerCompareOperator) {
			Integer lv = ((Integer) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(Integer.compare(lv, rv));
		}

		return top();
	}

	@Override
	public ConstantValue evalTernaryExpression(
			TernaryExpression expression,
			ConstantValue left,
			ConstantValue middle,
			ConstantValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// if left, middle or right is top, top is returned
		if (left.isTop() || middle.isTop() || right.isTop())
			return top();

		TernaryOperator operator = expression.getOperator();
		if (operator instanceof JavaStringInsertCharOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			if (right.getValue() instanceof Character) {
				Character rv = ((Character) right.getValue());
				return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), rv.charValue()).toString());
			} else if (right.getValue() instanceof Integer) {
				Integer rv = ((Integer) right.getValue());
				return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), (char) rv.intValue()).toString());
			}
		}

		if (operator instanceof JavaStringInsertBooleanOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Boolean rv = ((Boolean) right.getValue());
			return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), rv.booleanValue()).toString());
		}

		if (operator instanceof JavaStringInsertStringOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), rv).toString());
		}

		if (operator instanceof JavaStringInsertObjectOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Object rv = right.getValue();
			return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), rv.toString()).toString());
		}

		if (operator instanceof JavaStringInsertIntOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), rv.intValue()).toString());
		}

		if (operator instanceof JavaStringInsertLongOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			if (right.getValue() instanceof Long) {
				Long rv = ((Long) right.getValue());
				return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), rv.longValue()).toString());
			} else if (right.getValue() instanceof Integer) {
				Integer rv = ((Integer) right.getValue());
				return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), (long) rv.intValue()).toString());
			}
		}

		if (operator instanceof JavaStringInsertFloatOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			if (right.getValue() instanceof Float) {
				Float rv = ((Float) right.getValue());
				return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), rv.floatValue()).toString());
			} else if (right.getValue() instanceof Integer) {
				Integer rv = ((Integer) right.getValue());
				return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), (float) rv.intValue()).toString());
			}
		}

		if (operator instanceof JavaStringInsertDoubleOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			if (right.getValue() instanceof Double) {
				Double rv = ((Double) right.getValue());
				return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), rv.doubleValue()).toString());
			} else if (right.getValue() instanceof Integer) {
				Integer rv = ((Integer) right.getValue());
				return new ConstantValue(new StringBuffer(lv).insert(mv.intValue(), (double) rv.intValue()).toString());
			}
		}

		if (operator instanceof JavaStringInsertCharArrayOperator) {
			return ConstantValue.TOP;
		}

		if (operator instanceof JavaStringDeleteOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(new StringBuffer(lv).delete(mv.intValue(), rv.intValue()).toString());
		}

		if (operator instanceof JavaStringReplaceAllOperator) {
			String lv = ((String) left.getValue());
			String mv = ((String) middle.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.replaceAll(mv, rv));
		}

		if (operator instanceof JavaStringReplaceOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.replace((char) mv.intValue(), (char) rv.intValue()));
		}

		if (operator instanceof JavaStringReplaceFirstOperator) {
			String lv = ((String) left.getValue());
			String mv = ((String) middle.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.replaceFirst(mv, rv));
		}

		if (operator instanceof JavaStringIndexOfCharFromIndexOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.indexOf((char) mv.intValue(), rv));
		}

		if (operator instanceof JavaStringLastIndexOfCharFromIndexOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Integer rv = ((Integer) right.getValue());
			return new ConstantValue(lv.lastIndexOf((char) mv.intValue(), rv));
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

		if (operator instanceof JavaStringSetCharAtOperator) {
			String lv = ((String) left.getValue());
			Integer mv = ((Integer) middle.getValue());
			Character rv;
			if (right.getValue() instanceof Character c)
				rv = c;
			else
				rv = Character.valueOf((char) ((Integer) right.getValue()).intValue());

			if (lv.length() <= mv)
				return bottom();
			StringBuilder sb = new StringBuilder(lv);
			sb.setCharAt(mv, rv);
			return new ConstantValue(sb.toString());
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
		for (ConstantValue val : subExpressions)
			// we won't be able to compute the result if one of the arg is top
			if (val.isTop())
				return top();

		NaryOperator operator = ((NaryExpression) expression).getOperator();
		if (subExpressions.length == 4) {

			if (operator instanceof JavaStringAppendCharSubArrayOperator) {
				return ConstantValue.TOP;
			}
		}

		if (subExpressions.length == 5) {

			if (operator instanceof JavaStringRegionMatchesOperator) {
				String f = ((String) subExpressions[0].getValue());
				Integer s = ((Integer) subExpressions[1].getValue());
				String t = ((String) subExpressions[2].getValue());
				Integer fo = ((Integer) subExpressions[3].getValue());
				Integer fi = ((Integer) subExpressions[4].getValue());
				return new ConstantValue(f.regionMatches(s, t, fo, fi));
			}

			if (operator instanceof JavaStringInsertCharSubArrayOperator) {
				return ConstantValue.TOP;
			}

		} else if (subExpressions.length == 6) {

			if (operator instanceof JavaStringRegionMatchesIgnoreCaseOperator) {
				String f = ((String) subExpressions[0].getValue());
				Boolean s = ((Boolean) subExpressions[1].getValue());
				Integer t = ((Integer) subExpressions[2].getValue());
				String fo = ((String) subExpressions[3].getValue());
				Integer fi = ((Integer) subExpressions[4].getValue());
				Integer si = ((Integer) subExpressions[5].getValue());
				return new ConstantValue(f.regionMatches(s, t, fo, fi, si));
			}

		}

		return top();
	}

	@Override
	public Satisfiability satisfies(
			ValueEnvironment<ConstantValue> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof NaryExpression) {
			SymbolicExpression[] exprs = ((NaryExpression) expression).getAllOperand(0);
			ConstantValue[] args = new ConstantValue[exprs.length];

			for (int i = 0; i < exprs.length; ++i) {
				ConstantValue left = eval(state, (ValueExpression) exprs[i], pp, oracle);
				if (left.isBottom())
					return Satisfiability.BOTTOM;
				args[i] = left;
			}

			return satisfiesNaryExpression((NaryExpression) expression, args, pp, oracle);
		}

		return BaseNonRelationalValueDomain.super.satisfies(state, expression, pp, oracle);
	}

	public Satisfiability satisfiesNaryExpression(
			NaryExpression expression,
			ConstantValue[] arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {

		for (ConstantValue val : arg) {
			if (val.isTop())
				return Satisfiability.UNKNOWN;
		}

		NaryOperator operator = expression.getOperator();

		if (arg.length == 5) {

			if (operator instanceof JavaStringRegionMatchesOperator) {
				String f = ((String) arg[0].getValue());
				Integer s = ((Integer) arg[1].getValue());
				String t = ((String) arg[2].getValue());
				Integer fo = ((Integer) arg[3].getValue());
				Integer fi = ((Integer) arg[4].getValue());
				return f.regionMatches(s, t, fo, fi) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
			}
		} else if (arg.length == 6) {

			if (operator instanceof JavaStringRegionMatchesIgnoreCaseOperator) {
				String f = ((String) arg[0].getValue());
				Boolean s = ((Boolean) arg[1].getValue());
				Integer t = ((Integer) arg[2].getValue());
				String fo = ((String) arg[3].getValue());
				Integer fi = ((Integer) arg[4].getValue());
				Integer si = ((Integer) arg[5].getValue());
				return f.regionMatches(s, t, fo, fi, si) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
			}
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability satisfiesAbstractValue(
			ConstantValue value,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (value.getValue() instanceof Boolean)
			return ((Boolean) value.getValue()).booleanValue() ? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(
			UnaryExpression expression,
			ConstantValue arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
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

		if (operator instanceof JavaIsDoubleParsableOperator) {
			if (arg.getValue() instanceof String v)
				try {
					Double.parseDouble(v);
				} catch (NumberFormatException e) {
					return Satisfiability.NOT_SATISFIED;
				}
			return Satisfiability.SATISFIED;
		}

		if (operator instanceof JavaIsFloatParsableOperator) {
			if (arg.getValue() instanceof String v)
				try {
					Float.parseFloat(v);
				} catch (NumberFormatException e) {
					return Satisfiability.NOT_SATISFIED;
				}
			return Satisfiability.SATISFIED;
		}

		return BaseNonRelationalValueDomain.super.satisfiesUnaryExpression(expression, arg, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			ConstantValue left,
			ConstantValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
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

		if (operator instanceof JavaStringEqualsIgnoreCaseOperator) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.equalsIgnoreCase(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
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

			if (lVal instanceof Number && rVal instanceof Number)
				if (lVal instanceof Double || rVal instanceof Double) {
					return ((Number) lVal).doubleValue() == ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				} else if (lVal instanceof Float || rVal instanceof Float) {
					return ((Number) lVal).floatValue() == ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				} else if (lVal instanceof Long || rVal instanceof Long) {
					return ((Number) lVal).longValue() == ((Number) rVal).longValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				} else {
					return ((Number) lVal).intValue() == ((Number) rVal).intValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				}
			else if (lVal instanceof Boolean && rVal instanceof Boolean)
				return ((Boolean) lVal).booleanValue() == ((Boolean) rVal).booleanValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Character && rVal instanceof Character)
				return ((Character) lVal).charValue() == ((Character) rVal).charValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof ComparisonNe) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Number || rVal instanceof Number)
				if (lVal instanceof Double || rVal instanceof Double) {
					return ((Number) lVal).doubleValue() != ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				} else if (lVal instanceof Float || rVal instanceof Float) {
					return ((Number) lVal).floatValue() != ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				} else if (lVal instanceof Long || rVal instanceof Long) {
					return ((Number) lVal).longValue() != ((Number) rVal).longValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				} else {
					return ((Number) lVal).intValue() != ((Number) rVal).intValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				}
			else if (lVal instanceof Boolean && rVal instanceof Boolean)
				return ((Boolean) lVal).booleanValue() != ((Boolean) rVal).booleanValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Character && rVal instanceof Character)
				return ((Character) lVal).charValue() != ((Character) rVal).charValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof ComparisonLt) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return ((Number) lVal).doubleValue() < ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return ((Number) lVal).floatValue() < ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return ((Number) lVal).longValue() < ((Number) rVal).longValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else {
				return ((Number) lVal).intValue() < ((Number) rVal).intValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			}
		}

		if (operator instanceof ComparisonLe) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return ((Number) lVal).doubleValue() <= ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return ((Number) lVal).floatValue() <= ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return ((Number) lVal).longValue() <= ((Number) rVal).longValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else {
				return ((Number) lVal).intValue() <= ((Number) rVal).intValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			}
		}

		if (operator instanceof ComparisonGt) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return ((Number) lVal).doubleValue() > ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return ((Number) lVal).floatValue() > ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return ((Number) lVal).longValue() > ((Number) rVal).longValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else {
				return ((Number) lVal).intValue() > ((Number) rVal).intValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			}
		}

		if (operator instanceof ComparisonGe) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return ((Number) lVal).doubleValue() >= ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return ((Number) lVal).floatValue() >= ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return ((Number) lVal).longValue() >= ((Number) rVal).longValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			} else {
				return ((Number) lVal).intValue() >= ((Number) rVal).intValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
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
	public Satisfiability satisfiesTernaryExpression(
			TernaryExpression expression,
			ConstantValue left,
			ConstantValue middle,
			ConstantValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesTernaryExpression(expression, left, middle, right, pp,
				oracle);
	}

	@Override
	public Satisfiability satisfiesConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof Boolean)
			return ((Boolean) constant.getValue()).booleanValue() ? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<ConstantValue> assume(
			ValueEnvironment<ConstantValue> environment,
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
		return BaseNonRelationalValueDomain.super.assume(environment, expression, src, dest, oracle);
	}

	public ConstantValue evalTypeConv(
			BinaryExpression conv,
			ConstantValue left,
			ConstantValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (oracle.getRuntimeTypesOf(conv, pp).isEmpty())
			return left.bottom();

		if (left.getValue() instanceof Number)
			if (right.getValue() instanceof JavaByteType)
				return new ConstantValue(((Number) left.getValue()).byteValue());
			else if (right.getValue() instanceof JavaShortType)
				return new ConstantValue(((Number) left.getValue()).shortValue());
			else if (right.getValue() instanceof JavaIntType)
				return new ConstantValue(((Number) left.getValue()).intValue());
			else if (right.getValue() instanceof JavaLongType)
				return new ConstantValue(((Number) left.getValue()).longValue());
			else if (right.getValue() instanceof JavaDoubleType)
				return new ConstantValue(((Number) left.getValue()).doubleValue());
			else if (right.getValue() instanceof JavaCharType)
				return new ConstantValue((int) (char) ((Number) left.getValue()).longValue());

		return left;
	}

	@Override
	public ValueEnvironment<ConstantValue> assumeBinaryExpression(
			ValueEnvironment<ConstantValue> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Identifier id;
		ConstantValue eval;
		// boolean rightIsExpr;
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			eval = eval(environment, right, src, oracle);
			id = (Identifier) left;
			// rightIsExpr = true;
		} else if (right instanceof Identifier) {
			eval = eval(environment, left, src, oracle);
			id = (Identifier) right;
			// rightIsExpr = false;
		} else
			return environment;

		ConstantValue starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();
		if (eval.isTop())
			// if the value is not constant, we cannot refine the state
			return environment;

		ConstantValue update = null;
		// note for eq and neq: since we are tracking constants,
		// exact equality/inequality is handled through satisfies
		// (eg, x == 5 is either satisfied or not satisfied if x is constant);
		// here we just refine the state when we have an unknown satisfiability
		// (eg, x == 5 when x is top). This means that for eq we can just
		// set x to 5, and for neq we must leave x to top.
		// The only exception is with booleans: since they
		// can have only two values, for neq we can invert the boolean value.
		if (expression.getOperator() instanceof ComparisonEq)
			update = eval;
		else if (expression.getOperator() instanceof ComparisonNe)
			if (eval.getValue() instanceof Boolean) {
				Boolean b = (Boolean) eval.getValue();
				update = new ConstantValue(!b.booleanValue());
			} else
				update = top();

		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
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

package it.unive.jlisa.analysis;

import java.util.Set;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
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
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;

public class ConstantPropagation 
        implements 
        BaseNonRelationalValueDomain<ConstantValue> {
    

    private static boolean isAccepted(
            Type t) {
        JavaClassType.lookup("String", null);
        return t.isNumericType()
                || (t instanceof JavaClassType && ((JavaClassType)t).getUnit().getName().equals("String"))
                || t.isBooleanType()
                || t.isNullType()
                || t.isTypeTokenType();
    }

    @Override
    public boolean canProcess(
            SymbolicExpression expression,
            ProgramPoint pp,
            SemanticOracle oracle) {
        if (expression instanceof PushInv)
            // the type approximation of a pushinv is bottom, so the below check
            // will always fail regardless of the kind of value we are tracking
            return isAccepted(expression.getStaticType());

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

        return rts.stream().anyMatch(ConstantPropagation::isAccepted);
    }

    @Override
    public ConstantValue evalNullConstant(
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {
        return new ConstantValue(new Constant(NullType.INSTANCE, null, pp.getLocation()));
    }

    @Override
    public ConstantValue evalNonNullConstant(
            Constant constant,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {
        if (isAccepted(constant.getStaticType()))
            return new ConstantValue(constant);
        return ConstantValue.TOP;
    }

    @Override
    public ConstantValue evalTypeConv(BinaryExpression conv, ConstantValue left, ConstantValue right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (!(right.getValue() instanceof Type)) {
            return ConstantValue.BOTTOM; // ERROR
        }
        Type destType = (Type) right.getValue();

        if (left.getStaticType().canBeAssignedTo(destType)) {
            if (left.getValue() instanceof Number) {
                Number leftValue = (Number) left.getValue();
                if (destType instanceof JavaByteType) {
                    return new ConstantValue(new Constant(destType, leftValue.byteValue(), pp.getLocation()));
                }
                if (destType instanceof JavaShortType) {
                    return new ConstantValue(new Constant(destType, leftValue.shortValue(), pp.getLocation()));
                }
                if (destType instanceof JavaIntType) {
                    return new ConstantValue(new Constant(destType, leftValue.intValue(), pp.getLocation()));
                }
                if (destType instanceof JavaLongType) {
                    return new ConstantValue(new Constant(destType, leftValue.longValue(), pp.getLocation()));
                }
                if (destType instanceof JavaFloatType) {
                    return new ConstantValue(new Constant(destType, leftValue.floatValue(), pp.getLocation()));
                }
                if (destType instanceof JavaDoubleType) {
                    return new ConstantValue(new Constant(destType, leftValue.doubleValue(), pp.getLocation()));
                }
            }
            if (left.getValue() instanceof String) {
                return new ConstantValue(new Constant(destType, left.getValue(), pp.getLocation()));
            }
            return ConstantValue.BOTTOM;
        } else {
            return ConstantValue.TOP;
        }
    }

    @Override
    public ConstantValue evalBinaryExpression(
            BinaryExpression expression,
            ConstantValue left,
            ConstantValue right,
            ProgramPoint pp,
            SemanticOracle oracle) {
        BinaryOperator operator = expression.getOperator();
        if (operator instanceof AdditionOperator) {
            if (left.isTop() || right.isTop() || !left.getStaticType().isNumericType() || !right.getStaticType().isNumericType())
                return top();
            if ((left.getStaticType() instanceof JavaByteType || left.getStaticType() instanceof JavaShortType)) {
                return bottom();
            }
            Type superType = left.getStaticType().commonSupertype(right.getStaticType());
            Number leftValue = (Number) left.getValue();
            Number rightValue = (Number) right.getValue();
            if (superType instanceof JavaIntType) {
                return new ConstantValue(new Constant(superType, leftValue.intValue() + rightValue.intValue(), pp.getLocation()));
            }
            if (superType instanceof JavaLongType) {
                return new ConstantValue(new Constant(superType, leftValue.longValue() + rightValue.longValue(), pp.getLocation()));
            }
            if (superType instanceof JavaFloatType) {
                return new ConstantValue(new Constant(superType, leftValue.floatValue() + rightValue.floatValue(), pp.getLocation()));
            }
            if (superType instanceof JavaDoubleType) {
                return new ConstantValue(new Constant(superType, leftValue.doubleValue() + rightValue.doubleValue(), pp.getLocation()));
            }
            //new IntegerConstantPropagation(left.as + right.value);
        }
        if (operator instanceof SubtractionOperator) {
            if (left.isTop() || right.isTop() || !left.getStaticType().isNumericType() || !right.getStaticType().isNumericType())
                return top();
            if ((left.getStaticType() instanceof JavaByteType || left.getStaticType() instanceof JavaShortType)) {
                return bottom();
            }
            Type superType = left.getStaticType().commonSupertype(right.getStaticType());
            Number leftValue = (Number) left.getValue();
            Number rightValue = (Number) right.getValue();
            if (superType instanceof JavaIntType) {
                return new ConstantValue(new Constant(superType, leftValue.intValue() + rightValue.intValue(), pp.getLocation()));
            }
            if (superType instanceof JavaLongType) {
                return new ConstantValue(new Constant(superType, leftValue.longValue() + rightValue.longValue(), pp.getLocation()));
            }
            if (superType instanceof JavaFloatType) {
                return new ConstantValue(new Constant(superType, leftValue.floatValue() + rightValue.floatValue(), pp.getLocation()));
            }
            if (superType instanceof JavaDoubleType) {
                return new ConstantValue(new Constant(superType, leftValue.doubleValue() + rightValue.doubleValue(), pp.getLocation()));
            }

        }
        return top();
    }

	@Override
	public Satisfiability satisfiesAbstractValue(ConstantValue value, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesAbstractValue(value, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesNullConstant(ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesNullConstant(pp, oracle);
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(UnaryExpression expression, ConstantValue arg, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesUnaryExpression(expression, arg, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryExpression expression, ConstantValue left,
			ConstantValue right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesNonNullConstant(constant, pp, oracle);
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

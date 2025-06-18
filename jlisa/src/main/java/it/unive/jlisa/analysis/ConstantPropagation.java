package it.unive.jlisa.analysis;

import it.unive.jlisa.program.type.*;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Objects;
import java.util.Set;

public class ConstantPropagation implements BaseNonRelationalValueDomain<ConstantPropagation> {
    private static final ConstantPropagation TOP = new ConstantPropagation(null, false);
    private static final ConstantPropagation BOTTOM = new ConstantPropagation(null, true);

    private final boolean isTop;

    private final Constant constant;

    /**
     * Builds the top abstract value.
     */
    public ConstantPropagation() {
        this(null, true, false);
    }

    private ConstantPropagation(
            Constant constant,
            boolean isTop,
            boolean isBottom) {
        this.constant = constant;
        this.isTop = isTop;
    }

    private ConstantPropagation(
            Constant constant,
            boolean isTop) {
        this.constant = constant;
        this.isTop = isTop;
    }

    public ConstantPropagation(Constant constant) {
        this(constant, false);
    }

    public Object getConstant() {
        return constant.getValue();
    }

    public <T> boolean is(
            Class<T> type) {
        return type.isInstance(getConstant());
    }

    public <T> T as(
            Class<T> type) {
        return type.cast(getConstant());
    }

    @Override
    public String toString() {
        return representation().toString();
    }

    public StructuredRepresentation representation() {
        if (isTop())
            return Lattice.topRepresentation();
        if (isBottom())
            return Lattice.bottomRepresentation();
        return new StringRepresentation(constant);
    }

    @Override
    public ConstantPropagation top() {
        return TOP;
    }

    @Override
    public boolean isTop() {
        return BaseNonRelationalValueDomain.super.isTop() || (constant == null && isTop);
    }

    @Override
    public ConstantPropagation bottom() {
        return BOTTOM;
    }

    @Override
    public boolean isBottom() {
        return BaseNonRelationalValueDomain.super.isBottom() || (constant == null && !isTop);
    }

    @Override
    public ConstantPropagation lubAux(
            ConstantPropagation other)
            throws SemanticException {
        return Objects.equals(constant, other.constant) ? this : top();
    }

    @Override
    public ConstantPropagation wideningAux(
            ConstantPropagation other)
            throws SemanticException {
        return lubAux(other);
    }

    @Override
    public boolean lessOrEqualAux(
            ConstantPropagation other)
            throws SemanticException {
        return Objects.equals(constant, other.constant);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((constant == null) ? 0 : constant.hashCode());
        result = prime * result + (isTop ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(
            Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConstantPropagation other = (ConstantPropagation) obj;
        if (constant == null) {
            if (other.constant != null)
                return false;
        } else if (!constant.equals(other.constant))
            return false;
        return isTop == other.isTop;
    }

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
            rts = oracle.getRuntimeTypesOf(expression, pp, oracle);
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
    public ConstantPropagation evalNullConstant(
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {
        return new ConstantPropagation(new Constant(NullType.INSTANCE, null, pp.getLocation()));
    }

    @Override
    public ConstantPropagation evalNonNullConstant(
            Constant constant,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {
        if (isAccepted(constant.getStaticType()))
            return new ConstantPropagation(constant);
        return TOP;
    }

    @Override
    public ConstantPropagation evalTypeConv(BinaryExpression conv, ConstantPropagation left, ConstantPropagation right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (!(right.constant.getValue() instanceof Type)) {
            return BOTTOM; // ERROR
        }
        Type destType = (Type) right.constant.getValue();

        if (left.constant.getStaticType().canBeAssignedTo(destType)) {
            if (left.constant.getValue() instanceof Number) {
                Number leftValue = (Number) left.constant.getValue();
                if (destType instanceof JavaByteType) {
                    return new ConstantPropagation(new Constant(destType, leftValue.byteValue(), pp.getLocation()));
                }
                if (destType instanceof JavaShortType) {
                    return new ConstantPropagation(new Constant(destType, leftValue.shortValue(), pp.getLocation()));
                }
                if (destType instanceof JavaIntType) {
                    return new ConstantPropagation(new Constant(destType, leftValue.intValue(), pp.getLocation()));
                }
                if (destType instanceof JavaLongType) {
                    return new ConstantPropagation(new Constant(destType, leftValue.longValue(), pp.getLocation()));
                }
                if (destType instanceof JavaFloatType) {
                    return new ConstantPropagation(new Constant(destType, leftValue.floatValue(), pp.getLocation()));
                }
                if (destType instanceof JavaDoubleType) {
                    return new ConstantPropagation(new Constant(destType, leftValue.doubleValue(), pp.getLocation()));
                }
            }
            if (left.constant.getValue() instanceof String) {
                return new ConstantPropagation(new Constant(destType, left.constant.getValue(), pp.getLocation()));
            }
            return BOTTOM;
        } else {
            return TOP;
        }
    }

    @Override
    public ConstantPropagation evalBinaryExpression(
            BinaryOperator operator,
            ConstantPropagation left,
            ConstantPropagation right,
            ProgramPoint pp,
            SemanticOracle oracle) {
        if (operator instanceof AdditionOperator) {
            if (left.isTop() || right.isTop() || !left.constant.getStaticType().isNumericType() || !right.constant.getStaticType().isNumericType())
                return top();
            if ((left.constant.getStaticType() instanceof JavaByteType || left.constant.getStaticType() instanceof JavaShortType)) {
                return bottom();
            }
            Type superType = left.constant.getStaticType().commonSupertype(right.constant.getStaticType());
            Number leftValue = (Number) left.constant.getValue();
            Number rightValue = (Number) right.constant.getValue();
            if (superType instanceof JavaIntType) {
                return new ConstantPropagation(new Constant(superType, leftValue.intValue() + rightValue.intValue(), pp.getLocation()));
            }
            if (superType instanceof JavaLongType) {
                return new ConstantPropagation(new Constant(superType, leftValue.longValue() + rightValue.longValue(), pp.getLocation()));
            }
            if (superType instanceof JavaFloatType) {
                return new ConstantPropagation(new Constant(superType, leftValue.floatValue() + rightValue.floatValue(), pp.getLocation()));
            }
            if (superType instanceof JavaDoubleType) {
                return new ConstantPropagation(new Constant(superType, leftValue.doubleValue() + rightValue.doubleValue(), pp.getLocation()));
            }
            //new IntegerConstantPropagation(left.as + right.value);
        }
        if (operator instanceof SubtractionOperator) {
            if (left.isTop() || right.isTop() || !left.constant.getStaticType().isNumericType() || !right.constant.getStaticType().isNumericType())
                return top();
            if ((left.constant.getStaticType() instanceof JavaByteType || left.constant.getStaticType() instanceof JavaShortType)) {
                return bottom();
            }
            Type superType = left.constant.getStaticType().commonSupertype(right.constant.getStaticType());
            Number leftValue = (Number) left.constant.getValue();
            Number rightValue = (Number) right.constant.getValue();
            if (superType instanceof JavaIntType) {
                return new ConstantPropagation(new Constant(superType, leftValue.intValue() + rightValue.intValue(), pp.getLocation()));
            }
            if (superType instanceof JavaLongType) {
                return new ConstantPropagation(new Constant(superType, leftValue.longValue() + rightValue.longValue(), pp.getLocation()));
            }
            if (superType instanceof JavaFloatType) {
                return new ConstantPropagation(new Constant(superType, leftValue.floatValue() + rightValue.floatValue(), pp.getLocation()));
            }
            if (superType instanceof JavaDoubleType) {
                return new ConstantPropagation(new Constant(superType, leftValue.doubleValue() + rightValue.doubleValue(), pp.getLocation()));
            }

        }
        return top();
    }
}

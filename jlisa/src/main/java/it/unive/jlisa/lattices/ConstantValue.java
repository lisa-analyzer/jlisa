package it.unive.jlisa.lattices;

import java.util.Objects;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class ConstantValue 
        implements
        BaseLattice<ConstantValue> {
    
    public static final ConstantValue TOP = new ConstantValue(null, false);
    public static final ConstantValue BOTTOM = new ConstantValue(null, true);

    private final boolean isTop;

    private final Constant constant;

    /**
     * Builds the top abstract value.
     */
    public ConstantValue() {
        this(null, true, false);
    }

    private ConstantValue(
            Constant constant,
            boolean isTop,
            boolean isBottom) {
        this.constant = constant;
        this.isTop = isTop;
    }

    private ConstantValue(
            Constant constant,
            boolean isTop) {
        this.constant = constant;
        this.isTop = isTop;
    }

    public ConstantValue(Constant constant) {
        this(constant, false);
    }

    public Object getValue() {
        return constant.getValue();
    }

    public Type getStaticType() {
        return constant.getStaticType();
    }

    public <T> boolean is(
            Class<T> type) {
        return type.isInstance(getValue());
    }

    public <T> T as(
            Class<T> type) {
        return type.cast(getValue());
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
    public ConstantValue top() {
        return TOP;
    }

    @Override
    public boolean isTop() {
        return BaseLattice.super.isTop() || (constant == null && isTop);
    }

    @Override
    public ConstantValue bottom() {
        return BOTTOM;
    }

    @Override
    public boolean isBottom() {
        return BaseLattice.super.isBottom() || (constant == null && !isTop);
    }

    @Override
    public ConstantValue lubAux(
            ConstantValue other)
            throws SemanticException {
        return Objects.equals(constant, other.constant) ? this : top();
    }

    @Override
    public ConstantValue wideningAux(
            ConstantValue other)
            throws SemanticException {
        return lubAux(other);
    }

    @Override
    public boolean lessOrEqualAux(
            ConstantValue other)
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
        ConstantValue other = (ConstantValue) obj;
        if (constant == null) {
            if (other.constant != null)
                return false;
        } else if (!constant.equals(other.constant))
            return false;
        return isTop == other.isTop;
    }
}

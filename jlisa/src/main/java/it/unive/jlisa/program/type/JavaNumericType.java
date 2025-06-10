package it.unive.jlisa.program.type;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public interface JavaNumericType extends NumericType {

    default Type commonSupertype(
            Type other) {
        if (!(other instanceof JavaNumericType)) {
            return Untyped.INSTANCE;
        }
        return supertype(other.asNumericType());
    }

    default boolean canBeAssignedTo(
            Type other) {
        if (other.isUntyped()) {
            return true;
        }
        return commonSupertype(other).equals(other);
    }
}

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

	private static int distanceFromSmallest(
			JavaNumericType type) {
		if (type.is8Bits()) { // byte
			return 0;
		}
		if (type.is16Bits()) { // short, char
			return 1;
		}
		if (type.is32Bits() && type.isIntegral()) { // int
			return 2;
		}
		if (type.is64Bits() && type.isIntegral()) { // long
			return 3;
		}
		if (type.is32Bits() && !type.isIntegral()) { // float
			return 4;
		}
		if (type.is64Bits() && !type.isIntegral()) { // double
			return 5;
		}
		return -1; // incomparable (should never happen)
	}

	default int distance(
			JavaNumericType other) {
		return distanceFromSmallest(other) - distanceFromSmallest(this);
	}

}

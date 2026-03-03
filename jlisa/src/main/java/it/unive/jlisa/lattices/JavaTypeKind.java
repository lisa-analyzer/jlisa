package it.unive.jlisa.lattices;

import it.unive.jlisa.program.type.JavaNumericType;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * An enumeration of the Java numeric primitive types, organised as a lattice
 * for use in the {@link JavaFlaggedInterval} domain.
 * <p>
 * The lattice order reflects value-range containment:
 *
 * <pre>
 *                 UNKNOWN
 *              /    |     \
 *           LONG  DOUBLE  (non-numeric)
 *            |       |
 *           INT    FLOAT
 *            |
 *          SHORT
 *            |
 *           BYTE
 * </pre>
 * <p>
 * Note: {@code INT} and {@code FLOAT} are <em>incomparable</em> because neither
 * type's value range subsumes the other's. When both appear together (e.g. in a
 * type-union), the safe choice is {@code DOUBLE}, the widest type that can
 * represent all values without overflow.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum JavaTypeKind {

	/** Unknown / non-numeric type. Acts as the top element. */
	UNKNOWN,

	/** 8-bit signed integer ({@code byte}). */
	BYTE,

	/** 16-bit signed integer ({@code short}). */
	SHORT,

	/** 32-bit signed integer ({@code int}). */
	INT,

	/** 64-bit signed integer ({@code long}). */
	LONG,

	/** 32-bit IEEE 754 floating-point ({@code float}). */
	FLOAT,

	/** 64-bit IEEE 754 floating-point ({@code double}). */
	DOUBLE;

	/**
	 * Infers the {@link JavaTypeKind} from a single LiSA {@link Type} object.
	 * Returns {@link #UNKNOWN} for non-numeric types.
	 *
	 * @param t the type to inspect
	 *
	 * @return the corresponding kind
	 */
	public static JavaTypeKind fromType(
			Type t) {
		if (!(t instanceof JavaNumericType jnt))
			return UNKNOWN;
		if (!jnt.isIntegral())
			return jnt.getNBits() == 32 ? FLOAT : DOUBLE;
		switch (jnt.getNBits()) {
		case 8:
			return BYTE;
		case 16:
			return SHORT;
		case 32:
			return INT;
		case 64:
			return LONG;
		default:
			return UNKNOWN;
		}
	}

	/**
	 * Infers the {@link JavaTypeKind} from a set of runtime types returned by
	 * the oracle. When multiple types are present, their least upper bound in
	 * this lattice is taken (safe over-approximation).
	 *
	 * @param types the set of runtime types; may be {@code null} or empty
	 *
	 * @return the corresponding kind (defaults to {@link #UNKNOWN})
	 */
	public static JavaTypeKind fromTypes(
			Set<Type> types) {
		if (types == null || types.isEmpty())
			return UNKNOWN;
		JavaTypeKind result = null;
		for (Type t : types) {
			JavaTypeKind k = fromType(t);
			result = result == null ? k : result.lub(k);
		}
		return result == null ? UNKNOWN : result;
	}

	/**
	 * Returns the least upper bound of this kind and {@code other} in the
	 * lattice.
	 *
	 * @param other the other kind
	 *
	 * @return the least upper bound
	 */
	public JavaTypeKind lub(
			JavaTypeKind other) {
		if (this == UNKNOWN || other == UNKNOWN)
			return UNKNOWN;
		if (this == other)
			return this;
		if (isInteger() && other.isInteger())
			return rank() >= other.rank() ? this : other;
		if (isFloat() && other.isFloat())
			return DOUBLE;
		// Mixed integer / float → DOUBLE
		return DOUBLE;
	}

	/**
	 * Returns {@code true} if this kind represents an integer type
	 * ({@link #BYTE}, {@link #SHORT}, {@link #INT}, or {@link #LONG}).
	 *
	 * @return {@code true} for integer kinds
	 */
	public boolean isInteger() {
		return this == BYTE || this == SHORT || this == INT || this == LONG;
	}

	/**
	 * Returns {@code true} if this kind represents a floating-point type
	 * ({@link #FLOAT} or {@link #DOUBLE}).
	 *
	 * @return {@code true} for floating-point kinds
	 */
	public boolean isFloat() {
		return this == FLOAT || this == DOUBLE;
	}

	/**
	 * Returns the minimum value representable by this type as a {@code long}.
	 * Falls back to {@link Long#MIN_VALUE} for non-integer kinds.
	 *
	 * @return the minimum representable value
	 */
	public long minValueLong() {
		switch (this) {
		case BYTE:
			return Byte.MIN_VALUE;
		case SHORT:
			return Short.MIN_VALUE;
		case INT:
			return Integer.MIN_VALUE;
		case LONG:
			return Long.MIN_VALUE;
		default:
			return Long.MIN_VALUE;
		}
	}

	/**
	 * Returns the maximum value representable by this type as a {@code long}.
	 * Falls back to {@link Long#MAX_VALUE} for non-integer kinds.
	 *
	 * @return the maximum representable value
	 */
	public long maxValueLong() {
		switch (this) {
		case BYTE:
			return Byte.MAX_VALUE;
		case SHORT:
			return Short.MAX_VALUE;
		case INT:
			return Integer.MAX_VALUE;
		case LONG:
			return Long.MAX_VALUE;
		default:
			return Long.MAX_VALUE;
		}
	}

	/**
	 * Returns the type-bounded {@link it.unive.lisa.util.numeric.IntInterval}
	 * for this kind: {@code [minValueLong(), maxValueLong()]} for integer
	 * kinds, and {@link it.unive.lisa.util.numeric.IntInterval#TOP} for
	 * floating-point and unknown kinds.
	 *
	 * @return the bounded interval
	 */
	public it.unive.lisa.util.numeric.IntInterval toBoundedInterval() {
		if (!isInteger())
			return it.unive.lisa.util.numeric.IntInterval.TOP;
		return new it.unive.lisa.util.numeric.IntInterval(
				new it.unive.lisa.util.numeric.MathNumber(minValueLong()),
				new it.unive.lisa.util.numeric.MathNumber(maxValueLong()));
	}

	/**
	 * Returns an integer rank used to establish the integer sub-lattice order
	 * ({@link #BYTE} = 0, …, {@link #LONG} = 3).
	 *
	 * @return the rank
	 */
	private int rank() {
		switch (this) {
		case BYTE:
			return 0;
		case SHORT:
			return 1;
		case INT:
			return 2;
		case LONG:
			return 3;
		case FLOAT:
			return 4;
		case DOUBLE:
			return 5;
		default:
			return -1;
		}
	}
}

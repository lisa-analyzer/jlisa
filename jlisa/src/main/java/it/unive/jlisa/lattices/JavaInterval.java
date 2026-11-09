package it.unive.jlisa.lattices;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;

/**
 * An interval of LiSA ({@link IntInterval}) where NaN is a value of the
 * lattice. {@link #NaN} (only NaN) is incomparable with the other intervals,
 * and {@link #TOP} (every value, NaN included) is above both: the least upper
 * bound of NaN and an interval is {@link #TOP}, and their greatest lower bound
 * is bottom. [-∞, +∞] ({@link #INFINITY}) is every number, NaN excluded: it is
 * the largest interval of an integral type (whose values are never NaN), and it
 * is below {@link #TOP} for the floating point types. The operations between
 * intervals without NaN are the ones of {@link IntInterval}, except that their
 * results are never {@link #TOP}.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaInterval
		extends
		IntInterval {

	/**
	 * Every value, NaN included.
	 */
	public static final JavaInterval TOP = new JavaInterval(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY,
			true);

	/**
	 * Every number, NaN excluded.
	 */
	public static final JavaInterval INFINITY = new JavaInterval(MathNumber.MINUS_INFINITY,
			MathNumber.PLUS_INFINITY, false);

	/**
	 * Only NaN.
	 */
	public static final JavaInterval NaN = new JavaInterval(MathNumber.NaN, MathNumber.NaN, false);

	// whether this is TOP, that has the bounds of INFINITY
	private final boolean everyValue;

	private JavaInterval(
			MathNumber low,
			MathNumber high,
			boolean everyValue) {
		super(low, high);
		this.everyValue = everyValue;
	}

	/**
	 * Returns the given interval of {@link IntInterval} as an interval of this
	 * class: {@link IntInterval#TOP}, that stands for any value, is
	 * {@link #TOP}, [NaN, NaN] is {@link #NaN}, and the other intervals (e.g.,
	 * [-∞, +∞] computed by an operation) keep their bounds.
	 *
	 * @param interval the interval, possibly {@code null}
	 *
	 * @return the interval of this class
	 */
	public static IntInterval of(
			IntInterval interval) {
		if (interval == null || interval instanceof JavaInterval || interval.isBottom())
			return interval;
		if (interval == IntInterval.TOP)
			return TOP;
		if (isNaN(interval))
			return NaN;
		return new JavaInterval(interval.getLow(), interval.getHigh(), false);
	}

	/**
	 * Returns whether the given interval is the one of NaN alone.
	 *
	 * @param interval the interval
	 *
	 * @return whether the interval is NaN
	 */
	public static boolean isNaN(
			IntInterval interval) {
		return !interval.isBottom() && interval.getLow().isNaN();
	}

	@Override
	public IntInterval top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return everyValue;
	}

	@Override
	public IntInterval lubAux(
			IntInterval other)
			throws SemanticException {
		if (isNaN(this) != isNaN(other))
			return TOP;
		return number(super.lubAux(other));
	}

	@Override
	public IntInterval glbAux(
			IntInterval other) {
		if (isNaN(this) != isNaN(other))
			return bottom();
		return number(super.glbAux(other));
	}

	@Override
	public IntInterval wideningAux(
			IntInterval other)
			throws SemanticException {
		if (isNaN(this) != isNaN(other))
			return TOP;
		return number(super.wideningAux(other));
	}

	@Override
	public IntInterval narrowingAux(
			IntInterval other)
			throws SemanticException {
		if (isNaN(this) != isNaN(other))
			// nothing is refined
			return this;
		return number(super.narrowingAux(other));
	}

	@Override
	public boolean lessOrEqualAux(
			IntInterval other)
			throws SemanticException {
		if (isNaN(this) != isNaN(other))
			// NaN and the intervals of numbers are incomparable
			return false;
		return super.lessOrEqualAux(other);
	}

	// the result of an operation of IntInterval between two intervals without
	// NaN: IntInterval returns its top for [-∞, +∞], which is every number here
	private static IntInterval number(
			IntInterval result) {
		if (result.isTop())
			return INFINITY;
		return of(result);
	}

	@Override
	public int hashCode() {
		return everyValue ? 31 : super.hashCode();
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof JavaInterval other && (everyValue || other.everyValue))
			// TOP is equal only to itself
			return false;
		return super.equals(obj);
	}
}

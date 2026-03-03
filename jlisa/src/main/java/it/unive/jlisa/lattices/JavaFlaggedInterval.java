package it.unive.jlisa.lattices;

import it.unive.jlisa.lattices.flags.InfinityFlag;
import it.unive.jlisa.lattices.flags.IntegralityFlag;
import it.unive.jlisa.lattices.flags.NaNFlag;
import it.unive.jlisa.lattices.flags.ParityFlag;
import it.unive.jlisa.lattices.flags.ProvenanceFlag;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A lattice element that extends a standard over-approximating interval with:
 * <ul>
 * <li>a safe <em>under-approximation</em> of the reachable values;</li>
 * <li>a {@link JavaTypeKind} tag for type-bounded overflow semantics;</li>
 * <li>five annotation flags: {@link ProvenanceFlag}, {@link IntegralityFlag},
 * {@link NaNFlag}, {@link InfinityFlag}, {@link ParityFlag}.</li>
 * </ul>
 * <h2>Lattice order</h2>
 * 
 * <pre>
 *   (a1, u1, t1, flags1...) ⊑ (a2, u2, t2, flags2...) iff
 *     a1 ⊆ a2        (over-approx: standard interval containment)
 *     u1 ⊇ u2        (under-approx: REVERSED — a larger under-approx ≙ more info)
 *     ∀ flag: flag1 ⊑ flag2  (flag lattice order)
 * </pre>
 *
 * <h2>Top / Bottom</h2>
 * <ul>
 * <li>⊤ : over = {@link IntInterval#TOP}, under = ⊥, type =
 * {@link JavaTypeKind#UNKNOWN}, all flags = their respective ⊤.</li>
 * <li>⊥ : over = {@link IntInterval#BOTTOM}, all flags = their respective
 * ⊥.</li>
 * </ul>
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaFlaggedInterval
		implements
		BaseLattice<JavaFlaggedInterval> {

	/** The top element of this lattice. */
	public static final JavaFlaggedInterval TOP = new JavaFlaggedInterval(
			IntInterval.TOP,
			null,
			JavaTypeKind.UNKNOWN,
			ProvenanceFlag.MIXED,
			IntegralityFlag.UNKNOWN_INTEGRALITY,
			NaNFlag.POSSIBLY_NAN,
			InfinityFlag.POSSIBLY_INFINITE_OR_FINITE,
			ParityFlag.UNKNOWN_PARITY);

	/** The bottom element of this lattice. */
	public static final JavaFlaggedInterval BOTTOM = new JavaFlaggedInterval(
			IntInterval.BOTTOM,
			null,
			JavaTypeKind.UNKNOWN,
			ProvenanceFlag.BOTTOM,
			IntegralityFlag.BOTTOM,
			NaNFlag.BOTTOM,
			InfinityFlag.BOTTOM,
			ParityFlag.BOTTOM);

	/** Over-approximation (standard interval). */
	private final IntInterval over;

	/**
	 * Under-approximation ({@code null} = ⊥, meaning unknown / no guarantee).
	 */
	private final IntInterval under;

	/** Java numeric type tag used for overflow-bounded clamping. */
	private final JavaTypeKind type;

	/** Provenance flag. */
	private final ProvenanceFlag provenance;

	/** Integrality flag (most relevant for float/double values). */
	private final IntegralityFlag integrality;

	/** NaN flag (float/double only). */
	private final NaNFlag nan;

	/** Infinity flag (float/double only). */
	private final InfinityFlag infinity;

	/** Parity flag (integer types only). */
	private final ParityFlag parity;

	/**
	 * Constructs a fully specified {@link JavaFlaggedInterval}.
	 *
	 * @param over        the over-approximating interval
	 * @param under       the under-approximating interval (may be {@code null}
	 *                        to indicate unknown under-approx)
	 * @param type        the Java numeric type kind
	 * @param provenance  the provenance flag
	 * @param integrality the integrality flag
	 * @param nan         the NaN flag
	 * @param infinity    the infinity flag
	 * @param parity      the parity flag
	 */
	public JavaFlaggedInterval(
			IntInterval over,
			IntInterval under,
			JavaTypeKind type,
			ProvenanceFlag provenance,
			IntegralityFlag integrality,
			NaNFlag nan,
			InfinityFlag infinity,
			ParityFlag parity) {
		this.over = over == null ? IntInterval.BOTTOM : over;
		this.under = under;
		this.type = type == null ? JavaTypeKind.UNKNOWN : type;
		this.provenance = provenance == null ? ProvenanceFlag.BOTTOM : provenance;
		this.integrality = integrality == null ? IntegralityFlag.BOTTOM : integrality;
		this.nan = nan == null ? NaNFlag.BOTTOM : nan;
		this.infinity = infinity == null ? InfinityFlag.BOTTOM : infinity;
		this.parity = parity == null ? ParityFlag.BOTTOM : parity;
	}

	/**
	 * Constructs a {@link JavaFlaggedInterval} from a singleton value with
	 * type-derived flags for integer types.
	 *
	 * @param value the singleton value
	 * @param type  the Java numeric type kind
	 *
	 * @return the constructed interval
	 */
	public static JavaFlaggedInterval ofConstant(
			long value,
			JavaTypeKind type) {
		IntInterval interval = new IntInterval(new MathNumber(value), new MathNumber(value));
		ParityFlag par = (value % 2 == 0) ? ParityFlag.EVEN : ParityFlag.ODD;
		IntegralityFlag integ = type.isFloat() ? IntegralityFlag.INTEGRAL : IntegralityFlag.INTEGRAL;
		return new JavaFlaggedInterval(
				interval,
				interval,
				type,
				ProvenanceFlag.ANALYSIS,
				integ,
				NaNFlag.DEFINITELY_NOT_NAN,
				InfinityFlag.DEFINITELY_FINITE,
				par);
	}

	/**
	 * Constructs a {@link JavaFlaggedInterval} from a double constant with
	 * float-type flags.
	 *
	 * @param value the double constant
	 * @param type  the Java numeric type kind ({@link JavaTypeKind#FLOAT} or
	 *                  {@link JavaTypeKind#DOUBLE})
	 *
	 * @return the constructed interval
	 */
	public static JavaFlaggedInterval ofDoubleConstant(
			double value,
			JavaTypeKind type) {
		if (Double.isNaN(value))
			return new JavaFlaggedInterval(
					IntInterval.BOTTOM,
					null,
					type,
					ProvenanceFlag.ANALYSIS,
					IntegralityFlag.NON_INTEGRAL,
					NaNFlag.DEFINITELY_NAN,
					InfinityFlag.BOTTOM,
					ParityFlag.BOTTOM);
		if (value == Double.POSITIVE_INFINITY)
			return new JavaFlaggedInterval(
					IntInterval.BOTTOM,
					null,
					type,
					ProvenanceFlag.ANALYSIS,
					IntegralityFlag.NON_INTEGRAL,
					NaNFlag.DEFINITELY_NOT_NAN,
					InfinityFlag.POS_INF,
					ParityFlag.BOTTOM);
		if (value == Double.NEGATIVE_INFINITY)
			return new JavaFlaggedInterval(
					IntInterval.BOTTOM,
					null,
					type,
					ProvenanceFlag.ANALYSIS,
					IntegralityFlag.NON_INTEGRAL,
					NaNFlag.DEFINITELY_NOT_NAN,
					InfinityFlag.NEG_INF,
					ParityFlag.BOTTOM);
		IntInterval interval = new IntInterval(new MathNumber(value), new MathNumber(value));
		boolean isWhole = value == Math.floor(value) && !Double.isInfinite(value);
		IntegralityFlag integ = isWhole ? IntegralityFlag.INTEGRAL : IntegralityFlag.NON_INTEGRAL;
		return new JavaFlaggedInterval(
				interval,
				interval,
				type,
				ProvenanceFlag.ANALYSIS,
				integ,
				NaNFlag.DEFINITELY_NOT_NAN,
				InfinityFlag.DEFINITELY_FINITE,
				ParityFlag.BOTTOM);
	}

	/**
	 * Creates an exact (INPUT-provenance) interval for a non-deterministic
	 * value of the given type.
	 *
	 * @param type the Java numeric type kind
	 *
	 * @return the constructed interval representing any value of the type
	 */
	public static JavaFlaggedInterval ofNonDet(
			JavaTypeKind type) {
		if (type == JavaTypeKind.UNKNOWN)
			return TOP;
		IntInterval bounds;
		if (type.isInteger())
			bounds = new IntInterval(new MathNumber(type.minValueLong()), new MathNumber(type.maxValueLong()));
		else
			bounds = IntInterval.TOP;
		IntegralityFlag integ = type.isInteger() ? IntegralityFlag.INTEGRAL : IntegralityFlag.UNKNOWN_INTEGRALITY;
		NaNFlag nanFlag = type.isFloat() ? NaNFlag.POSSIBLY_NAN : NaNFlag.DEFINITELY_NOT_NAN;
		InfinityFlag infFlag = type.isFloat() ? InfinityFlag.POSSIBLY_INFINITE_OR_FINITE
				: InfinityFlag.DEFINITELY_FINITE;
		ParityFlag parFlag = type.isInteger() ? ParityFlag.UNKNOWN_PARITY : ParityFlag.BOTTOM;
		return new JavaFlaggedInterval(
				bounds,
				bounds,
				type,
				ProvenanceFlag.INPUT,
				integ,
				nanFlag,
				infFlag,
				parFlag);
	}

	/**
	 * Creates a {@link JavaFlaggedInterval} with
	 * {@link ProvenanceFlag#ANALYSIS} provenance from a pre-computed
	 * over-approximating interval and a type kind. No under-approximation is
	 * set. This factory is used for structurally constrained unknowns such as
	 * array lengths.
	 *
	 * @param over the over-approximating interval (must not be {@code null})
	 * @param type the Java type kind
	 *
	 * @return the constructed interval
	 */
	public static JavaFlaggedInterval ofAnalysis(
			IntInterval over,
			JavaTypeKind type) {
		if (type == JavaTypeKind.UNKNOWN || over == null)
			return TOP;
		IntegralityFlag integ = type.isInteger() ? IntegralityFlag.INTEGRAL : IntegralityFlag.UNKNOWN_INTEGRALITY;
		NaNFlag nanFlag = type.isFloat() ? NaNFlag.POSSIBLY_NAN : NaNFlag.DEFINITELY_NOT_NAN;
		InfinityFlag infFlag = type.isFloat() ? InfinityFlag.POSSIBLY_INFINITE_OR_FINITE
				: InfinityFlag.DEFINITELY_FINITE;
		ParityFlag parFlag = type.isInteger() ? ParityFlag.UNKNOWN_PARITY : ParityFlag.BOTTOM;
		return new JavaFlaggedInterval(
				over,
				null,
				type,
				ProvenanceFlag.ANALYSIS,
				integ,
				nanFlag,
				infFlag,
				parFlag);
	}

	// -------------------------------------------------------------------------
	// Accessors
	// -------------------------------------------------------------------------

	/**
	 * Returns the over-approximating interval.
	 *
	 * @return the over-approximation
	 */
	public IntInterval getOver() {
		return over;
	}

	/**
	 * Returns the under-approximating interval, or {@code null} if no
	 * under-approximation is available (⊥).
	 *
	 * @return the under-approximation, or {@code null}
	 */
	public IntInterval getUnder() {
		return under;
	}

	/**
	 * Returns the Java numeric type kind.
	 *
	 * @return the type kind
	 */
	public JavaTypeKind getType() {
		return type;
	}

	/**
	 * Returns the provenance flag.
	 *
	 * @return the provenance
	 */
	public ProvenanceFlag getProvenance() {
		return provenance;
	}

	/**
	 * Returns the integrality flag.
	 *
	 * @return the integrality
	 */
	public IntegralityFlag getIntegrality() {
		return integrality;
	}

	/**
	 * Returns the NaN flag.
	 *
	 * @return the NaN flag
	 */
	public NaNFlag getNan() {
		return nan;
	}

	/**
	 * Returns the infinity flag.
	 *
	 * @return the infinity flag
	 */
	public InfinityFlag getInfinity() {
		return infinity;
	}

	/**
	 * Returns the parity flag.
	 *
	 * @return the parity flag
	 */
	public ParityFlag getParity() {
		return parity;
	}

	/**
	 * Returns {@code true} if the over-approximation is a singleton interval.
	 *
	 * @return {@code true} iff the abstract value represents a single number
	 */
	public boolean isSingleton() {
		return !over.isTop() && !over.isBottom() && over.isSingleton();
	}

	/**
	 * Returns a copy of this element clamped to the bounds of the given type.
	 * If the over-approximation extends beyond the type bounds, the
	 * under-approximation is discarded (clamping introduces imprecision).
	 *
	 * @param targetType the target type kind
	 *
	 * @return the clamped interval
	 */
	public JavaFlaggedInterval clampToType(
			JavaTypeKind targetType) {
		if (!targetType.isInteger() || targetType == JavaTypeKind.UNKNOWN || over.isTop() || over.isBottom())
			return withType(targetType);
		MathNumber lo = new MathNumber(targetType.minValueLong());
		MathNumber hi = new MathNumber(targetType.maxValueLong());
		boolean needsClamp = over.getLow().compareTo(lo) < 0 || over.getHigh().compareTo(hi) > 0;
		if (!needsClamp)
			return withType(targetType);
		// Overflow can occur: Java wrapping means any value in [lo, hi] is
		// reachable. Return the full type range as a sound over-approximation.
		return new JavaFlaggedInterval(new IntInterval(lo, hi), null, targetType, provenance, integrality, nan,
				infinity, parity);
	}

	/**
	 * Returns a copy of this element with the type changed to
	 * {@code targetType}.
	 *
	 * @param targetType the new type kind
	 *
	 * @return the updated interval
	 */
	public JavaFlaggedInterval withType(
			JavaTypeKind targetType) {
		if (targetType == type)
			return this;
		return new JavaFlaggedInterval(over, under, targetType, provenance, integrality, nan, infinity, parity);
	}

	/**
	 * Returns a copy of this element with the under-approximation cleared (set
	 * to ⊥).
	 *
	 * @return the updated interval
	 */
	public JavaFlaggedInterval withNoUnder() {
		if (under == null)
			return this;
		return new JavaFlaggedInterval(over, null, type, provenance, integrality, nan, infinity, parity);
	}

	// -------------------------------------------------------------------------
	// BaseLattice implementation
	// -------------------------------------------------------------------------

	@Override
	public boolean isTop() {
		return over.isTop()
				&& under == null
				&& type == JavaTypeKind.UNKNOWN
				&& provenance.isTop()
				&& integrality.isTop()
				&& nan.isTop()
				&& infinity.isTop()
				&& parity.isTop();
	}

	@Override
	public boolean isBottom() {
		return over.isBottom();
	}

	@Override
	public JavaFlaggedInterval top() {
		return TOP;
	}

	@Override
	public JavaFlaggedInterval bottom() {
		return BOTTOM;
	}

	@Override
	public JavaFlaggedInterval lubAux(
			JavaFlaggedInterval other)
			throws SemanticException {
		IntInterval newOver = over.lub(other.over);
		// Under-approximation: intersection (only values reachable on BOTH
		// paths)
		IntInterval newUnder = intersectUnder(under, other.under);
		JavaTypeKind newType = type == other.type ? type : JavaTypeKind.UNKNOWN;
		return new JavaFlaggedInterval(
				newOver,
				newUnder,
				newType,
				provenance.lub(other.provenance),
				integrality.lub(other.integrality),
				nan.lub(other.nan),
				infinity.lub(other.infinity),
				parity.lub(other.parity));
	}

	@Override
	public JavaFlaggedInterval glbAux(
			JavaFlaggedInterval other)
			throws SemanticException {
		IntInterval newOver = over.glb(other.over);
		if (newOver.isBottom())
			return BOTTOM;
		// Under-approximation: hull (union of values reachable on either path)
		IntInterval newUnder = hullUnder(under, other.under);
		JavaTypeKind newType = type == other.type ? type : JavaTypeKind.UNKNOWN;
		return new JavaFlaggedInterval(
				newOver,
				newUnder,
				newType,
				provenance.glb(other.provenance),
				integrality.glb(other.integrality),
				nan.glb(other.nan),
				infinity.glb(other.infinity),
				parity.glb(other.parity));
	}

	@Override
	public JavaFlaggedInterval wideningAux(
			JavaFlaggedInterval other)
			throws SemanticException {
		// Widening always destroys the under-approximation
		IntInterval newOver = over.widening(other.over);
		JavaTypeKind newType = type == other.type ? type : JavaTypeKind.UNKNOWN;
		return new JavaFlaggedInterval(
				newOver,
				null,
				newType,
				provenance.widening(other.provenance),
				integrality.widening(other.integrality),
				nan.widening(other.nan),
				infinity.widening(other.infinity),
				parity.widening(other.parity));
	}

	@Override
	public JavaFlaggedInterval narrowingAux(
			JavaFlaggedInterval other)
			throws SemanticException {
		IntInterval newOver = over.narrowing(other.over);
		return new JavaFlaggedInterval(
				newOver,
				under,
				type,
				provenance,
				integrality,
				nan,
				infinity,
				parity);
	}

	@Override
	public boolean lessOrEqualAux(
			JavaFlaggedInterval other)
			throws SemanticException {
		if (!over.lessOrEqual(other.over))
			return false;
		// Under: this.under ⊇ other.under (reversed order)
		// if other.under == null (⊥) → condition trivially satisfied
		// if this.under == null but other.under != null → false
		if (other.under != null) {
			if (under == null)
				return false;
			if (!other.under.lessOrEqual(under))
				return false;
		}
		if (!provenance.lessOrEqual(other.provenance))
			return false;
		if (!integrality.lessOrEqual(other.integrality))
			return false;
		if (!nan.lessOrEqual(other.nan))
			return false;
		if (!infinity.lessOrEqual(other.infinity))
			return false;
		if (!parity.lessOrEqual(other.parity))
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		if (isBottom())
			return Lattice.bottomRepresentation();
		Map<StructuredRepresentation, StructuredRepresentation> map = new LinkedHashMap<>();
		map.put(new StringRepresentation("over"), over.representation());
		if (under != null)
			map.put(new StringRepresentation("under"), under.representation());
		map.put(new StringRepresentation("type"), new StringRepresentation(type.name()));
		if (!provenance.isBottom())
			map.put(new StringRepresentation("prov"), new StringRepresentation(provenance.name()));
		if (!integrality.isBottom())
			map.put(new StringRepresentation("integ"), new StringRepresentation(integrality.name()));
		if (!nan.isBottom())
			map.put(new StringRepresentation("nan"), new StringRepresentation(nan.name()));
		if (!infinity.isBottom())
			map.put(new StringRepresentation("inf"), new StringRepresentation(infinity.name()));
		if (!parity.isBottom())
			map.put(new StringRepresentation("par"), new StringRepresentation(parity.name()));
		return new MapRepresentation(map);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Returns the interval intersection of two under-approximations. If either
	 * is {@code null} (⊥), the result is {@code null}.
	 *
	 * @param a the first under-approximation
	 * @param b the second under-approximation
	 *
	 * @return their intersection, or {@code null}
	 *
	 * @throws SemanticException if LiSA's glb throws
	 */
	private static IntInterval intersectUnder(
			IntInterval a,
			IntInterval b)
			throws SemanticException {
		if (a == null || b == null)
			return null;
		IntInterval glb = a.glb(b);
		return glb.isBottom() ? null : glb;
	}

	/**
	 * Returns the interval hull (union) of two under-approximations. If both
	 * are {@code null}, the result is {@code null}. Otherwise the non-null one
	 * (or the hull of both) is returned.
	 *
	 * @param a the first under-approximation
	 * @param b the second under-approximation
	 *
	 * @return their hull, or {@code null} if both are {@code null}
	 *
	 * @throws SemanticException if LiSA's lub throws
	 */
	private static IntInterval hullUnder(
			IntInterval a,
			IntInterval b)
			throws SemanticException {
		if (a == null && b == null)
			return null;
		if (a == null)
			return b;
		if (b == null)
			return a;
		return a.lub(b);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof JavaFlaggedInterval other))
			return false;
		return Objects.equals(over, other.over)
				&& Objects.equals(under, other.under)
				&& type == other.type
				&& provenance == other.provenance
				&& integrality == other.integrality
				&& nan == other.nan
				&& infinity == other.infinity
				&& parity == other.parity;
	}

	@Override
	public int hashCode() {
		return Objects.hash(over, under, type, provenance, integrality, nan, infinity, parity);
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}

package it.unive.jlisa.lattices.heap.allocations;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.heap.allocations.AllocationSite;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;

/**
 * A heap allocation site, that is an allocation site pointing to something
 * allocated in the dynamic memory, the heap.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class JlisaHeapAllocationSite
		extends
		AllocationSite {

	/**
	 * Builds a heap allocation site.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param isWeak       if this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public JlisaHeapAllocationSite(
			Type staticType,
			String locationName,
			boolean isWeak,
			CodeLocation location) {
		this(staticType, locationName, (String) null, isWeak, location);
	}

	/**
	 * Builds a heap allocation site.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param isWeak       if this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public JlisaHeapAllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, isWeak, location);
	}

	/**
	 * Builds a heap allocation site.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the name of the field of this allocation site
	 * @param isWeak       if this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public JlisaHeapAllocationSite(
			Type staticType,
			String locationName,
			String field,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, isWeak, location);
	}

	@Override
	public JlisaHeapAllocationSite toWeak() {
		return isWeak() ? this
				: new JlisaHeapAllocationSite(getStaticType(), getLocationName(), getField(), true, getCodeLocation());
	}

	@Override
	public JlisaHeapAllocationSite withField(
			SymbolicExpression field) {
		if (getField() != null)
			throw new IllegalStateException("Cannot add a field to an allocation site that already has one");
		return new JlisaHeapAllocationSite(getStaticType(), getLocationName(), field, isWeak(), getCodeLocation());
	}
	
	@Override
	public Identifier lub(
			Identifier other)
			throws SemanticException {
		if (!getName().equals(other.getName()))
			throw new SemanticException(
					"Cannot perform the least upper bound between different identifiers: '"
							+ this
							+ "' and '"
							+ other
							+ "'");
		return other;
	}

	@Override
	public JlisaHeapAllocationSite withType(
			Type type) {
		return new JlisaHeapAllocationSite(type, getLocationName(), getField(), isWeak(), getCodeLocation());
	}

	@Override
	public JlisaHeapAllocationSite withoutField() {
		if (getField() == null)
			return this;
		return new JlisaHeapAllocationSite(getStaticType(), getLocationName(), isWeak(), getCodeLocation());
	}

	@Override
	public JlisaHeapAllocationSite asNonAllocation() {
		if (!isAllocation())
			return this;
		return new JlisaHeapAllocationSite(getStaticType(), getLocationName(), isWeak(), getCodeLocation());
	}

}

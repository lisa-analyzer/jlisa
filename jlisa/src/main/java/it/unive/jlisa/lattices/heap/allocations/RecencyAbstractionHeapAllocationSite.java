package it.unive.jlisa.lattices.heap.allocations;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.heap.allocations.AllocationSite;
import it.unive.lisa.lattices.heap.allocations.HeapAllocationSite;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;

public class RecencyAbstractionHeapAllocationSite extends AllocationSite{

	private AllocationSite recent;
	private AllocationSite summary;
	
	
	public RecencyAbstractionHeapAllocationSite(
			Type staticType,
			String locationName,
			boolean isWeak,
			CodeLocation location) {
		this(staticType, locationName, (String) null, isWeak, location);
	}
	
	public RecencyAbstractionHeapAllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, isWeak, location);
		recent = new HeapAllocationSite(staticType, locationName, field, false, location);
		summary = new HeapAllocationSite(staticType, locationName, field, true, location);
	}
	
	public RecencyAbstractionHeapAllocationSite(
			Type staticType,
			String locationName,
			String field,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, isWeak, location);
		recent = new HeapAllocationSite(staticType, locationName, field, false, location);
		summary = new HeapAllocationSite(staticType, locationName, field, true, location);
	}
	
	public AllocationSite getRecent() {
		return recent;
	}
	
	public AllocationSite getSummary() {
		return summary;
	}
	
	public RecencyAbstractionHeapAllocationSite lubWeak(
			Type staticType,
			String locationName,
			CodeLocation location) throws SemanticException {
		summary = (HeapAllocationSite) summary.lub(recent);
		recent = new HeapAllocationSite(staticType, locationName ,false, location);
		
		return this;
	}

	@Override
	public AllocationSite toWeak() {
		return isWeak() ? this
				: new RecencyAbstractionHeapAllocationSite(getStaticType(), getLocationName(), getField(), true, getCodeLocation());
	}

	@Override
	public AllocationSite withField(SymbolicExpression field) {
		if (getField() != null)
			throw new IllegalStateException("Cannot add a field to an allocation site that already has one");
		return new RecencyAbstractionHeapAllocationSite(getStaticType(), getLocationName(), field, isWeak(), getCodeLocation());

	}

	@Override
	public AllocationSite withoutField() {
		if (getField() == null)
			return this;
		return new RecencyAbstractionHeapAllocationSite(getStaticType(), getLocationName(), isWeak(), getCodeLocation());

	}

	@Override
	public AllocationSite withType(Type type) {
		return new RecencyAbstractionHeapAllocationSite(type, getLocationName(), getField(), isWeak(), getCodeLocation());
	}
	
	@Override
	public HeapLocation asNonAllocation() {
		if (!isAllocation())
			return this;
		return new RecencyAbstractionHeapAllocationSite(getStaticType(), getLocationName(), isWeak(), getCodeLocation());

	}
	
	@Override
	public RecencyAbstractionHeapAllocationSite lub(Identifier other) throws SemanticException {
		RecencyAbstractionHeapAllocationSite aux;
		if(other instanceof RecencyAbstractionHeapAllocationSite) {
			aux = (RecencyAbstractionHeapAllocationSite) other;
		} else {
			throw new RuntimeException("Wrong type of other, cannot calculate lub!");
		}
		
//		if(this.isWeak() || other.isWeak()) {
//			recent = (AllocationSite) recent.lub(aux.getRecent());
//			summary = (AllocationSite) summary.lub(aux.getSummary());
//			return (RecencyAbstractionHeapAllocationSite) toWeak();
//		}
		
		recent = (AllocationSite) recent.lub(aux.getRecent());
		summary = (AllocationSite) summary.lub(aux.getSummary());
		
		return this;
	}
	
}

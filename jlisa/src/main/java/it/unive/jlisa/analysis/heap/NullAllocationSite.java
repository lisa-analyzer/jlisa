package it.unive.jlisa.analysis.heap;

import it.unive.jlisa.program.type.JavaNullType;
import it.unive.lisa.lattices.heap.allocations.HeapAllocationSite;
import it.unive.lisa.program.SourceCodeLocation;

public class NullAllocationSite extends HeapAllocationSite {
	
	public static NullAllocationSite INSTANCE = new NullAllocationSite();
	
	private NullAllocationSite() {
		// FIXME: better synthetic code location
		super(JavaNullType.INSTANCE, "null", false, new SourceCodeLocation("", 0, 0));
	}
	
	@Override
	public String toString() {
		return "null";
	}
}

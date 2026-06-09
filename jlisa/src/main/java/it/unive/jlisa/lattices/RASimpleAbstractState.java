package it.unive.jlisa.lattices;

import it.unive.jlisa.lattices.heap.allocations.RecencyAbstractionHeapAllocationSite;
import it.unive.jlisa.lattices.heap.allocations.RecencyAbstractionHeapEnvWithFields;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.lattices.SingleHeapLattice;
import it.unive.lisa.lattices.SingleTypeLattice;
import it.unive.lisa.lattices.SingleValueLattice;
import it.unive.lisa.lattices.heap.allocations.AllocationSite;
import it.unive.lisa.lattices.heap.allocations.AllocationSites;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An abstract state of the analysis, composed by a heap state modeling the
 * memory layout, a value state modeling values of program variables and memory
 * locations, and a type state that can give types to expressions knowing the
 * ones of variables.<br>
 * <br>
 * The interaction between heap and value/type states follows the one defined
 * <a href=
 * "https://www.sciencedirect.com/science/article/pii/S0304397516300299">in this
 * paper</a>.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <H> the type of {@link HeapLattice} embedded in this state
 * @param <V> the type of {@link ValueLattice} embedded in this state
 * @param <T> the type of {@link TypeLattice} embedded in this state
 */
public class RASimpleAbstractState<
		H extends HeapLattice<H>,
		V extends ValueLattice<V>,
		T extends TypeLattice<T>>
		implements
		BaseLattice<RASimpleAbstractState<H, V, T>>,
		AbstractLattice<RASimpleAbstractState<H, V, T>> {

	/**
	 * The key that should be used to store the instance of {@link HeapDomain}
	 * inside the {@link StructuredRepresentation} returned by
	 * {@link #representation()}.
	 */
	public static final String HEAP_REPRESENTATION_KEY = "heap";

	/**
	 * The key that should be used to store the instance of {@link TypeDomain}
	 * inside the {@link StructuredRepresentation} returned by
	 * {@link #representation()}.
	 */
	public static final String TYPE_REPRESENTATION_KEY = "type";

	/**
	 * The key that should be used to store the instance of {@link ValueDomain}
	 * inside the {@link StructuredRepresentation} returned by
	 * {@link #representation()}.
	 */
	public static final String VALUE_REPRESENTATION_KEY = "value";

	/**
	 * The state containing information regarding memory structures.
	 */
	public final H heapState;

	/**
	 * The state containing information regarding values of program variables
	 * and concretized memory locations.
	 */
	public final V valueState;

	/**
	 * The state containing runtime types information regarding runtime types of
	 * program variables and concretized memory locations.
	 */
	public final T typeState;

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleHeapLattice}, {@link SingleValueLattice},
	 * and {@link SingleTypeLattice}).
	 * 
	 * @param heapState the state containing information regarding heap
	 *                      structures
	 */
	@SuppressWarnings("unchecked")
	public RASimpleAbstractState(
			H heapState) {
		this.heapState = heapState;
		this.valueState = (V) SingleValueLattice.SINGLETON;
		this.typeState = (T) SingleTypeLattice.SINGLETON;
	}

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleHeapLattice}, {@link SingleValueLattice},
	 * and {@link SingleTypeLattice}).
	 * 
	 * @param valueState the state containing information regarding values of
	 *                       program variables and concretized memory locations
	 */
	@SuppressWarnings("unchecked")
	public RASimpleAbstractState(
			V valueState) {
		this.heapState = (H) SingleHeapLattice.SINGLETON;
		this.valueState = valueState;
		this.typeState = (T) SingleTypeLattice.SINGLETON;
	}

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleHeapLattice}, {@link SingleValueLattice},
	 * and {@link SingleTypeLattice}).
	 * 
	 * @param typeState the state containing information regarding runtime types
	 *                      of program variables and concretized memory
	 *                      locations
	 */
	@SuppressWarnings("unchecked")
	public RASimpleAbstractState(
			T typeState) {
		this.heapState = (H) SingleHeapLattice.SINGLETON;
		this.valueState = (V) SingleValueLattice.SINGLETON;
		this.typeState = typeState;
	}

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleHeapLattice}, {@link SingleValueLattice},
	 * and {@link SingleTypeLattice}).
	 * 
	 * @param heapState  the state containing information regarding heap
	 *                       structures
	 * @param valueState the state containing information regarding values of
	 *                       program variables and concretized memory locations
	 */
	@SuppressWarnings("unchecked")
	public RASimpleAbstractState(
			H heapState,
			V valueState) {
		this.heapState = heapState;
		this.valueState = valueState;
		this.typeState = (T) SingleTypeLattice.SINGLETON;
	}

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleHeapLattice}, {@link SingleValueLattice},
	 * and {@link SingleTypeLattice}).
	 * 
	 * @param heapState the state containing information regarding heap
	 *                      structures
	 * @param typeState the state containing information regarding runtime types
	 *                      of program variables and concretized memory
	 *                      locations
	 */
	@SuppressWarnings("unchecked")
	public RASimpleAbstractState(
			H heapState,
			T typeState) {
		this.heapState = heapState;
		this.valueState = (V) SingleValueLattice.SINGLETON;
		this.typeState = typeState;
	}

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleHeapLattice}, {@link SingleValueLattice},
	 * and {@link SingleTypeLattice}).
	 * 
	 * @param valueState the state containing information regarding values of
	 *                       program variables and concretized memory locations
	 * @param typeState  the state containing information regarding runtime
	 *                       types of program variables and concretized memory
	 *                       locations
	 */
	@SuppressWarnings("unchecked")
	public RASimpleAbstractState(
			V valueState,
			T typeState) {
		this.heapState = (H) SingleHeapLattice.SINGLETON;
		this.valueState = valueState;
		this.typeState = typeState;
	}

	/**
	 * Builds a new abstract state.
	 * 
	 * @param heapState  the state containing information regarding heap
	 *                       structures
	 * @param valueState the state containing information regarding values of
	 *                       program variables and concretized memory locations
	 * @param typeState  the state containing information regarding runtime
	 *                       types of program variables and concretized memory
	 *                       locations
	 */
	public RASimpleAbstractState(
			H heapState,
			V valueState,
			T typeState) {
		this.heapState = heapState;
		this.valueState = valueState;
		this.typeState = typeState;
	}

	/**
	 * Builds a new abstract state with the information contained in the given
	 * oracle.
	 * 
	 * @param mo the oracle containing the components for the state to be
	 *               created
	 */
	public RASimpleAbstractState(
			SimpleAbstractDomain<H, V, T>.MutableOracle mo) {
		this(mo.heap, mo.value, mo.type);
	}

	public RASimpleAbstractState(
			it.unive.jlisa.analysis.RASimpleAbstractDomain<H, V, T>.MutableOracle mo) {
		this(mo.heap, mo.value, mo.type);	
	}

	private RASimpleAbstractState<H, V, T> applySubstitution(
			List<HeapReplacement> subs,
			ProgramPoint pp)
			throws SemanticException {
		T t = typeState;
		V v = valueState;
		if (subs != null)
			for (HeapReplacement repl : subs) {
				t = t.applyReplacement(repl, pp);
				v = v.applyReplacement(repl, pp);
			}
		return new RASimpleAbstractState<>(heapState, v, t);
	}

	@Override
	public RASimpleAbstractState<H, V, T> pushScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		// it should not be necessary to apply substitutions here,
		// as we are not deleting variables and the heap locations
		// won't be masked by the scope
		return new RASimpleAbstractState<>(
				heapState.pushScope(scope, pp).getLeft(),
				valueState.pushScope(scope, pp),
				typeState.pushScope(scope, pp));
	}

	@Override
	public RASimpleAbstractState<H, V, T> popScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		Pair<H, List<HeapReplacement>> heap = heapState.popScope(scope, pp);
		RASimpleAbstractState<H, V, T> subs = applySubstitution(heap.getRight(), pp);
		V v = subs.valueState.popScope(scope, pp);
		T t = subs.typeState.popScope(scope, pp);
		return new RASimpleAbstractState<>(heap.getLeft(), v, t);
	}

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public RASimpleAbstractState<H, V, T> lubAux(
			RASimpleAbstractState<H, V, T> other)
			throws SemanticException {
		H postHeap = heapState.lub(other.heapState);
		V postValue = valueState.lub(other.valueState);
		T postType = typeState.lub(other.typeState);
		
		if( postHeap instanceof RecencyAbstractionHeapEnvWithFields) {
			RecencyAbstractionHeapEnvWithFields pHeap = (RecencyAbstractionHeapEnvWithFields) postHeap;
			RecencyAbstractionHeapEnvWithFields tHeap = (RecencyAbstractionHeapEnvWithFields) heapState;
			RecencyAbstractionHeapEnvWithFields oHeap = (RecencyAbstractionHeapEnvWithFields) other.heapState;

			List<HeapReplacement> replacements = new LinkedList<>();

			Collection<AllocationSites> s = tHeap.function.values();
			
			// Checks if there are changes in heap identifiers of this state
			for(AllocationSites sites : s) {
				for(AllocationSite val : sites) {
					if(!pHeap.function.containsValue(val)) {
						if(val instanceof RecencyAbstractionHeapAllocationSite) {
							RecencyAbstractionHeapAllocationSite site = (RecencyAbstractionHeapAllocationSite) val;
							for (SymbolicExpression field : pHeap.fields.getState(site.getRecent())) {
								AllocationSite withField = site.withField(field);
								if (!withField.isWeak()) {
									HeapReplacement replacement = new HeapReplacement();
									replacement.addSource(withField);
									replacement.addTarget(withField.toWeak());
									replacements.add(replacement);
								}
							}
						} 
					}
				}
			}

			s = oHeap.function.values();
			
			// Checks if there are changes in heap identifiers of this state
			for(AllocationSites sites : s) {
				for(AllocationSite val : sites) {
					if(!pHeap.function.containsValue(val)) {
						if(val instanceof RecencyAbstractionHeapAllocationSite) {
							RecencyAbstractionHeapAllocationSite site = (RecencyAbstractionHeapAllocationSite) val;
							for (SymbolicExpression field : pHeap.fields.getState(site.getRecent())) {
								AllocationSite withField = site.withField(field);
								if (!withField.isWeak()) {
									HeapReplacement replacement = new HeapReplacement();
									replacement.addSource(withField);
									replacement.addTarget(withField.toWeak());
									replacements.add(replacement);
								}
							}
						} 
					}
				}
			}
			
			V aux = postValue;
			
			for(HeapReplacement repl : replacements) {
				aux = aux.applyReplacement(repl, null);
				postType = postType.applyReplacement(repl, null);
			}
			
			postValue = postValue.lub(aux);
			
			for(HeapReplacement repl : replacements) {
				for(Identifier id : repl.getSources()) {
					postValue = postValue.forgetIdentifier(id, null);
				}
			}
		}
		
		return new RASimpleAbstractState<>(
				postHeap,
				postValue,
				postType);
	}

	@Override
	public RASimpleAbstractState<H, V, T> upchainAux(
			RASimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return new RASimpleAbstractState<>(
				heapState.upchain(other.heapState),
				valueState.upchain(other.valueState),
				typeState.upchain(other.typeState));
	}

	@Override
	public RASimpleAbstractState<H, V, T> glbAux(
			RASimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return new RASimpleAbstractState<>(
				heapState.glb(other.heapState),
				valueState.glb(other.valueState),
				typeState.glb(other.typeState));
	}

	@Override
	public RASimpleAbstractState<H, V, T> downchainAux(
			RASimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return new RASimpleAbstractState<>(
				heapState.downchain(other.heapState),
				valueState.downchain(other.valueState),
				typeState.downchain(other.typeState));
	}

	@Override
	public RASimpleAbstractState<H, V, T> wideningAux(
			RASimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return new RASimpleAbstractState<>(
				heapState.widening(other.heapState),
				valueState.widening(other.valueState),
				typeState.widening(other.typeState));
	}

	@Override
	public RASimpleAbstractState<H, V, T> narrowingAux(
			RASimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return new RASimpleAbstractState<>(
				heapState.narrowing(other.heapState),
				valueState.narrowing(other.valueState),
				typeState.narrowing(other.typeState));
	}

	@Override
	public boolean lessOrEqualAux(
			RASimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return heapState.lessOrEqual(other.heapState)
				&& valueState.lessOrEqual(other.valueState)
				&& typeState.lessOrEqual(other.typeState);
	}

	@Override
	public RASimpleAbstractState<H, V, T> top() {
		return new RASimpleAbstractState<>(heapState.top(), valueState.top(), typeState.top());
	}

	@Override
	public RASimpleAbstractState<H, V, T> bottom() {
		return new RASimpleAbstractState<>(heapState.bottom(), valueState.bottom(), typeState.bottom());
	}

	@Override
	public boolean isTop() {
		return heapState.isTop() && valueState.isTop() && typeState.isTop();
	}

	@Override
	public boolean isBottom() {
		return heapState.isBottom() && valueState.isBottom() && typeState.isBottom();
	}

	@Override
	public RASimpleAbstractState<H, V, T> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		Pair<H, List<HeapReplacement>> heap = heapState.forgetIdentifier(id, pp);
		RASimpleAbstractState<H, V, T> subs = applySubstitution(heap.getRight(), pp);
		V v = subs.valueState.forgetIdentifier(id, pp);
		T t = subs.typeState.forgetIdentifier(id, pp);
		return new RASimpleAbstractState<>(heap.getLeft(), v, t);
	}

	@Override
	public RASimpleAbstractState<H, V, T> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		Pair<H, List<HeapReplacement>> heap = heapState.forgetIdentifiers(ids, pp);
		RASimpleAbstractState<H, V, T> subs = applySubstitution(heap.getRight(), pp);
		V v = subs.valueState.forgetIdentifiers(ids, pp);
		T t = subs.typeState.forgetIdentifiers(ids, pp);
		return new RASimpleAbstractState<>(heap.getLeft(), v, t);
	}

	@Override
	public RASimpleAbstractState<H, V, T> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		Pair<H, List<HeapReplacement>> heap = heapState.forgetIdentifiersIf(test, pp);
		RASimpleAbstractState<H, V, T> subs = applySubstitution(heap.getRight(), pp);
		V v = subs.valueState.forgetIdentifiersIf(test, pp);
		T t = subs.typeState.forgetIdentifiersIf(test, pp);
		return new RASimpleAbstractState<>(heap.getLeft(), v, t);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((heapState == null) ? 0 : heapState.hashCode());
		result = prime * result + ((valueState == null) ? 0 : valueState.hashCode());
		result = prime * result + ((typeState == null) ? 0 : typeState.hashCode());
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
		RASimpleAbstractState<?, ?, ?> other = (RASimpleAbstractState<?, ?, ?>) obj;
		if (heapState == null) {
			if (other.heapState != null)
				return false;
		} else if (!heapState.equals(other.heapState))
			return false;
		if (valueState == null) {
			if (other.valueState != null)
				return false;
		} else if (!valueState.equals(other.valueState))
			return false;
		if (typeState == null) {
			if (other.typeState != null)
				return false;
		} else if (!typeState.equals(other.typeState))
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		StructuredRepresentation h = heapState.representation();
		StructuredRepresentation t = typeState.representation();
		StructuredRepresentation v = valueState.representation();
		return new ObjectRepresentation(
				Map.of(HEAP_REPRESENTATION_KEY, h, TYPE_REPRESENTATION_KEY, t, VALUE_REPRESENTATION_KEY, v));
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return heapState.knowsIdentifier(id) || valueState.knowsIdentifier(id) || typeState.knowsIdentifier(id);
	}

	@Override
	public RASimpleAbstractState<H, V, T> withTopMemory() {
		return new RASimpleAbstractState<>(heapState.top(), valueState, typeState);
	}

	@Override
	public RASimpleAbstractState<H, V, T> withTopValues() {
		return new RASimpleAbstractState<>(heapState, valueState.top(), typeState);
	}

	@Override
	public RASimpleAbstractState<H, V, T> withTopTypes() {
		return new RASimpleAbstractState<>(heapState, valueState, typeState.top());
	}

	@Override
	public <D extends Lattice<D>> Collection<D> getAllLatticeInstances(
			Class<D> lattice) {
		Collection<D> result = AbstractLattice.super.getAllLatticeInstances(lattice);
		result.addAll(heapState.getAllLatticeInstances(lattice));
		result.addAll(typeState.getAllLatticeInstances(lattice));
		result.addAll(valueState.getAllLatticeInstances(lattice));
		return result;
	}

}

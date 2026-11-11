package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.java.constructs.lists.ListModel;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;

/**
 * A view of the keys or of the values of a map (e.g., {@code keySet()}): a new
 * collection with unknown elements, whose number is bounded by the size of the
 * map ({@link MapPut} counts a key that is already in the map again, so the
 * view has between 1 and {@code size} elements for a map that is not empty).
 */
public class MapView extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	/**
	 * The location of the allocation of the view.
	 */
	private final CodeLocation viewLocation;

	/**
	 * The location of the allocation of the array of the view.
	 */
	private final CodeLocation elementDataLocation;

	/**
	 * The location of the allocation of the entries, if the view holds the
	 * entries of the map ({@code null} otherwise).
	 */
	private final CodeLocation entryLocation;

	public MapView(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		this(cfg, location, expr, false);
	}

	/**
	 * Builds the view.
	 *
	 * @param cfg      the cfg
	 * @param location the location
	 * @param expr     the map
	 * @param entries  whether the view holds the entries of the map
	 */
	protected MapView(
			CFG cfg,
			CodeLocation location,
			Expression expr,
			boolean entries) {
		super(cfg, location, "view", expr);
		SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("map-view-" + location.toString());
		viewLocation = synGen.nextLocation();
		elementDataLocation = synGen.nextLocation();
		entryLocation = entries ? synGen.nextLocation() : null;
	}

	public static MapView build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapView(cfg, location, params[0]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();
		Type viewType = originating instanceof Expression e ? e.getStaticType() : Untyped.INSTANCE;
		AccessChild mapSize = MapModel.size(expr, location);

		// a map that is not modeled gives a view that is not modeled either
		boolean known = false;
		for (SymbolicExpression e : analysis.rewrite(state, mapSize, context, this))
			if (e instanceof Identifier id && state.knowsIdentifier(id))
				known = true;
		if (!known || !viewType.isPointerType())
			return analysis.smallStepSemantics(state, new PushAny(viewType, location), context, this);

		// a new collection, as big as the map
		Type inner = viewType.asPointerType().getInnerType();
		MemoryAllocation created = new MemoryAllocation(inner, viewLocation, false);
		HeapReference ref = new HeapReference(viewType, created, location);
		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, context, this);
		InstrumentedReceiver view = new InstrumentedReceiver(viewType, true, location);
		AnalysisState<A> empty = analysis.assign(allocated, view, ref, context, this);
		empty = ListModel.init(analysis, context, empty, view, elementDataLocation, this);
		AccessChild size = ListModel.size(view, location);
		AnalysisState<A> result = empty.bottomExecution();
		for (int k = 0; k <= ListModel.MAX_TRACKED_ELEMENTS; k++) {
			boolean beyond = k == ListModel.MAX_TRACKED_ELEMENTS;
			AnalysisState<A> before = analysis.assume(empty,
					new BinaryExpression(JavaBooleanType.INSTANCE, mapSize,
							new Constant(JavaIntType.INSTANCE, k, location),
							beyond ? ComparisonGe.INSTANCE : ComparisonEq.INSTANCE, location),
					context, this, this);
			if (before.getExecutionState().isBottom())
				continue;
			SymbolicExpression value;
			if (k == 0)
				value = new Constant(JavaIntType.INSTANCE, 0, location);
			else if (beyond)
				value = new PushFromConstraints(JavaIntType.INSTANCE, location,
						Set.of(new BinaryExpression(JavaBooleanType.INSTANCE,
								new Constant(JavaIntType.INSTANCE, 1, location), size, ComparisonLe.INSTANCE,
								location)));
			else
				value = ListModel.between(size, 1, k, location);
			result = result.lub(analysis.assign(before, size, value, context, this));
		}
		if (entryLocation != null)
			result = addEntries(analysis, context, result, view, location);
		getMetaVariables().add(view);
		return analysis.smallStepSemantics(result, view, context, this);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> addEntries(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression view,
			CodeLocation location)
			throws SemanticException {
		// all the elements are the same entry, with an unknown key and value
		LibrarySpecificationProvider.importClass(getProgram(), "java.util.Map.Entry");
		JavaClassType entryType = JavaClassType.lookup("java.util.Map.Entry");
		MemoryAllocation created = new MemoryAllocation(entryType, entryLocation, false);
		HeapReference ref = new HeapReference(entryType.getReference(), created, location);
		AnalysisState<A> tmp = analysis.smallStepSemantics(state, created, context, this);
		// a variable of its own (the one of the view has the location of the
		// call)
		InstrumentedReceiver entry = new InstrumentedReceiver(entryType.getReference(), true, entryLocation);
		tmp = analysis.assign(tmp, entry, ref, context, this);
		PushAny any = new PushAny(new JavaReferenceType(JavaClassType.getObjectType()), location);
		tmp = analysis.assign(tmp, MapEntryGet.field(entry, "$key", location), any, context, this);
		tmp = analysis.assign(tmp, MapEntryGet.field(entry, "$value", location), any, context, this);
		for (int i = 0; i < ListModel.MAX_TRACKED_ELEMENTS; i++) {
			AnalysisState<A> cell = analysis.assume(tmp,
					ListModel.compareSize(view, ComparisonGt.INSTANCE, i, location), context, this, this);
			if (cell.getExecutionState().isBottom())
				break;
			AnalysisState<A> other = analysis.assume(tmp,
					ListModel.compareSize(view, ComparisonLe.INSTANCE, i, location), context, this, this);
			tmp = analysis.assign(cell, ListModel.element(view, i, location), entry, context, this).lub(other);
		}
		return tmp.forgetIdentifier(entry, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

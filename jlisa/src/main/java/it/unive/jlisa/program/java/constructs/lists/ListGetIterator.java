package it.unive.jlisa.program.java.constructs.lists;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
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
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.PushAny;

public class ListGetIterator
		extends
		it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	/**
	 * The location of the allocation of the iterator.
	 */
	private final CodeLocation iteratorLocation;

	public ListGetIterator(
			CFG cfg,
			CodeLocation location,
			Expression rec) {
		super(cfg, location, "iterator", rec);
		iteratorLocation = new SyntheticCodeLocationManager("list-iterator-" + location.toString()).nextLocation();
	}

	public static ListGetIterator build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListGetIterator(cfg, location, params[0]);
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
		JavaClassType iteratorType = JavaClassType.lookup("java.util.Iterator");
		JavaReferenceType iteratorRef = iteratorType.getReference();
		if (!ListModel.isModeled(analysis, context, state, expr, this))
			return analysis.smallStepSemantics(state, new PushAny(iteratorRef, location), context, this);

		// a new iterator on the list, at its first element
		MemoryAllocation created = new MemoryAllocation(iteratorType, iteratorLocation, false);
		HeapReference ref = new HeapReference(iteratorRef, created, location);
		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, context, this);
		InstrumentedReceiver iterator = new InstrumentedReceiver(iteratorRef, true, location);
		AnalysisState<A> tmp = analysis.assign(allocated, iterator, ref, context, this);
		tmp = analysis.assign(tmp, ListModel.iteratedList(iterator, location), expr, context, this);
		tmp = analysis.assign(tmp, ListModel.cursor(iterator, location),
				new Constant(JavaIntType.INSTANCE, 0, location), context, this);
		getMetaVariables().add(iterator);
		return analysis.smallStepSemantics(tmp, iterator, context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

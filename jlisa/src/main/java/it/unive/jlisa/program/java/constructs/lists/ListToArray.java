package it.unive.jlisa.program.java.constructs.lists;

import it.unive.jlisa.program.type.JavaArrayType;
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
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.type.Type;
import java.util.Set;

public class ListToArray
		extends
		it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public ListToArray(
			CFG cfg,
			CodeLocation location,
			Expression arg) {
		super(cfg, location, "toArray", arg);
	}

	public static ListToArray build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListToArray(cfg, location, params[0]);
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
		JavaReferenceType refType = JavaArrayType.OBJECT_ARRAY;
		MemoryAllocation created = new MemoryAllocation(refType.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(refType, created, getLocation());

		InstrumentedReceiver array = new InstrumentedReceiver(refType, true, getLocation());
		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, context, this);
		AnalysisState<A> tmp = analysis.assign(allocated, array, ref, context, this);
		GlobalVariable lenProperty = new GlobalVariable(JavaIntType.INSTANCE, "length", getLocation());
		AccessChild lenAccess = new AccessChild(refType.getInnerType(), array, lenProperty, getLocation());
		getMetaVariables().add(array);
		if (ListModel.isModeled(analysis, context, tmp, expr, this))
			return copy(analysis, context, tmp, expr, array, lenAccess);
		PushFromConstraints length = new PushFromConstraints(JavaIntType.INSTANCE, getLocation(), Set.of(
				new BinaryExpression(
						JavaBooleanType.INSTANCE,
						new Constant(JavaIntType.INSTANCE, 0, getLocation()),
						lenAccess,
						ComparisonLe.INSTANCE,
						getLocation())));
		tmp = analysis.assign(tmp, lenAccess, length, context, this);
		return analysis.smallStepSemantics(tmp, array, context, this);
	}

	// the array has the elements of the list, one case for each size of the
	// list up to the tracked elements (as ListModel#copy): its length is the
	// size of the list, and its tracked cells are the tracked elements
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> copy(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression list,
			InstrumentedReceiver array,
			AccessChild lenAccess)
			throws SemanticException {
		CodeLocation location = getLocation();
		Type elementType = new JavaReferenceType(JavaClassType.getObjectType());
		AnalysisState<A> result = state.bottomExecution();
		for (int k = 0; k <= ListModel.MAX_TRACKED_ELEMENTS; k++) {
			boolean beyond = k == ListModel.MAX_TRACKED_ELEMENTS;
			AnalysisState<A> tmp = analysis.assume(state, ListModel.compareSize(list,
					beyond ? ComparisonGe.INSTANCE : ComparisonEq.INSTANCE, k, location), context, this, this);
			if (tmp.getExecutionState().isBottom())
				continue;
			for (int i = 0; i < k; i++)
				tmp = analysis.assign(tmp, new AccessChild(elementType, array,
						new Constant(JavaIntType.INSTANCE, i, location), location),
						ListModel.element(list, i, location), context, this);
			tmp = analysis.assign(tmp, lenAccess, beyond ? ListModel.size(list, location)
					: new Constant(JavaIntType.INSTANCE, k, location), context, this);
			result = result.lub(tmp);
		}
		return analysis.smallStepSemantics(result, array, context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

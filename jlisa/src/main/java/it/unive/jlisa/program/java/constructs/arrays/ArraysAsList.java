package it.unive.jlisa.program.java.constructs.arrays;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.java.constructs.lists.ListModel;
import it.unive.jlisa.program.type.JavaArrayType;
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
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;

public class ArraysAsList extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	/**
	 * The location of the allocation of the list.
	 */
	private final CodeLocation listLocation;

	/**
	 * The location of the allocation of the array of a list with a single
	 * element.
	 */
	private final CodeLocation elementDataLocation;

	public ArraysAsList(
			CFG cfg,
			CodeLocation location,
			Expression arg) {
		super(cfg, location, "asList", JavaClassType.lookup("java.util.List").getReference(), arg);
		SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("aslist-" + location.toString());
		listLocation = synGen.nextLocation();
		elementDataLocation = synGen.nextLocation();
	}

	public static ArraysAsList build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ArraysAsList(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
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
			SymbolicExpression arg,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();
		Type argType = getSubExpression().getStaticType();
		if (argType.isUntyped())
			return analysis.smallStepSemantics(state, new PushAny(getStaticType(), location), context, originating);

		JavaReferenceType listType = JavaClassType.lookup("java.util.List").getReference();
		AnalysisState<A> created = ListModel.create(analysis, context, state, listType, listLocation,
				elementDataLocation, ListModel.FIXED_SIZE, this);
		AnalysisState<A> result = created.bottomExecution();
		for (SymbolicExpression list : created.getExecutionExpressions())
			if (argType.isReferenceType() && argType.asReferenceType().getInnerType().isArrayType()
					&& argType.asReferenceType().getInnerType().asArrayType().getInnerType().isReferenceType()) {
				// the list is backed by the array of references: its elements
				// are the cells of the array, and its size is the length
				JavaArrayType arrayType = (JavaArrayType) argType.asReferenceType().getInnerType();
				AccessChild length = new AccessChild(JavaIntType.INSTANCE,
						new HeapDereference(arrayType, arg, location),
						new GlobalVariable(JavaIntType.INSTANCE, "length", location), location);
				AnalysisState<A> tmp = analysis.assign(created, ListModel.elementData(list, location), arg, context,
						this);
				tmp = analysis.assign(tmp, ListModel.size(list, location), length, context, this);
				result = result.lub(tmp.withExecutionExpression(list));
			} else
				// a single element (e.g., an array of primitive values)
				result = result.lub(
						ListModel.append(analysis, context, created, list, arg, this).withExecutionExpression(list));
		return result;
	}
}
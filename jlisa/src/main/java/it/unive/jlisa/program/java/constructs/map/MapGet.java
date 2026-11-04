package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.ConstraintUtils;
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
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class MapGet extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public MapGet(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "get", left, right);
	}

	public static MapGet build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapGet(cfg, location, params[0], params[1]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		Set<Type> keyTypes = analysis.getRuntimeTypesOf(state, right, context, this);

		Satisfiability isEmpty = checkZeroSize(interprocedural, state, left, expressions);
		if (isEmpty.isBottom())
			return state.bottomExecution();
		if (isEmpty == Satisfiability.SATISFIED) {
			return analysis.smallStepSemantics(state, new NullConstant(getLocation()), context, this);
		}

		AnalysisState<A> res = state.bottomExecution();

		if (keyTypes.size() == 1) {
			Type keyType = keyTypes.iterator().next();
			if (keyType instanceof JavaReferenceType jrt)
				keyType = jrt.getInnerType();

			if (keyType == JavaIntType.INSTANCE || keyType.isStringType()) {
				res = getKeyValue(interprocedural, state, left, right, keyType, expressions);
			}
			// not string nor int, return top as if open call
			else {
				res = topReturnValue(interprocedural, state, expressions);
			}
		}
		// definitely not constant key, return top as if open call
		else {
			res = topReturnValue(interprocedural, state, expressions);
		}
		return res;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> topReturnValue(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {
		ScopeId<A> context = interprocedural.getCurrentContext();
		AnalysisState<A> topState = interprocedural.getAnalysis().smallStepSemantics(state,
				new PushAny(new JavaReferenceType(JavaClassType.getObjectType()), getLocation()),
				context, originating);
		return topState;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> getKeyValue(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			Type keyType,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();
		Type mapType = JavaClassType.lookup("java.util.Map");

		HeapDereference derefMap = new HeapDereference(mapType, left, location);

		SymbolicExpression accessKeyArg = right;
		if (keyType.isStringType()) {
			HeapDereference derefKeyArg = new HeapDereference(keyType, right, location);
			GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", location);
			accessKeyArg = new AccessChild(keyType, derefKeyArg, var, location);
		}

		Stream<it.unive.lisa.symbolic.value.BinaryExpression> constraints = ConstraintUtils
				.extractConstraints(interprocedural, state, accessKeyArg, this);
		if (constraints == null)
			return state.bottomExecution();

		List<it.unive.lisa.symbolic.value.BinaryExpression> constraintsList = constraints.toList();
		if (constraintsList.isEmpty() || constraintsList.size() > 1) {
			return topReturnValue(interprocedural, state, expressions);
		}

		it.unive.lisa.symbolic.value.BinaryExpression eqConstraint = constraintsList.iterator().next();
		Object value = (Object) ((Constant) eqConstraint.getLeft()).getValue();
		String keyString = value.toString();

		GlobalVariable keyStringVar = new GlobalVariable(Untyped.INSTANCE, keyString, location);

		AccessChild accessMapKey = new AccessChild(Untyped.INSTANCE, derefMap, keyStringVar, location);

		ExpressionSet ids = analysis.rewrite(state, accessMapKey, context, this);
		boolean anyKnown = false;
		boolean anyUnknown = ids.isEmpty();

		for (SymbolicExpression e : ids) {
			if (e instanceof Identifier id && state.knowsIdentifier(id))
				anyKnown = true;
			else
				anyUnknown = true;
		}

		AnalysisState<A> foundState = state.bottomExecution();
		AnalysisState<A> nullState = state.bottomExecution();
		if (anyKnown) {
			foundState = analysis.smallStepSemantics(state, accessMapKey, context, this);
		}
		if (anyUnknown) {
			nullState = analysis.smallStepSemantics(state, new NullConstant(getLocation()), context,
					originating);
		}
		return foundState.lub(nullState);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Satisfiability checkZeroSize(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression map,
			StatementStore<A> expressions)
			throws SemanticException {

		CodeLocation location = getLocation();
		ScopeId<A> context = interprocedural.getCurrentContext();
		Type mapType = JavaClassType.lookup("java.util.Map");

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "size", location);
		HeapDereference derefMap = new HeapDereference(mapType, map, location);
		AccessChild accessSize = new AccessChild(JavaIntType.INSTANCE, derefMap, var, location);

		Constant zero = new Constant(JavaIntType.INSTANCE, 0, location);
		SymbolicExpression eq = new it.unive.lisa.symbolic.value.BinaryExpression(JavaBooleanType.INSTANCE, accessSize,
				zero, ComparisonEq.INSTANCE, location);

		Satisfiability sat = interprocedural.getAnalysis().satisfies(state, eq, context, this);
		return sat;
	}

}

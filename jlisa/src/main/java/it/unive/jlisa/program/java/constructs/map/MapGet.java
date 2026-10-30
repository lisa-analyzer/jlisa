package it.unive.jlisa.program.java.constructs.map;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import it.unive.jlisa.program.ConstraintUtils;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
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
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;

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

		Stream<it.unive.lisa.symbolic.value.BinaryExpression> constraints = ConstraintUtils.extractConstraints(interprocedural, state, accessKeyArg, this);
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
		return analysis.smallStepSemantics(state, accessMapKey, context, this);

		// AnalysisState<A> tmp = state.bottomExecution();
		// for (Type t : analysis.getRuntimeTypesOf(state, right, context, this)) {
		// 	AccessChild accessMapKey = new AccessChild(t, derefMap, keyStringVar, location);
		// 	tmp = tmp.lub(analysis.assign(state, accessMapKey, right, context, this));
		// }
	}

}



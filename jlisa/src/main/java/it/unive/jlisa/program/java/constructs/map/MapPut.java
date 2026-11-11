package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.ConstraintUtils;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class MapPut extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public MapPut(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "put", left, middle, right);
	}

	public static MapPut build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapPut(cfg, location, params[0], params[1], params[2]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdTernarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		// a ConcurrentMap is not null safe
		return MapModel.nullKey(interprocedural, state,
				semantics(interprocedural, state, left, middle, right, expressions), left,
				middle, this, getSubExpressions(), expressions);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> semantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		Set<Type> keyTypes = analysis.getRuntimeTypesOf(state, middle, context, this);

		// a key that is not a constant goes to the entries of the map, if they
		// are tracked (see MapModel)
		if (!isConstantKey(interprocedural, state, middle, keyTypes)
				&& MapModel.isTracked(analysis, context, state, left, this))
			return MapModel.put(interprocedural, state, left, middle, right, getRight(), this, expressions);

		AnalysisState<A> res = state.bottomExecution();

		if (keyTypes.size() == 1) {
			Type keyType = keyTypes.iterator().next();
			if (keyType instanceof JavaReferenceType jrt)
				keyType = jrt.getInnerType();

			if (keyType == JavaIntType.INSTANCE || keyType.isStringType()) {
				res = assignValueToKey(interprocedural, state, left, middle, right, keyType, expressions);
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

		return incrementSize(interprocedural, res, left, expressions);
	}

	// whether the key is a constant (an int or a string with a single value),
	// that is written in a field named after it
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean isConstantKey(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression key,
			Set<Type> keyTypes)
			throws SemanticException {
		if (keyTypes.size() != 1)
			return false;
		Type keyType = keyTypes.iterator().next();
		if (keyType instanceof JavaReferenceType jrt)
			keyType = jrt.getInnerType();
		if (keyType != JavaIntType.INSTANCE && !keyType.isStringType())
			return false;
		SymbolicExpression accessKeyArg = key;
		if (keyType.isStringType()) {
			HeapDereference derefKeyArg = new HeapDereference(keyType, key, getLocation());
			GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
			accessKeyArg = new AccessChild(keyType, derefKeyArg, var, getLocation());
		}
		Stream<BinaryExpression> constraints = ConstraintUtils.extractConstraints(interprocedural, state, accessKeyArg,
				this);
		if (constraints == null)
			return true;
		return constraints.toList().size() == 1;
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
				new PushAny(Untyped.INSTANCE, getLocation()),
				context, originating);
		return topState;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> assignValueToKey(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			Type keyType,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();
		Type mapType = JavaClassType.lookup("java.util.Map");

		HeapDereference derefMap = new HeapDereference(mapType, left, location);

		SymbolicExpression accessKeyArg = middle;
		if (keyType.isStringType()) {
			HeapDereference derefKeyArg = new HeapDereference(keyType, middle, location);
			GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", location);
			accessKeyArg = new AccessChild(keyType, derefKeyArg, var, location);
		}

		Stream<BinaryExpression> constraints = ConstraintUtils.extractConstraints(interprocedural, state, accessKeyArg,
				this);
		if (constraints == null)
			return state.bottomExecution();

		List<BinaryExpression> constraintsList = constraints.toList();
		if (constraintsList.isEmpty() || constraintsList.size() > 1) {
			return topReturnValue(interprocedural, state, expressions);
		}

		BinaryExpression eqConstraint = constraintsList.iterator().next();
		Object value = (Object) ((Constant) eqConstraint.getLeft()).getValue();
		String keyString = value.toString();

		GlobalVariable keyStringVar = new GlobalVariable(Untyped.INSTANCE, keyString, location);

		AnalysisState<A> tmp = state;
		for (Type t : analysis.getRuntimeTypesOf(state, right, context, this)) {

			ExpressionSet rightExprs = new ExpressionSet(right);
			AnalysisState<A> wrapped = tmp;
			if (JavaTypeSystem.isPrimitive(t)) {
				wrapped = boxValue(interprocedural, wrapped, right, t, expressions);
				rightExprs = wrapped.getExecutionExpressions();
				t = new JavaReferenceType(JavaClassType.getWrappedType(t));
			}

			AccessChild accessMapKey = new AccessChild(t, derefMap, keyStringVar, location);

			for (SymbolicExpression s : rightExprs) {
				wrapped = wrapped.lub(analysis.assign(wrapped, accessMapKey, s, context, this));
			}
			tmp = wrapped;
		}
		return tmp;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> boxValue(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			Type valueType,
			StatementStore<A> expressions)
			throws SemanticException {
		// CodeLocation location = getLocation();
		Type boxedType = new JavaReferenceType(JavaClassType.getWrappedType(valueType));

		JavaNewObj wrap = new JavaNewObj(getCFG(), getRight().getLocation(), (JavaReferenceType) boxedType,
				new Expression[] { getRight() });

		return wrap.forwardSemantics(state, interprocedural, expressions);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> incrementSize(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression map,
			StatementStore<A> expressions)
			throws SemanticException {
		CodeLocation location = getLocation();
		ScopeId<A> context = interprocedural.getCurrentContext();
		AccessChild accessSize = MapModel.size(map, location);

		Constant one = new Constant(JavaIntType.INSTANCE, 1, location);
		SymbolicExpression add = new BinaryExpression(JavaIntType.INSTANCE, accessSize, one,
				NumericNonOverflowingAdd.INSTANCE, location);

		return interprocedural.getAnalysis().assign(state, accessSize, add, context, this);
	}
}

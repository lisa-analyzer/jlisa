package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Reachability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.ReachabilityProduct;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;

public class ClassGetMethod extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public ClassGetMethod(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "getMethod", left, middle, right);
	}

	public static ClassGetMethod build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassGetMethod(cfg, location, params[0], params[1], params[2]);
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

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type stringType = getProgram().getTypes().getStringType();
		Type refStringType = new JavaReferenceType(stringType);
		Type classMetaType = JavaClassType.getClassMetaType();
		Type methodType = JavaClassType.getMethodType();
		JavaReferenceType refMethodType = new JavaReferenceType(methodType);
		JavaArrayType methodArrType = JavaArrayType.lookup(refMethodType, 1);
		JavaReferenceType refMethodArrType = new JavaReferenceType(methodArrType);

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);
		GlobalVariable declaredMethodsVar = new GlobalVariable(Untyped.INSTANCE, "declaredMethods", location);

		HeapDereference derefClazz = new HeapDereference(classMetaType, left, location);

		AccessChild accessClazzName = new AccessChild(refStringType, derefClazz, nameVar, location);
		HeapDereference derefClazzName = new HeapDereference(stringType, accessClazzName, location);
		AccessChild accessClazzNameValue = new AccessChild(stringType, derefClazzName, valueVar, location);

		Set<it.unive.lisa.symbolic.value.BinaryExpression> constraints = getConstraints(analysis, state, accessClazzNameValue);

		// TODO: temporary assumption
		assert(constraints.size() == 1);

		it.unive.lisa.symbolic.value.BinaryExpression constraint = constraints.iterator().next();
		String clazzName = (String)((Constant)constraint.getLeft()).getValue();
		UnitType t = getTypeFromStr(clazzName);

		if (!ReflectionCache.isClassInitialized(t)) {
			ExpressionSet clazz = new ExpressionSet(ReflectionCache.getCachedClass(t));

			InternalInitClassMetaObject initClazz = new InternalInitClassMetaObject(getCFG(), location, t);
			AnalysisState<A> initState = initClazz.forwardSemanticsAux(interprocedural, state, new ExpressionSet[] {clazz}, expressions);

			state = initState;
		}

		// (*left)->declaredMethods
		AccessChild accessDeclaredMethods = new AccessChild(refMethodArrType, derefClazz, declaredMethodsVar, location);

		// *((*left)->declaredMethods)
		HeapDereference derefArr = new HeapDereference(methodArrType, accessDeclaredMethods, location);

		// (*(*left)->declaredMethods)->length
		AccessChild lenAccess = new AccessChild(JavaIntType.INSTANCE, derefArr, lengthVar, location);

		boolean outOfBoundsMethodArr = false;
		int i = 0;

		// stop when we are out of bounds
		while (outOfBoundsMethodArr == false) {

			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);

			it.unive.lisa.symbolic.value.BinaryExpression withinBounds = new it.unive.lisa.symbolic.value.BinaryExpression( JavaBooleanType.INSTANCE,
				idx, lenAccess, ComparisonLt.INSTANCE, location);

			Satisfiability sat = analysis.satisfies(state, withinBounds, this);
			if (sat == Satisfiability.NOT_SATISFIED) {
				outOfBoundsMethodArr = true;
				break;
			}

			// check if the two methods' signatures are the same

			AccessChild accessMethod = new AccessChild(refMethodType, derefArr, idx, location);
			boolean methodFound = matchesTarget(interprocedural, state, accessMethod, middle, right);

			if (methodFound) {
				HeapReference refMethod = new HeapReference(refMethodType, accessMethod, location);
				return analysis.smallStepSemantics(state, refMethod, this);
			}
			++i;
		}

		// TODO AP: method not found here

		return state.bottomExecution();
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	// check whether a target method matches the signature of the candidate one
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean matchesTarget(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression candidateMethod,
			SymbolicExpression targetMethodName,
			SymbolicExpression targetMethodParameterTypes)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type stringType = JavaClassType.getStringType();
		JavaReferenceType refStringType = new JavaReferenceType(stringType);
		Type methodMetaType = JavaClassType.getMethodType();
		Type classMetaType = JavaClassType.getClassMetaType();
		Type refClassMetaType = new JavaReferenceType(classMetaType);
		JavaArrayType classArrType = JavaArrayType.lookup(refClassMetaType, 1);
		JavaReferenceType refClassArrType = new JavaReferenceType(classArrType);

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);
		GlobalVariable parameterTypesVar = new GlobalVariable(Untyped.INSTANCE, "parameterTypes", location);

		// candidateMethod is of type Method*

		// stringequals on the names
		HeapDereference derefMethod = new HeapDereference(methodMetaType, candidateMethod, location);
		AccessChild accessMethodName = new AccessChild(refStringType, derefMethod, nameVar, location);

		HeapDereference derefMethodName = new HeapDereference(stringType, accessMethodName, location);
		AccessChild accessMethodNameValue = new AccessChild(stringType, derefMethodName, valueVar, location);

		HeapDereference derefTargetMethodName = new HeapDereference(stringType, targetMethodName, location);
		AccessChild accessTargetMethodNameValue = new AccessChild(stringType, derefTargetMethodName, valueVar, location);

		it.unive.lisa.symbolic.value.BinaryExpression equalsExpr = new it.unive.lisa.symbolic.value.BinaryExpression(
				getProgram().getTypes().getBooleanType(),
				accessMethodNameValue,
				accessTargetMethodNameValue,
				JavaStringEqualsOperator.INSTANCE,
				getLocation());

		Satisfiability nameMatches = analysis.satisfies(state, equalsExpr, this);

		if (nameMatches == Satisfiability.NOT_SATISFIED) {
			return false;
		}

		// strequals on the name of every Class object
		// NOTE AP: ideally I think one would do just `==` on the Class objects

		AccessChild accessCandidateParameterTypes = new AccessChild(refClassArrType, derefMethod, parameterTypesVar, location);
		HeapDereference derefCandidateArr = new HeapDereference(classArrType, accessCandidateParameterTypes, location);
		AccessChild candidateLenAccess = new AccessChild(JavaIntType.INSTANCE, derefCandidateArr, lengthVar, location);


		HeapDereference derefTargetArr = new HeapDereference(classArrType, targetMethodParameterTypes, location);
		AccessChild targetLenAccess = new AccessChild(JavaIntType.INSTANCE, derefTargetArr, lengthVar, location);


		it.unive.lisa.symbolic.value.BinaryExpression eq = new it.unive.lisa.symbolic.value.BinaryExpression(JavaBooleanType.INSTANCE,
			candidateLenAccess, targetLenAccess, ComparisonEq.INSTANCE, location);

		Satisfiability sameLen = analysis.satisfies(state, eq, this);

		if (sameLen == Satisfiability.NOT_SATISFIED) {
			return false;
		}

		boolean outOfBoundsParamsArr = false;
		boolean allParametersMatch = true;

		// stop when we are out of bounds
		for (int i = 0; outOfBoundsParamsArr == false; ++i) {

			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);

			it.unive.lisa.symbolic.value.BinaryExpression withinBounds = new it.unive.lisa.symbolic.value.BinaryExpression( JavaBooleanType.INSTANCE,
				idx, targetLenAccess, ComparisonLt.INSTANCE, location);

			Satisfiability sat = analysis.satisfies(state, withinBounds, this);
			if (sat == Satisfiability.NOT_SATISFIED) {
				outOfBoundsParamsArr = true;
				break;
			}

			AccessChild accessCandidateClazz = new AccessChild(refClassMetaType, derefCandidateArr, idx, location);
			AccessChild accessTargetClazz = new AccessChild(refClassMetaType, derefTargetArr, idx, location);

			HeapDereference derefCandidateClazz = new HeapDereference(classMetaType, accessCandidateClazz, location);
			HeapDereference derefTargetClazz = new HeapDereference(classMetaType, accessTargetClazz, location);

			AccessChild accessCandidateName = new AccessChild(refStringType, derefCandidateClazz, nameVar, location);
			AccessChild accessTargetName = new AccessChild(refStringType, derefTargetClazz, nameVar, location);

			HeapDereference derefCandidateName = new HeapDereference(stringType, accessCandidateName, location);
			HeapDereference derefTargetName = new HeapDereference(stringType, accessTargetName, location);

			AccessChild accessCandidateValue = new AccessChild(stringType, derefCandidateName, valueVar, location);
			AccessChild accessTargetValue = new AccessChild(stringType, derefTargetName, valueVar, location);

			equalsExpr = new it.unive.lisa.symbolic.value.BinaryExpression(
					getProgram().getTypes().getBooleanType(),
					accessCandidateValue,
					accessTargetValue,
					JavaStringEqualsOperator.INSTANCE,
					getLocation());

			nameMatches = analysis.satisfies(state, equalsExpr, this);

			if (nameMatches == Satisfiability.NOT_SATISFIED) {
				allParametersMatch = false;
				break;
			}
		}

		return allParametersMatch;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Set<it.unive.lisa.symbolic.value.BinaryExpression> getConstraints(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression expr) {

		Set<it.unive.lisa.symbolic.value.BinaryExpression> constraints = new HashSet<>();

		try {
			Class<?> c = Reachability.class;
			Field f = c.getDeclaredField("domain");

			f.setAccessible(true);

			SimpleAbstractDomain<?, ?, ?> innerDomain = (SimpleAbstractDomain<?, ?, ?>) f.get(analysis.domain);

			ValueDomain vdom = (ValueDomain) innerDomain.valueDomain;

			Object executionState = state.getExecutionState();
			ReachabilityProduct<?> reachabilityProduct = (ReachabilityProduct<?>) executionState;

			SimpleAbstractState simpleAbstractState = (SimpleAbstractState) reachabilityProduct.second;

			ValueLattice env = (ValueLattice) simpleAbstractState.valueState;

			SemanticOracle oracle = innerDomain.makeOracle(simpleAbstractState);

			ValueExpression ex = (ValueExpression) analysis.rewrite(state, expr, this).iterator().next();

			constraints = vdom.constraints(null, env, ex, this, oracle);
		}
		catch (Exception e) {
		}

		return constraints;
	}

	private UnitType getTypeFromStr(String clazzName) {

		clazzName = clazzName.replace('$', '.');

		// NOTE: `Class.forName` cannot access `Class` of primitive types. For that the class literal is needed

		JavaClassType foundClass = null;
		JavaInterfaceType foundInterface = null;

		try {
			foundClass = JavaClassType.lookup(clazzName);
		} catch (IllegalArgumentException e) {
		}
		try {
			foundInterface = JavaInterfaceType.lookup(clazzName);
		} catch (IllegalArgumentException e) {
		}

		UnitType t = (foundClass != null) ? foundClass : foundInterface;
		return t;
	}

}

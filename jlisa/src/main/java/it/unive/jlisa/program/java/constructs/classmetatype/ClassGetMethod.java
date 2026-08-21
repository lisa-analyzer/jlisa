package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import it.unive.jlisa.program.ReflectionDataUtils;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
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
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.BinaryExpression;
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

		AnalysisState<A> result = state.bottomExecution();

		ExpressionSet classes = analysis.rewrite(state, new HeapDereference(Untyped.INSTANCE, left, getLocation()),
				this);
		for (SymbolicExpression clazz : classes) {

			AnalysisState<A> searchResult = state;
			Set<Type> clazzTypes = analysis.getRuntimeTypesOf(state, clazz, this);

			for (Type t : clazzTypes) {
				if (t instanceof JavaReferenceType jrt && jrt.getInnerType().isNullType()) {
					searchResult = throwNullPointerException(interprocedural, searchResult, expressions);
				}
				// search for the method
				else {
					AnalysisState<
							A> methodSearched = searchMethod(interprocedural, searchResult, clazz, middle, right,
									expressions);

					if (methodSearched.isTop() || methodSearched.isBottom()) {
						return methodSearched;
					}

					// didn't find any matching method
					if (methodSearched.getExecutionExpressions().isEmpty()) {
						searchResult = throwNoSuchMethodException(interprocedural, methodSearched, expressions);
					} else {
						// found at least one method, copy them
						AnalysisState<A> tmp = state.bottomExecution();
						for (SymbolicExpression expr : methodSearched.getExecutionExpressions()) {
							ClassCopyMethod copyMethod = new ClassCopyMethod(getCFG(), getLocation(), getMiddle());
							tmp = tmp.lub(
									copyMethod.fwdUnarySemantics(interprocedural, methodSearched, expr, expressions));
						}
						searchResult = tmp;
					}
				}
			}
			result = result.lub(searchResult);
		}

		return result;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> searchMethod(
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
		Type refClassMetaType = new JavaReferenceType(classMetaType);
		Type methodType = JavaClassType.getMethodType();
		JavaReferenceType refMethodType = new JavaReferenceType(methodType);
		JavaArrayType methodArrType = JavaArrayType.lookup(refMethodType, 1);
		JavaReferenceType refMethodArrType = new JavaReferenceType(methodArrType);

		SymbolicExpression derefClazz = left;

		// get the type of the left expression
		Set<Type> clazzTypes = analysis.getRuntimeTypesOf(state, left, this);

		// NOTE: this is always either Class or null (in case we reached the top
		// of the class hierarchy)
		assert (clazzTypes.size() == 1);
		Type clazzType = clazzTypes.iterator().next();

		if (clazzType instanceof JavaReferenceType) {
			derefClazz = new HeapDereference(classMetaType, left, location);
		}

		// we only have the null type. Stop the search
		if ((clazzType instanceof JavaReferenceType jrt && jrt.getInnerType().isNullType())
				|| clazzType.isNullType()) {
			return state.withExecutionExpressions(new ExpressionSet());
		}

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);
		GlobalVariable declaredMethodsVar = new GlobalVariable(Untyped.INSTANCE, "declaredMethods", location);
		GlobalVariable superClassVar = new GlobalVariable(Untyped.INSTANCE, "superClass", location);

		AccessChild accessClazzName = new AccessChild(refStringType, derefClazz, nameVar, location);
		HeapDereference derefClazzName = new HeapDereference(stringType, accessClazzName, location);
		AccessChild accessClazzNameValue = new AccessChild(stringType, derefClazzName, valueVar, location);

		Stream<BinaryExpression> constraints = extractConstraints(interprocedural, state, accessClazzNameValue);

		if (constraints == null)
			return state.topExecution();

		// make sure that all classes we are searching have thei reflection data
		// loaded
		for (BinaryExpression constraint : constraints.toList()) {
			String clazzName = (String) ((Constant) constraint.getLeft()).getValue();
			UnitType t = getTypeFromStr(clazzName);

			assert (t != null);
			if (t == null)
				return state.topExecution();

			if (!ReflectionDataUtils.isClassReflectionDataCached(interprocedural, state, left, this)) {

				assert (ReflectionDataUtils.isClassLoaded(state, t, location));
				ExpressionSet clazz = new ExpressionSet(
						ReflectionDataUtils.getLoadedClassHandle(t, location));

				InternalInitClassMetaObject initClazz = new InternalInitClassMetaObject(getCFG(), location, t,
						getLeft());
				AnalysisState<A> initState = initClazz.forwardSemanticsAux(interprocedural, state,
						new ExpressionSet[] { clazz }, expressions);

				state = initState;
			}
		}

		// (*left)->declaredMethods
		AccessChild accessDeclaredMethods = new AccessChild(refMethodArrType, derefClazz, declaredMethodsVar, location);

		// *((*left)->declaredMethods)
		HeapDereference derefArr = new HeapDereference(methodArrType, accessDeclaredMethods, location);

		// (*(*left)->declaredMethods)->length
		AccessChild lenAccess = new AccessChild(JavaIntType.INSTANCE, derefArr, lengthVar, location);

		boolean outOfBoundsMethodArr = false;
		int i = 0;

		ExpressionSet unknownMethods = new ExpressionSet();

		// stop when we are out of bounds
		while (outOfBoundsMethodArr == false) {

			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);

			it.unive.lisa.symbolic.value.BinaryExpression withinBounds = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaBooleanType.INSTANCE,
					idx, lenAccess, ComparisonLt.INSTANCE, location);

			Satisfiability sat = analysis.satisfies(state, withinBounds, this);
			if (sat == Satisfiability.NOT_SATISFIED) {
				outOfBoundsMethodArr = true;
				break;
			}

			// check if the two methods' signatures are the same
			AccessChild accessMethod = new AccessChild(refMethodType, derefArr, idx, location);

			// searching
			Satisfiability methodFound = matchesTarget(interprocedural, state, accessMethod, middle, right);

			if (methodFound == Satisfiability.SATISFIED) {
				HeapReference refMethod = new HeapReference(refMethodType, accessMethod, location);
				return analysis.smallStepSemantics(state, refMethod, this);
			} else if (methodFound == Satisfiability.UNKNOWN) {
				HeapReference refMethod = new HeapReference(refMethodType, accessMethod, location);
				AnalysisState<A> noExceptionState = analysis.smallStepSemantics(state, refMethod, this);
				unknownMethods = unknownMethods.lub(noExceptionState.getExecutionExpressions());

				AnalysisState<A> exceptionState = throwNoSuchMethodException(interprocedural, state, expressions);

				state = noExceptionState.lub(exceptionState);
			}

			++i;
		}

		// haven't found the method. Look in the superclass
		AccessChild superClass = new AccessChild(refClassMetaType, derefClazz, superClassVar, location);
		state = searchMethod(interprocedural, state, superClass, middle, right, expressions);

		state = state.withExecutionExpressions(state.getExecutionExpressions().lub(unknownMethods));

		// we didn't find anything in the superclasses.
		if (state.getExecutionExpressions().isEmpty()) {
			state = searchInterfaces(interprocedural, state, derefClazz, middle, right, expressions);
		}

		return state;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> searchInterfaces(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression derefClazz,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type classMetaType = JavaClassType.getClassMetaType();
		Type refClassMetaType = new JavaReferenceType(classMetaType);
		JavaReferenceType refClassArr = JavaArrayType.CLASS_ARRAY;

		GlobalVariable interfacesVar = new GlobalVariable(Untyped.INSTANCE, "interfaces", location);
		GlobalVariable lenVar = new GlobalVariable(Untyped.INSTANCE, "length", location);

		AccessChild accessInterfaces = new AccessChild(refClassArr, derefClazz, interfacesVar, location);
		HeapDereference derefInterfaces = new HeapDereference(refClassArr.getInnerType(), accessInterfaces, location);
		AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE, derefInterfaces, lenVar, location);

		boolean outOfBoundsMethodArr = false;
		int i = 0;

		AnalysisState<A> tmp = state;

		// stop when we are out of bounds
		while (outOfBoundsMethodArr == false) {

			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);

			it.unive.lisa.symbolic.value.BinaryExpression withinBounds = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaBooleanType.INSTANCE,
					idx, accessLen, ComparisonLt.INSTANCE, location);

			Satisfiability sat = analysis.satisfies(state, withinBounds, this);
			if (sat == Satisfiability.NOT_SATISFIED) {
				outOfBoundsMethodArr = true;
				break;
			}

			// access the interface and call searchMethod on it
			AccessChild accessInterface = new AccessChild(refClassMetaType, derefInterfaces, idx, location);
			tmp = searchMethod(interprocedural, tmp, accessInterface, middle, right, expressions);

			// NOTE: this stops as soon as any matching method is found.
			// This however, should only happen when we know for sure that the
			// method
			// we found is the correct one
			if (!tmp.getExecutionExpressions().isEmpty()) {
				return tmp;
			}

			++i;
		}

		return tmp.withExecutionExpressions(new ExpressionSet());
	}

	// check whether a target method matches the signature of the candidate one
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Satisfiability matchesTarget(
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
		Satisfiability res = Satisfiability.BOTTOM;

		// stringequals on the names
		HeapDereference derefMethod = new HeapDereference(methodMetaType, candidateMethod, location);
		AccessChild accessMethodName = new AccessChild(refStringType, derefMethod, nameVar, location);

		HeapDereference derefMethodName = new HeapDereference(stringType, accessMethodName, location);
		AccessChild accessMethodNameValue = new AccessChild(stringType, derefMethodName, valueVar, location);

		HeapDereference derefTargetMethodName = new HeapDereference(stringType, targetMethodName, location);
		AccessChild accessTargetMethodNameValue = new AccessChild(stringType, derefTargetMethodName, valueVar,
				location);

		it.unive.lisa.symbolic.value.BinaryExpression equalsExpr = new it.unive.lisa.symbolic.value.BinaryExpression(
				getProgram().getTypes().getBooleanType(),
				accessMethodNameValue,
				accessTargetMethodNameValue,
				JavaStringEqualsOperator.INSTANCE,
				getLocation());

		Satisfiability nameMatches = analysis.satisfies(state, equalsExpr, this);

		if (nameMatches == Satisfiability.NOT_SATISFIED) {
			return nameMatches;
		}
		res = res.lub(nameMatches);

		// strequals on the name of every Class object
		// NOTE: ideally I think one would do just `==` on the Class objects

		AccessChild accessCandidateParameterTypes = new AccessChild(refClassArrType, derefMethod, parameterTypesVar,
				location);
		HeapDereference derefCandidateArr = new HeapDereference(classArrType, accessCandidateParameterTypes, location);
		AccessChild candidateLenAccess = new AccessChild(JavaIntType.INSTANCE, derefCandidateArr, lengthVar, location);

		HeapDereference derefTargetArr = new HeapDereference(classArrType, targetMethodParameterTypes, location);
		AccessChild targetLenAccess = new AccessChild(JavaIntType.INSTANCE, derefTargetArr, lengthVar, location);

		it.unive.lisa.symbolic.value.BinaryExpression eq = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				candidateLenAccess, targetLenAccess, ComparisonEq.INSTANCE, location);

		Satisfiability sameLen = analysis.satisfies(state, eq, this);

		if (sameLen == Satisfiability.NOT_SATISFIED) {
			return sameLen;
		}
		res = res.lub(sameLen);

		boolean outOfBoundsParamsArr = false;

		// stop when we are out of bounds
		for (int i = 0; outOfBoundsParamsArr == false; ++i) {

			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);

			it.unive.lisa.symbolic.value.BinaryExpression withinBounds = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaBooleanType.INSTANCE,
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
				return nameMatches;
			}

			res = res.lub(nameMatches);
		}

		return res;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> throwNoSuchMethodException(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		JavaClassType noSuchMethodType = JavaClassType.getNoSuchMethodException();

		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
				noSuchMethodType.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		// assign exception to variable thrower
		CFGThrow throwVar = new CFGThrow(getCFG(), noSuchMethodType.getReference(), getLocation());
		state = analysis.assign(state, throwVar,
				state.getExecutionExpressions().elements.stream().findFirst().get(), this);

		// deletes the receiver of the constructor
		// and all the metavariables from subexpressions
		state = state.forgetIdentifiers(call.getMetaVariables(), this);
		state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
		state = state.forgetIdentifiers(getRight().getMetaVariables(), this);

		return analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
				new Error(noSuchMethodType.getReference(), originating), this);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> throwNullPointerException(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		JavaClassType nullPointerExceptionType = JavaClassType.getNullPointerExceptionType();

		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
				nullPointerExceptionType.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		// assign exception to variable thrower
		CFGThrow throwVar = new CFGThrow(getCFG(), nullPointerExceptionType.getReference(), getLocation());
		state = analysis.assign(state, throwVar,
				state.getExecutionExpressions().elements.stream().findFirst().get(), this);

		// deletes the receiver of the constructor
		// and all the metavariables from subexpressions
		state = state.forgetIdentifiers(call.getMetaVariables(), this);
		state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
		state = state.forgetIdentifiers(getRight().getMetaVariables(), this);

		return analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
				new Error(nullPointerExceptionType.getReference(), originating), this);
	}

	private UnitType getTypeFromStr(
			String clazzName) {

		clazzName = clazzName.replace('$', '.');

		// NOTE: `Class.forName` cannot access `Class` of primitive types. For
		// that the class literal is needed
		Type t = getProgram().getTypes().getType(clazzName);

		if (!(t instanceof UnitType))
			return null;

		return (UnitType) t;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Stream<BinaryExpression> extractConstraints(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		SimpleAbstractDomain<?, ?, ?> innerDomain;

		try {
			Class<?> c = Reachability.class;
			Field f = c.getDeclaredField("domain");

			f.setAccessible(true);

			innerDomain = (SimpleAbstractDomain<?, ?, ?>) f.get(analysis.domain);
		} catch (Exception e) {
			return null;
		}

		assert (innerDomain != null);
		ValueDomain vdom = (ValueDomain) innerDomain.valueDomain;

		Object executionState = state.getExecutionState();
		ReachabilityProduct<?> reachabilityProduct = (ReachabilityProduct<?>) executionState;

		SimpleAbstractState simpleAbstractState = (SimpleAbstractState) reachabilityProduct.second;

		ValueLattice env = (ValueLattice) simpleAbstractState.valueState;

		SemanticOracle oracle = innerDomain.makeOracle(simpleAbstractState);

		ExpressionSet rewritten = analysis.rewrite(state, expr, this);

		return StreamSupport.stream(rewritten.spliterator(), false)
				.map(ex -> (ValueExpression) ex)
				.flatMap(vex -> {
					try {
						return vdom.constraints(null, env, vex, this, oracle).stream();
					} catch (SemanticException e) {
						return null;
					}
				});
	}

}

package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaIsClassDefinedOperator;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Reachability;
import it.unive.lisa.analysis.AnalysisState.Error;
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
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ClassForName extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {
	protected Statement originating;

	private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("java.lang.Class");

	public ClassForName(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "forName", JavaClassType.getClassMetaType(), expr);
	}

	public static ClassForName build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassForName(cfg, location, params[0]);
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
		CodeLocation location = getLocation();
		CFG cfg = getCFG();

		Type stringType = getProgram().getTypes().getStringType();
		Type classMetaType = JavaClassType.getClassMetaType();
		Type refClassMetaType = new JavaReferenceType(classMetaType);

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", location);
		HeapDereference derefExpr = new HeapDereference(stringType, expr, location);
		AccessChild accessExpr = new AccessChild(stringType, derefExpr, var, location);

		// check if class actually exists
		it.unive.lisa.symbolic.value.UnaryExpression isClassDefined = new it.unive.lisa.symbolic.value.UnaryExpression(
				stringType,
				accessExpr,
				JavaIsClassDefinedOperator.INSTANCE,
				location);

		Satisfiability sat = analysis.satisfies(state, isClassDefined, originating);

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();

		// populate the "no exception" path
		if (sat != Satisfiability.NOT_SATISFIED) {

			Set<BinaryExpression> constraints = new HashSet<>();

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

				ValueExpression ex = (ValueExpression) analysis.rewrite(state, accessExpr, this).iterator().next();

				constraints = vdom.constraints(null, env, ex, this, oracle);
			}
			catch (Exception e) {
			}


			assert(constraints.size() == 1);

			for (BinaryExpression constraint : constraints) {

				String clazzName = (String)((Constant)constraint.getLeft()).getValue();
				UnitType t = getTypeFromStr(clazzName);

				// TODO AP: static initializer goes here
				// ClassUnit classUnit = (ClassUnit) t.getUnit();
				// if (classUnit.getCodeMembersByName(t.toString()).isEmpty()) {
				// 	Set<CompilationUnit> superClasses = classUnit
				// 			.getImmediateAncestors().stream()
				// 			.filter(u -> u instanceof ClassUnit)
				// 			.collect(Collectors.toSet());
				//
				// 	classUnit = (ClassUnit) superClasses.stream().findFirst().orElse(classUnit);
				// }
				// state = InitializedClassSet.initialize(state, new JavaReferenceType(t), this, interprocedural);

				LoadClass loadClass = new LoadClass(t, clazzName, cfg, location);
				AnalysisState<A> callState = loadClass.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

				AnalysisState<A> tmp = callState;

				SymbolicExpression clazz = callState.getExecutionExpressions().iterator().next();

				if (!ReflectionCache.isClassInitialized(t)) {

					AnalysisState<A> fieldsLoaded = loadGlobals(interprocedural, callState, expressions, getAllFields(t.getUnit()), clazz);

					AnalysisState<A> methodsLoaded = loadMethods(interprocedural, fieldsLoaded, expressions, getAllMethods(t.getUnit()), clazz);

					// set it as initialized to avoid reinitialization
					ReflectionCache.addInitializedClass(t);

					AnalysisState<A> superclassesInit = initializeSuperclasses(interprocedural, methodsLoaded, expressions, clazz, t);

					tmp = tmp.lub(superclassesInit);
				}

				noExceptionState = analysis.smallStepSemantics(tmp, clazz, this);
			}
		}

		// `ClassNotFoundException to be thrown
		if (sat != Satisfiability.SATISFIED) {

			JavaClassType classNotFoundType = JavaClassType.getClassNotFoundException();

			JavaNewObj call = new JavaNewObj(cfg, location,
					classNotFoundType.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(cfg, classNotFoundType.getReference(), location);
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);
			state = state.forgetIdentifiers(getSubExpression().getMetaVariables(), this);

			exceptionState = analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(classNotFoundType.getReference(), originating), this);

		}

		return exceptionState.lub(noExceptionState);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	Collection<Global> getAllFields(CompilationUnit unit) {
		Collection<Global> fields = new ArrayList<>(unit.getGlobalsRecursively());
		return fields;
	}


	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> loadGlobals(
		InterproceduralAnalysis<A, D> interprocedural,
		AnalysisState<A> state,
		StatementStore<A> expressions,
		Collection<Global> globals,
		SymbolicExpression clazz)
			throws SemanticException {

		CodeLocation location = getLocation();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		JavaReferenceType wrappedFieldType = new JavaReferenceType(JavaClassType.getFieldMetaType());
		JavaClassType classMetaType = JavaClassType.getClassMetaType();
		JavaArrayType fieldArrType = JavaArrayType.lookup(wrappedFieldType, 1);

		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", getLocation());
		GlobalVariable declaredFieldsVar = new GlobalVariable(Untyped.INSTANCE, "declaredFields", getLocation());

		MemoryAllocation created = new MemoryAllocation(fieldArrType, synGen.nextLocation(), false);
		HeapReference ref = new HeapReference(new JavaReferenceType(fieldArrType), created, getLocation());

		AnalysisState<A> arrAllocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver array = new InstrumentedReceiver(new JavaReferenceType(fieldArrType), true, getLocation());
		arrAllocated = analysis.assign(arrAllocated, array, ref, this);

		AnalysisState<A> tmp = arrAllocated.bottomExecution();

		HeapDereference arrayDeref = new HeapDereference(fieldArrType, array, getLocation());

		// FIXME AP: this should really use newArrayWithInitializer. If not, need to initialize the length variable

		// assign length to array
		Constant c = new Constant(JavaIntType.INSTANCE, globals.size(), location);
		AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE, arrayDeref, lengthVar, location);
		tmp = tmp.lub(analysis.assign(arrAllocated, accessLen, c, this));


		// assign to `declaredFields` the newly allocated array
		HeapDereference derefClazz = new HeapDereference(classMetaType, clazz, getLocation());
		AccessChild accessDeclaredFields = new AccessChild(new JavaReferenceType(fieldArrType), derefClazz, declaredFieldsVar, getLocation());

		int nextIdx = 0;

		for (Global global : globals) {

			Constant idx = new Constant(JavaIntType.INSTANCE, nextIdx, location);

			AccessChild accessIdx = new AccessChild(wrappedFieldType, arrayDeref, idx, getLocation());

			LoadField loadField = new LoadField(global, getCFG(), getLocation(), new Expression[0]);

			ExpressionSet[] params = genLoadFieldParams(clazz, global);

			AnalysisState<A> t = loadField.forwardSemanticsAux(interprocedural, arrAllocated, params, expressions);

			// assign initialized field to the next index of the array
			for (SymbolicExpression initializedField : t.getExecutionExpressions()) {
				AnalysisState<A> t2 = analysis.assign(t, accessIdx, initializedField, this);

				tmp = tmp.lub(t2);
			}

			++nextIdx;

		}

		tmp = tmp.lub(analysis.assign(arrAllocated, accessDeclaredFields, array, this));
		tmp = tmp.forgetIdentifier(array, this);

		return tmp;

	}

	private ExpressionSet[] genLoadFieldParams(SymbolicExpression clazz, Global global) {

		CodeLocation location = getLocation();
		Type stringType = JavaClassType.getStringType();

		// 4 parameters flow into loadField
		ExpressionSet[] params = new ExpressionSet[4];

		// 0 is clazz
		params[0] = new ExpressionSet(clazz);

		String fieldName = global.getName();

		// 1 is field name
		Constant c1 = new Constant(stringType, fieldName, location);
		params[1] = new ExpressionSet(c1);

		// 2 is field type
		Type t = global.getStaticType();
		if (t instanceof JavaReferenceType jrt)
			t = jrt.getInnerType();
		Constant c2 = new Constant(stringType, t.toString(), location);
		params[2] = new ExpressionSet(c2);

		// 3 is field modifiers
		boolean isInstance = global.isInstance();
		int modifiers = (isInstance) ? 0 : Modifier.STATIC;
		Constant c3 = new Constant(JavaIntType.INSTANCE, modifiers, location);
		params[3] = new ExpressionSet(c3);

		return params;
	}

	Collection<CodeMember> getAllMethods(CompilationUnit unit) {

		// TODO AP: I don't think the ctors should be in there, double check this
		// TODO AP: get rid of the static initializer that sneaks in there
		Collection<CodeMember> methods = new ArrayList<>(unit.getCodeMembersRecursively());

		return methods;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> loadMethods(
		InterproceduralAnalysis<A, D> interprocedural,
		AnalysisState<A> state,
		StatementStore<A> expressions,
		Collection<CodeMember> methods,
		SymbolicExpression clazz)
			throws SemanticException {

		CodeLocation location = getLocation();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		JavaReferenceType wrappedMethodType = new JavaReferenceType(JavaClassType.getMethodType());
		JavaClassType classMetaType = JavaClassType.getClassMetaType();
		JavaArrayType methodArrType = JavaArrayType.lookup(wrappedMethodType, 1);

		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", getLocation());
		GlobalVariable declaredMethodsVar = new GlobalVariable(Untyped.INSTANCE, "declaredMethods", getLocation());

		MemoryAllocation created = new MemoryAllocation(methodArrType, synGen.nextLocation(), false);
		HeapReference ref = new HeapReference(new JavaReferenceType(methodArrType), created, getLocation());

		AnalysisState<A> arrAllocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver array = new InstrumentedReceiver(new JavaReferenceType(methodArrType), true, getLocation());
		arrAllocated = analysis.assign(arrAllocated, array, ref, this);

		AnalysisState<A> tmp = arrAllocated.bottomExecution();

		HeapDereference arrayDeref = new HeapDereference(methodArrType, array, getLocation());

		// FIXME AP: this should really use newArrayWithInitializer. If not, need to initialize the length variable

		// assign length to array
		Constant c = new Constant(JavaIntType.INSTANCE, methods.size(), location);
		AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE, arrayDeref, lengthVar, location);
		tmp = tmp.lub(analysis.assign(arrAllocated, accessLen, c, this));


		// assign to `declaredMethods` the newly allocated array
		HeapDereference derefClazz = new HeapDereference(classMetaType, clazz, getLocation());
		AccessChild accessDeclaredMethods = new AccessChild(new JavaReferenceType(methodArrType), derefClazz, declaredMethodsVar, getLocation());

		int nextIdx = 0;

		for (CodeMember method : methods) {

			Constant idx = new Constant(JavaIntType.INSTANCE, nextIdx, location);

			AccessChild accessIdx = new AccessChild(wrappedMethodType, arrayDeref, idx, getLocation());

			LoadMethod loadMethod = new LoadMethod(method.getDescriptor(), getCFG(), getLocation(), new Expression[0]);

			ExpressionSet[] params = genMethodParams(clazz, method.getDescriptor());

			AnalysisState<A> t = loadMethod.forwardSemanticsAux(interprocedural, arrAllocated, params, expressions);

			// assign initialized method to the next index of the array
			for (SymbolicExpression initializedMethod : t.getExecutionExpressions()) {
				AnalysisState<A> t2 = analysis.assign(t, accessIdx, initializedMethod, this);

				tmp = tmp.lub(t2);
			}

			++nextIdx;

		}

		tmp = tmp.lub(analysis.assign(arrAllocated, accessDeclaredMethods, array, this));
		tmp = tmp.forgetIdentifier(array, this);

		return tmp;

	}

	private ExpressionSet[] genMethodParams(SymbolicExpression clazz, CodeMemberDescriptor d) {

		CodeLocation location = getLocation();
		Type stringType = JavaClassType.getStringType();

		Parameter[] methodParams = d.getFormals();
		int methodParamsCount = methodParams.length;

		// 4 parameters flow into loadField
		ExpressionSet[] params = new ExpressionSet[4 + methodParamsCount];

		// 0 is clazz
		params[0] = new ExpressionSet(clazz);


		// 1 is method name
		String methodName = d.getName();
		Constant c1 = new Constant(stringType, methodName, location);
		params[1] = new ExpressionSet(c1);

		// 2 is method return type
		Type t = d.getReturnType();
		if (t instanceof JavaReferenceType jrt)
			t = jrt.getInnerType();
		Constant c2 = new Constant(stringType, t.toString(), location);
		params[2] = new ExpressionSet(c2);

		// 3 is method modifiers
		boolean isInstance = d.isInstance();
		int modifiers = (isInstance) ? 0 : Modifier.STATIC;
		Constant c3 = new Constant(JavaIntType.INSTANCE, modifiers, location);
		params[3] = new ExpressionSet(c3);

		// the rest are the method formal parameters
		for (int i = 0; i < methodParamsCount; ++i) {

			Type paramType = methodParams[i].getStaticType();
			if (paramType instanceof JavaReferenceType jrt)
				paramType = jrt.getInnerType();

			Constant c = new Constant(stringType, paramType.toString(), location);
			params[4 + i] = new ExpressionSet(c);

		}

		return params;
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

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> initializeSuperclasses(
		InterproceduralAnalysis<A, D> interprocedural,
		AnalysisState<A> state,
		StatementStore<A> expressions,
		SymbolicExpression clazz,
		UnitType unitType)
			throws SemanticException {

		CodeLocation location = getLocation();
		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);

		Collection<CompilationUnit> ancestors = unitType.getUnit().getImmediateAncestors();
		ancestors.removeIf(t -> t instanceof InterfaceUnit);

		// no superclass
		if (ancestors.isEmpty())
			return state;

		assert(ancestors.size() == 1);

		CompilationUnit superclassUnit = ancestors.iterator().next();
		assert(superclassUnit instanceof ClassUnit);

		JavaClassType superclassType = JavaClassType.lookup(superclassUnit.getName());

		GlobalVariable superclassVar = new GlobalVariable(Untyped.INSTANCE, "superClass", location);

		HeapDereference derefClazz = new HeapDereference(classMetaType, clazz, location);

		// ancestor clazz Class object
		AccessChild accessSuperclass = new AccessChild(refClassMetaType, derefClazz, superclassVar, location);

		AnalysisState<A> tmp = state;

		// initialize the class if not already initialized
		if (!ReflectionCache.isClassInitialized(superclassType)) {
			AnalysisState<A> fieldsLoaded = loadGlobals(interprocedural, state, expressions, getAllFields(superclassUnit), accessSuperclass);

			AnalysisState<A> methodsLoaded = loadMethods(interprocedural, fieldsLoaded, expressions, getAllMethods(superclassUnit), accessSuperclass);

			ReflectionCache.addInitializedClass(superclassType);

			tmp = methodsLoaded;
		}

		return tmp.lub(initializeSuperclasses(interprocedural, tmp, expressions, accessSuperclass, superclassType));
	}

}


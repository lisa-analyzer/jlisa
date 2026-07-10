package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
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
import it.unive.lisa.lattices.ExpressionSet;
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
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;

public class InternalInitClassMetaObject extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	private Type initializingClassType;

	private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("InternalInitClassMetaObject");

	public InternalInitClassMetaObject(
			CFG cfg,
			CodeLocation location,
			Type t) {
		super(cfg, location, "internalInitClassMetaObject");
		initializingClassType = t;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}


	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {

		assert(params.length == 1);
		assert(params[0].size() == 1);
		assert(ReflectionCache.isClassLoaded(initializingClassType));

		SymbolicExpression clazz = params[0].iterator().next();

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		AnalysisState<A> noExceptionState = state.bottomExecution();

		AnalysisState<A> tmp = state;

		if (!ReflectionCache.isClassInitialized(initializingClassType)) {

			// set it as initialized to avoid reinitialization
			ReflectionCache.addInitializedClass(initializingClassType);

			if (initializingClassType instanceof UnitType ut) {
				AnalysisState<A> fieldsLoaded = loadGlobals(interprocedural, state, expressions, getAllFields(ut.getUnit()), clazz);

				AnalysisState<A> methodsLoaded = loadMethods(interprocedural, fieldsLoaded, expressions, getAllMethods(ut.getUnit()), clazz);

				AnalysisState<A> superclassesInit = initializeSuperclasses(interprocedural, methodsLoaded, expressions, clazz, ut);

				tmp = superclassesInit;
			}
		}

		noExceptionState = analysis.smallStepSemantics(tmp, clazz, this);

		return noExceptionState;
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

		String unitSimpleName = unit.getName().contains(".")
				? unit.getName().substring(unit.getName().lastIndexOf('.') + 1)
				: unit.getName();

		Collection<CodeMember> methods = unit.getCodeMembersRecursively().stream()
				.filter(cm -> {
					String name = cm.getDescriptor().getName();
					boolean isCtor = name.equals(unitSimpleName);
					boolean isSyntheticClinit = name.endsWith(InitializedClassSet.SUFFIX_CLINIT);
					return !isCtor && !isSyntheticClinit;
				})
				.collect(Collectors.toCollection(ArrayList::new));

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

			SymbolicExpression expr = new HeapReference(refClassMetaType, accessSuperclass, location);

			AnalysisState<A> fieldsLoaded = loadGlobals(interprocedural, state, expressions, getAllFields(superclassUnit), expr);

			AnalysisState<A> methodsLoaded = loadMethods(interprocedural, fieldsLoaded, expressions, getAllMethods(superclassUnit), expr);

			ReflectionCache.addInitializedClass(superclassType);

			tmp = methodsLoaded;
		}

		return tmp.lub(initializeSuperclasses(interprocedural, tmp, expressions, accessSuperclass, superclassType));
	}

}



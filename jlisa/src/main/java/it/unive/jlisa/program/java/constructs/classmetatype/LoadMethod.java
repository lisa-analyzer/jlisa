package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.GhostTypeLookupOperator;
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
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
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
import it.unive.lisa.type.Untyped;

public class LoadMethod extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("java.lang.reflect.Method");

	protected LoadMethod(
			CFG cfg,
			CodeLocation location,
			Expression[] subExpressions) {
		super(cfg, location, "loadMethod", subExpressions);
	}

	public static LoadMethod build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new LoadMethod(cfg, location, params);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;

	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {

		// params[0] is clazz, [1] is fieldname, [2] is type, [3] is modifiers
		// then the other ones are parameter types

		assert(params.length >= 4);

		int methodParamCount = params.length - 4;

		SymbolicExpression[] exprs = new SymbolicExpression[params.length];

		for (int i = 0; i < params.length; ++i) {
			ExpressionSet set = params[i];
			if (set.size() > 1 || set.size() <= 0)
				throw new IllegalArgumentException("Number of operands is incorrect!");
			for (SymbolicExpression expr : set) {
				exprs[i] = expr;
			}
		}

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type intType = JavaIntType.INSTANCE;
		Type stringType = getProgram().getTypes().getStringType();
		Type methodMetaType = JavaClassType.getMethodType();
		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType refMethodMetaType = new JavaReferenceType(methodMetaType);
		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);
		JavaReferenceType refStringType = new JavaReferenceType(stringType);
		JavaArrayType classArrType = JavaArrayType.lookup(refClassMetaType, 1);
		JavaReferenceType refClassArrType = new JavaReferenceType(classArrType);

		GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", location);
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable typeVar = new GlobalVariable(Untyped.INSTANCE, "returnType", location);
		GlobalVariable modifiersVar = new GlobalVariable(Untyped.INSTANCE, "modifiers", location);
		GlobalVariable paramTypesVar = new GlobalVariable(Untyped.INSTANCE, "paramTypes", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);

		AnalysisState<A> resultState = state.bottomExecution();


		MemoryAllocation created = new MemoryAllocation(methodMetaType, synGen.nextLocation(), false);
		HeapReference ref = new HeapReference(refMethodMetaType, created, location);

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver method = new InstrumentedReceiver(refMethodMetaType, false, synGen.nextLocation());
		AnalysisState<A> methodAllocated = analysis.assign(allocated, method, ref, this);

		HeapDereference derefThisMethod = new HeapDereference(methodMetaType, method, location);


		// AnalysisState<A> tmp = methodAllocated.bottomExecution();

		// assign method clazz
		AccessChild accessThisMethodClazz = new AccessChild(refClassMetaType, derefThisMethod, clazzVar, location);

		AnalysisState<A> sem = analysis.assign(methodAllocated, accessThisMethodClazz, exprs[0], this);

		// assign method name
		sem = sem.lub(allocateSubField(interprocedural, methodAllocated, derefThisMethod, nameVar, refStringType, expressions));

		AccessChild accessThisMethodName = new AccessChild(refStringType, derefThisMethod, nameVar, location);

		HeapDereference derefMethodName = new HeapDereference(stringType, accessThisMethodName, location);
		AccessChild dst = new AccessChild(stringType, derefMethodName, valueVar, location);

		sem = analysis.assign(sem, dst, exprs[1], this);


		// assign method type

		// sem = sem.lub(allocateSubField(interprocedural, methodAllocated, derefThisMethod, typeVar, refClassMetaType, expressions));
		//
		// AccessChild accessThisMethodType = new AccessChild(refClassMetaType, derefThisMethod, typeVar, location);
		//
		// HeapDereference derefMethodType = new HeapDereference(classMetaType, accessThisMethodType, location);
		// dst = new AccessChild(stringType, derefMethodType, nameVar, location);
		//
		// sem = analysis.assign(sem, dst, exprs[2], this);


		AccessChild accessThisMethodType = new AccessChild(refClassMetaType, derefThisMethod, typeVar, location);
		sem = lazyLoadClass(interprocedural, sem, exprs[2], expressions);
		sem = analysis.assign(sem, accessThisMethodType, ReflectionCache.getCachedLastClass(), this);


		// assign parameter types
		{
			MemoryAllocation arrCreated = new MemoryAllocation(classArrType, synGen.nextLocation(), false);
			HeapReference arrRef = new HeapReference(refClassArrType, arrCreated, location);

			AnalysisState<A> arrAllocated = analysis.smallStepSemantics(sem, arrCreated, this);

			InstrumentedReceiver array = new InstrumentedReceiver(refClassArrType, true, location);
			arrAllocated = analysis.assign(arrAllocated, array, arrRef, this);

			AnalysisState<A> tmp = arrAllocated.bottomExecution();

			HeapDereference arrayDeref = new HeapDereference(classArrType, array, location);

			// FIXME AP: this should really use newArrayWithInitializer. If not, need to initialize the length variable

			// assign length to array
			Constant arrLen = new Constant(JavaIntType.INSTANCE, methodParamCount, location);
			AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE, arrayDeref, lengthVar, location);
			tmp = tmp.lub(analysis.assign(arrAllocated, accessLen, arrLen, this));


			for (int i = 0; i < methodParamCount; ++i) {

				Constant idx = new Constant(JavaIntType.INSTANCE, i, location);
				// AccessChild accessIdx = new AccessChild(wrappedClassMetaType, arrayDeref, idx, location);


				// TODO assign class type from global arr

				// x

			}

		}



		// assign method modifiers

		// (*method)->modifiers
		AccessChild accessThisMethodModifiers = new AccessChild(intType, derefThisMethod, modifiersVar, location);
		sem = analysis.assign(sem, accessThisMethodModifiers, exprs[3], this);


		resultState = resultState.lub(sem).withExecutionExpression(method);

		return resultState;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> allocateSubField(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			HeapDereference methodDereference,
			GlobalVariable subField,
			JavaReferenceType type,
			StatementStore<A> expressions
			) throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		JavaNewObj call = new JavaNewObj(getCFG(), synGen.nextLocation(),
				type,
				new Expression[0]);
		AnalysisState<
				A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		AccessChild accessSubField = new AccessChild(type, methodDereference, subField, getLocation());

		AnalysisState<A> tmp = state.bottomExecution();

		for (SymbolicExpression allocatedExpr : callState.getExecutionExpressions()) {
			AnalysisState<A> t = analysis.assign(callState, accessSubField, allocatedExpr, this);
			tmp = tmp.lub(t);
		}

		return tmp;
	}

	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> lazyLoadClass(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression clazzName,
			StatementStore<A> expressions)
			throws SemanticException {

		// clazzName is always a constant

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaClassType.getStringType(),
				clazzName,
				GhostTypeLookupOperator.INSTANCE,
				getLocation());

		analysis.satisfies(state, un, this);

		LoadClass loadClass = new LoadClass(getCFG(), getLocation());

		// this can lazily load a new Class object. It also loads a reference to that object in ReflectionCache.lastClass
		AnalysisState<A> classLoaded = loadClass.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		assert(ReflectionCache.lastClass != null);

		return classLoaded;
	}

}


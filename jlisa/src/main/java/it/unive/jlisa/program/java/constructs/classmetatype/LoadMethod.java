package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Modifier;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
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
import it.unive.lisa.type.Untyped;

public class LoadMethod extends NaryExpression implements PluggableStatement {
	private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("java.lang.reflect.Method");

	private CodeMemberDescriptor methodData;

	protected Statement originating;

	protected LoadMethod(
			CodeMemberDescriptor d,
			CFG cfg,
			CodeLocation location,
			Expression[] subExpressions) {
		super(cfg, location, "loadMethod", subExpressions);
		methodData = d;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;

	}

	// TODO AP: change this into a unary expression
	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] p,
			StatementStore<A> expressions)
			throws SemanticException {

		// params[0] is clazz, [1] is fieldname, [2] is type, [3] is modifiers
		// then the other ones are parameter types

		Parameter[] methodParameters = methodData.getFormals();
		int paramCount = methodParameters.length;

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		SymbolicExpression clazz = p[0].iterator().next();

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
		GlobalVariable paramTypesVar = new GlobalVariable(Untyped.INSTANCE, "parameterTypes", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);

		AnalysisState<A> resultState = state.bottomExecution();


		MemoryAllocation created = new MemoryAllocation(methodMetaType, synGen.nextLocation(), false);
		HeapReference ref = new HeapReference(refMethodMetaType, created, location);

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver method = new InstrumentedReceiver(refMethodMetaType, false, synGen.nextLocation());
		AnalysisState<A> methodAllocated = analysis.assign(allocated, method, ref, this);

		HeapDereference derefThisMethod = new HeapDereference(methodMetaType, method, location);


		// assign method clazz
		AccessChild accessThisMethodClazz = new AccessChild(refClassMetaType, derefThisMethod, clazzVar, location);
		AnalysisState<A> sem = analysis.assign(methodAllocated, accessThisMethodClazz, clazz, this);

		// assign method name
		sem = sem.lub(allocateSubField(interprocedural, methodAllocated, derefThisMethod, nameVar, refStringType, expressions));

		AccessChild accessThisMethodName = new AccessChild(refStringType, derefThisMethod, nameVar, location);

		HeapDereference derefMethodName = new HeapDereference(stringType, accessThisMethodName, location);
		AccessChild dst = new AccessChild(stringType, derefMethodName, valueVar, location);

		Constant methodNameConstant = new Constant(stringType, methodData.getName(),location);
		sem = analysis.assign(sem, dst, methodNameConstant, this);


		// assign method type

		Type returnType = methodData.getReturnType();
		if (returnType instanceof JavaReferenceType jrt) {
			returnType = jrt.getInnerType();
		}

		AccessChild accessThisMethodType = new AccessChild(refClassMetaType, derefThisMethod, typeVar, location);
		sem = lazyLoadClass(returnType, interprocedural, sem, expressions);
		sem = analysis.assign(sem, accessThisMethodType, ReflectionCache.getCachedClass(returnType), this);


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
			Constant arrLen = new Constant(JavaIntType.INSTANCE, paramCount - 1, location);
			AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE, arrayDeref, lengthVar, location);
			tmp = tmp.lub(analysis.assign(arrAllocated, accessLen, arrLen, this));


			for (int i = 1; i < paramCount; ++i) {

				Parameter parameter = methodParameters[i];

				Type parameterType = getNoReferenceType(parameter.getStaticType());
				String parameterClazzGlobalName = "__" + parameterType.toString();

				// variable pointing to the corresponding Class object
				GlobalVariable parameterClazz = new GlobalVariable(refClassMetaType, parameterClazzGlobalName, location);

				Constant idx = new Constant(JavaIntType.INSTANCE, i-1, location);
				AccessChild accessIdx = new AccessChild(refClassMetaType, arrayDeref, idx, location);

				// if it doesn't exist we need to load it
				if (!ReflectionCache.isClassLoaded(parameterType)) {
					tmp = tmp.lub(lazyLoadClass(parameterType, interprocedural, tmp, expressions));
				}

				AnalysisState<A> t = analysis.assign(tmp, accessIdx, parameterClazz, this);
				tmp = tmp.lub(t);
			}

			// assign the array to the parameterTypes field of Method
			// TODO AP: add it to the stub file too

			AccessChild accessParameterTypes = new AccessChild(refClassArrType, derefThisMethod, paramTypesVar, location);

			tmp = tmp.lub(analysis.assign(tmp, accessParameterTypes, array, this));
			tmp = tmp.forgetIdentifier(array, this);

			sem = sem.lub(tmp);
		}

		// assign method modifiers
		boolean isInstance = methodData.isInstance();
		int modifiers = (isInstance) ? 0 : Modifier.STATIC;
		Constant modifiersConstant = new Constant(JavaIntType.INSTANCE, modifiers, location);

		// (*method)->modifiers
		AccessChild accessThisMethodModifiers = new AccessChild(intType, derefThisMethod, modifiersVar, location);

		sem = analysis.assign(sem, accessThisMethodModifiers, modifiersConstant, this);


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
			HeapDereference fieldDereference,
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

		AccessChild accessThisFieldName = new AccessChild(type, fieldDereference, subField, getLocation());

		AnalysisState<A> tmp = state.bottomExecution();

		for (SymbolicExpression allocatedTypeExpr : callState.getExecutionExpressions()) {
			AnalysisState<A> t = analysis.assign(callState, accessThisFieldName, allocatedTypeExpr, this);
			tmp = tmp.lub(t);
		}

		return tmp;

	}

	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> lazyLoadClass(
			Type t,
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {

		if (t instanceof JavaReferenceType jrt)
			t = jrt.getInnerType();

		LoadClass loadClass = new LoadClass(t, getCFG(), getLocation());

		AnalysisState<A> classLoaded = loadClass.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		return classLoaded;
	}

	private Type getNoReferenceType(Type t) {
		Type res = t;
		if (res instanceof JavaReferenceType jrt) {
			res = jrt.getInnerType();
		}
		return res;
	}
}



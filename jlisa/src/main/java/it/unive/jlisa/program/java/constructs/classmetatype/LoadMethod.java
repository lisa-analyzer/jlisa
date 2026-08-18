package it.unive.jlisa.program.java.constructs.classmetatype;

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
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
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
import java.lang.reflect.Modifier;

public class LoadMethod extends UnaryExpression implements PluggableStatement {
	private SyntheticCodeLocationManager synGen;

	private CodeMemberDescriptor methodData;

	protected Statement originating;

	protected LoadMethod(
			CodeMemberDescriptor d,
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "loadMethod", expr);
		methodData = d;
                synGen = new SyntheticCodeLocationManager("internal-load-method-" +
                        methodData.getUnit().getName() + "." + methodData.getName());
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;

	}

	// TODO AP: change this into a unary expression
	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		return loadAndStore(interprocedural, state, expr, null, expressions);
	}

	/**
	 * Loads this method's metaobject and, if {@code destAccessIdx} is
	 * non-{@code null}, stores the resulting reference into it BEFORE
	 * writing any of the metaobject's fields. This matters because storing a
	 * freshly allocated object's reference into a heap-resident array cell
	 * causes the heap domain to re-derive that object's allocation site as
	 * weak (summarized), under a different identifier than the one used at
	 * allocation time; any field written before that point, under the
	 * strong identifier, becomes unreachable (and thus imprecise/top) once
	 * later code reads the method back out of the array. Writing fields
	 * after the array store instead keys them under the identifier that
	 * every later reader (e.g. {@code Method.invoke}) will actually use.
	 */
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> loadAndStore(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			AccessChild destAccessIdx,
			StatementStore<A> expressions)
			throws SemanticException {

		Parameter[] methodParameters = methodData.getFormals();
		int paramCount = methodParameters.length;

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		SymbolicExpression clazz = expr;

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

		// store into the destination array cell BEFORE writing any fields:
		// see this method's Javadoc for why the order matters
		if (destAccessIdx != null)
			methodAllocated = analysis.assign(methodAllocated, destAccessIdx, ref, this);

		HeapDereference derefThisMethod = destAccessIdx != null
				? new HeapDereference(methodMetaType, destAccessIdx, location)
				: new HeapDereference(methodMetaType, method, location);

		// assign method clazz
		AccessChild accessThisMethodClazz = new AccessChild(refClassMetaType, derefThisMethod, clazzVar, location);
		AnalysisState<A> sem = analysis.assign(methodAllocated, accessThisMethodClazz, clazz, this);

		// assign method name
		sem = sem.lub(allocateSubField(interprocedural, methodAllocated, derefThisMethod, nameVar, refStringType,
				expressions));

		AccessChild accessThisMethodName = new AccessChild(refStringType, derefThisMethod, nameVar, location);

		HeapDereference derefMethodName = new HeapDereference(stringType, accessThisMethodName, location);
		AccessChild dst = new AccessChild(stringType, derefMethodName, valueVar, location);

		Constant methodNameConstant = new Constant(stringType, methodData.getName(), location);
		sem = analysis.assign(sem, dst, methodNameConstant, this);

		// assign method type
		Type returnType = methodData.getReturnType();

		AccessChild accessThisMethodType = new AccessChild(refClassMetaType, derefThisMethod, typeVar, location);
		sem = lazyLoadClass(returnType, interprocedural, sem, expressions);

		assert (sem.getExecutionExpressions().size() == 1);
		SymbolicExpression returnTypeClazzVar = sem.getExecutionExpressions().iterator().next();
		sem = analysis.assign(sem, accessThisMethodType, returnTypeClazzVar, this);

		// assign parameter types
		MemoryAllocation arrCreated = new MemoryAllocation(classArrType, synGen.nextLocation(), false);
		HeapReference arrRef = new HeapReference(refClassArrType, arrCreated, location);

		AnalysisState<A> arrAllocated = analysis.smallStepSemantics(sem, arrCreated, this);

		InstrumentedReceiver array = new InstrumentedReceiver(refClassArrType, true, location);
		arrAllocated = analysis.assign(arrAllocated, array, arrRef, this);

		AnalysisState<A> tmp = arrAllocated.bottomExecution();

		HeapDereference arrayDeref = new HeapDereference(classArrType, array, location);

		// FIXME AP: this should really use newArrayWithInitializer. If not,
		// need to initialize the length variable

		// assign length to array
		int subtractReceiver = methodData.isInstance() ? 1 : 0;
		Constant arrLen = new Constant(JavaIntType.INSTANCE, paramCount - subtractReceiver, location);
		AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE, arrayDeref, lengthVar, location);
		sem = analysis.assign(arrAllocated, accessLen, arrLen, this);

		for (int i = 1; i < paramCount; ++i) {

			Parameter parameter = methodParameters[i];

			Type parameterType = parameter.getStaticType();

			Constant idx = new Constant(JavaIntType.INSTANCE, i - 1, location);
			AccessChild accessIdx = new AccessChild(refClassMetaType, arrayDeref, idx, location);

			AnalysisState<A> t = lazyLoadClass(parameterType, interprocedural, sem, expressions);

			assert (t.getExecutionExpressions().size() == 1);
			SymbolicExpression parameterClazzVar = t.getExecutionExpressions().iterator().next();

			sem = analysis.assign(t, accessIdx, parameterClazzVar, this);
		}

		AccessChild accessParameterTypes = new AccessChild(refClassArrType, derefThisMethod, paramTypesVar, location);

		sem = analysis.assign(sem, accessParameterTypes, array, this);
		sem = sem.forgetIdentifier(array, this);

		// assign method modifiers
		boolean isInstance = methodData.isInstance();
		int modifiers = (isInstance) ? 0 : Modifier.STATIC;
		Constant modifiersConstant = new Constant(JavaIntType.INSTANCE, modifiers, location);

		// (*method)->modifiers
		AccessChild accessThisMethodModifiers = new AccessChild(intType, derefThisMethod, modifiersVar, location);

		sem = analysis.assign(sem, accessThisMethodModifiers, modifiersConstant, this);

		tmp = tmp.lub(sem);

		// forgetting `method` rewrites the state, which would drop the
		// primitive fields tracked by the value domain if they were keyed
		// off of it; this is only safe once fields no longer depend on
		// `method` (i.e. once they were written through destAccessIdx
		// instead)
		resultState = destAccessIdx != null
				? tmp.forgetIdentifier(method, this).withExecutionExpression(ref)
				: tmp.withExecutionExpression(ref);

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
			StatementStore<A> expressions)
			throws SemanticException {

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

		tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this);

		return tmp;

	}

	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> lazyLoadClass(
			Type t,
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {

		LoadClass loadClass = new LoadClass(t, getCFG(), getLocation());

		AnalysisState<A> classLoaded = loadClass.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
				expressions);

		return classLoaded;
	}

	private Type getNoReferenceType(
			Type t) {
		Type res = t;
		if (res instanceof JavaReferenceType jrt) {
			res = jrt.getInnerType();
		}
		return res;
	}
}

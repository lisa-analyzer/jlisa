package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
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

public class LoadClass extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("java.lang.LoadClass");

	public LoadClass(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "internal-load-class");
	}

	public static LoadClass build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new LoadClass(cfg, location);
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

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		Type stringType = getProgram().getTypes().getStringType();
		Type classMetaType = JavaClassType.getClassMetaType();
		Type refClassMetaType = new JavaReferenceType(classMetaType);


		// check if class is already loaded

		if (ReflectionCache.isLastClassLoaded()) {
			SymbolicExpression accessClazz = ReflectionCache.getCachedLastClass();
			return analysis.smallStepSemantics(state, accessClazz, this);
		}

		// load clazz in the global variable with the class name
		// TODO AP: call static initializer here
		// FIXME AP: the class could also be a JavaInterfaceType
		// JavaClassType loadingClass = JavaClassType.lookup(loadingClassStr);

		// execute static initializer
		// FIXME: initializer of parent classes is not run, see test class-for-name-1
		// ClassUnit classUnit = (ClassUnit) loadingClass.getUnit();
		// if (classUnit.getCodeMembersByName(loadingClassStr).isEmpty()) {
		// 	Set<CompilationUnit> superClasses = classUnit
		// 			.getImmediateAncestors().stream()
		// 			.filter(u -> u instanceof ClassUnit)
		// 			.collect(Collectors.toSet());
		//
		// 	classUnit = (ClassUnit) superClasses.stream().findFirst().orElse(classUnit);
		// }
		// state = InitializedClassSet.initialize(state, loadingClass.getReference(), this,
		// 		interprocedural);
		// END static initializer

		String clazzName = ReflectionCache.lastClass.toString();
		String internalGlobalVarName = "__" + ReflectionCache.lastClass.toString();

		GlobalVariable clazzVar = new GlobalVariable(refClassMetaType, internalGlobalVarName, getLocation());

		Constant c = new Constant(JavaClassType.getStringType(), clazzName, getLocation());

		AnalysisState<A> callState = allocateClass(interprocedural, state, c, expressions);

		AnalysisState<A> resultState = callState.bottomExecution();

		for (SymbolicExpression allocatedClazz : callState.getExecutionExpressions()) {
			AnalysisState<A> t = analysis.assign(callState, clazzVar, allocatedClazz, this);
			resultState = resultState.lub(t);
		}

		ReflectionCache.cacheLastClass(clazzVar);

		// TODO AP: I think there should be a forgetIdentifiers callState.getExecutionExpressions instead.
		// OR keep the executionExpression but set it to `clazzVar` and don't use the ReflectionCache (this is probably better).
		resultState = resultState.withExecutionExpressions(callState.getExecutionExpressions());

		return resultState;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}


	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> allocateClass(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression clazzName,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type stringType = JavaClassType.getStringType();
		Type classMetaType = JavaClassType.getClassMetaType();

		JavaReferenceType refClassType = new JavaReferenceType(classMetaType);
		JavaReferenceType refStringType = new JavaReferenceType(stringType);

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());


		// allocate the Class object
		MemoryAllocation created = new MemoryAllocation(refClassType.getInnerType(), synGen.nextLocation(), false);
		HeapReference ref = new HeapReference(refClassType, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver clazz = new InstrumentedReceiver(refClassType, false, getLocation());
		AnalysisState<A> clazzAllocated = analysis.assign(allocated, clazz, ref, this);

		HeapDereference derefThisClazz = new HeapDereference(classMetaType, clazz, getLocation());


		// allocate String object for field `name`
		AccessChild accessThisClazzName = new AccessChild(stringType, derefThisClazz, nameVar, location);

		JavaNewObj allocString = new JavaNewObj(getCFG(), synGen.nextLocation(), refStringType, new Expression[0]);

		AnalysisState<A> stringAllocated =
			allocString.forwardSemanticsAux(interprocedural, clazzAllocated, new ExpressionSet[0], expressions);


		AnalysisState<A> tmp = state.bottomExecution();
		for (SymbolicExpression allocatedStringExpr : stringAllocated.getExecutionExpressions()) {
			AnalysisState<A> t = analysis.assign(stringAllocated, accessThisClazzName, allocatedStringExpr, this);
			tmp = tmp.lub(t);
		}

		HeapDereference derefClazzName = new HeapDereference(stringType, accessThisClazzName, location);
		AccessChild accessValue = new AccessChild(stringType, derefClazzName, valueVar, location);

		tmp = analysis.assign(tmp, accessValue, clazzName, this);

		tmp = tmp.withExecutionExpression(clazz);

		return tmp;

	}

}


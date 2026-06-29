package it.unive.jlisa.program.java.constructs.classmetatype;

import java.util.Set;
import java.util.stream.Collectors;

import it.unive.jlisa.frontend.InitializedClassSet;
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
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
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
import it.unive.lisa.type.UnitType;
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

		// if lastClass is not primitive, there may be a static initializer to run
		Type lastClass = ReflectionCache.lastClass;
		if (lastClass instanceof UnitType loadingClazz) {

			// execute static initializer
			// FIXME: initializer of parent classes is not run, see test class-for-name-1
			// ClassUnit classUnit = (ClassUnit) loadingClazz.getUnit();
			// if (classUnit.getCodeMembersByName(loadingClazz.toString()).isEmpty()) {
			// 	Set<CompilationUnit> superClasses = classUnit
			// 			.getImmediateAncestors().stream()
			// 			.filter(u -> u instanceof ClassUnit)
			// 			.collect(Collectors.toSet());
			//
			// 	classUnit = (ClassUnit) superClasses.stream().findFirst().orElse(classUnit);
			// }
			// state = InitializedClassSet.initialize(state, new JavaReferenceType(loadingClazz), this, interprocedural);
		}

		// String clazzName = ReflectionCache.lastClass.toString();
		// String internalGlobalVarName = "__" + ReflectionCache.lastClass.toString();
		//
		// GlobalVariable clazzVar = new GlobalVariable(refClassMetaType, internalGlobalVarName, getLocation());
		//
		// Constant c = new Constant(JavaClassType.getStringType(), clazzName, getLocation());

		AnalysisState<A> callState = allocateClass(interprocedural, state, expressions);

		AnalysisState<A> resultState = callState;

		// for (SymbolicExpression allocatedClazz : callState.getExecutionExpressions()) {
		// 	AnalysisState<A> t = analysis.assign(callState, clazzVar, allocatedClazz, this);
		// 	resultState = resultState.lub(t);
		// }

		// ReflectionCache.cacheLastClass(clazzVar);

		// TODO AP: I think there should be a forgetIdentifiers callState.getExecutionExpressions instead.
		// OR keep the executionExpression but set it to `clazzVar` and don't use the ReflectionCache (this is probably better).
		// resultState = resultState.withExecutionExpressions(callState.getExecutionExpressions());

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
			StatementStore<A> expressions)
			throws SemanticException {

		String clazzNameStr = ReflectionCache.lastClass.toString();

		// class name is always a constant
		Constant clazzName = new Constant(JavaClassType.getStringType(), clazzNameStr, getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type stringType = JavaClassType.getStringType();
		Type classMetaType = JavaClassType.getClassMetaType();

		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);
		JavaReferenceType refStringType = new JavaReferenceType(stringType);

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());


		// allocate the Class object
		MemoryAllocation created = new MemoryAllocation(refClassMetaType.getInnerType(), synGen.nextLocation(), false);
		HeapReference ref = new HeapReference(refClassMetaType, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver clazz = new InstrumentedReceiver(refClassMetaType, false, getLocation());
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


		// assign the Class object to a global variable
		String internalGlobalVarName = "__" + clazzNameStr;

		GlobalVariable clazzVar = new GlobalVariable(refClassMetaType, internalGlobalVarName, getLocation());
		AnalysisState<A> t = analysis.assign(tmp, clazzVar, clazz, this);
		tmp = tmp.lub(t);

		// getMetaVariables().add(clazz);
		tmp = tmp.forgetIdentifier(clazz, this);

		ReflectionCache.cacheLastClass(clazzVar);

		return tmp;
	}

}


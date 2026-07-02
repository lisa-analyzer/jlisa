package it.unive.jlisa.program.java.constructs.classmetatype;

import java.util.Collection;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
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
import it.unive.lisa.program.CompilationUnit;
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

	private Type loadingType;

	public LoadClass(
			Type t,
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "internal-load-class");
		loadingType = t;

		if (loadingType instanceof JavaReferenceType jrt)
			loadingType = jrt.getInnerType();

		if (loadingType instanceof JavaArrayType arrType) {
			if (arrType.getBaseType() instanceof JavaReferenceType baseType) {
				Type baseTypeNoRef = baseType.getInnerType();
				Type newArrType = arrType = JavaArrayType.lookup(baseTypeNoRef, arrType.getDimensions());
				loadingType = newArrType;
			}
		}
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
		CodeLocation location = getLocation();

		// check if class is already loaded
		if (ReflectionCache.isClassLoaded(loadingType)) {
			SymbolicExpression accessClazz = ReflectionCache.getCachedClass(loadingType);
			return analysis.smallStepSemantics(state, accessClazz, this);
		}


		// if lastClass is not primitive, there may be a static initializer to run
		// FIXME AP: move this in class.forName
		if (loadingType instanceof UnitType loadingClazz) {
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


		AnalysisState<A> callState = allocateClass(interprocedural, state, expressions);

		AnalysisState<A> tmp = callState;

		if (loadingType instanceof UnitType loadingClazz) {

			Collection<CompilationUnit> ancestors = loadingClazz.getUnit().getImmediateAncestors();

			// find the superclass
			for (CompilationUnit ancestor : ancestors) {
				Type superClass = null;
				try {
					superClass = JavaClassType.lookup(ancestor.getName());
				}
				catch(Exception e) {}

				if (superClass != null) {
					LoadClass loadClass = new LoadClass(superClass, getCFG(), location);
					tmp = loadClass.forwardSemanticsAux(interprocedural, tmp, new ExpressionSet[0], expressions);

					GlobalVariable superClassVar =
						new GlobalVariable(Untyped.INSTANCE, "superClass", location);

					assert(tmp.getExecutionExpressions().size() == 1);
					SymbolicExpression superClazz = tmp.getExecutionExpressions().iterator().next();
					assert(callState.getExecutionExpressions().size() == 1);
					SymbolicExpression currentClazz = callState.getExecutionExpressions().iterator().next();

					HeapDereference derefClazz = new HeapDereference(JavaClassType.getClassMetaType(), currentClazz, location);

					AccessChild accessSuperClazz = new AccessChild(new JavaReferenceType(JavaClassType.getClassMetaType()), derefClazz, superClassVar, location);

					tmp = tmp.lub(analysis.assign(tmp, accessSuperClazz, superClazz, this));
				}
			}
		}

		return tmp.withExecutionExpressions(callState.getExecutionExpressions());
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

		String clazzNameStr = loadingType.toString();

		// class name is always a constant
		Constant clazzName = new Constant(JavaClassType.getStringType(), clazzNameStr, getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type stringType = JavaClassType.getStringType();
		Type classMetaType = JavaClassType.getClassMetaType();

		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);
		JavaReferenceType refStringType = new JavaReferenceType(stringType);

		GlobalVariable isArrayVar = new GlobalVariable(Untyped.INSTANCE, "isArray", location);
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);


		// allocate the Class object
		MemoryAllocation created = new MemoryAllocation(refClassMetaType.getInnerType(), synGen.nextLocation(), false);
		HeapReference ref = new HeapReference(refClassMetaType, created, location);

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver clazz = new InstrumentedReceiver(refClassMetaType, false, location);
		AnalysisState<A> clazzAllocated = analysis.assign(allocated, clazz, ref, this);

		HeapDereference derefThisClazz = new HeapDereference(classMetaType, clazz, location);

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

		// assign the isArray field
		AccessChild accessIsArray = new AccessChild(JavaBooleanType.INSTANCE, derefThisClazz, isArrayVar, location);
		Constant isArrayConstant = new Constant(JavaBooleanType.INSTANCE, loadingType instanceof JavaArrayType, location);

		AnalysisState<A> assigned = analysis.assign(tmp, accessIsArray, isArrayConstant, this);
		tmp = tmp.lub(assigned);


		// assign the Class object to a global variable
		String internalGlobalVarName = "__" + clazzNameStr;

		GlobalVariable clazzVar = new GlobalVariable(refClassMetaType, internalGlobalVarName, getLocation());
		AnalysisState<A> t = analysis.assign(tmp, clazzVar, clazz, this);
		tmp = tmp.lub(t);

		tmp = tmp.forgetIdentifier(clazz, this);

		tmp = tmp.withExecutionExpression(clazzVar);

		ReflectionCache.cacheLoadedClass(loadingType, clazzVar);

		return tmp;
	}

}


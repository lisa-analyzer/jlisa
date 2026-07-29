package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.ReflectionDataUtils;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.JavaNewArray;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
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
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class InternalInitClassMetaObject extends UnaryExpression implements PluggableStatement {

        private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("CacheReflectionData");

	protected Statement originating;
	private Type initializingClassType;

	public InternalInitClassMetaObject(
			CFG cfg,
			CodeLocation location,
			Type t,
			Expression expr) {
		super(cfg, location, "internalInitClassMetaObject", expr);
		initializingClassType = t;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression param,
			StatementStore<A> expressions)
			throws SemanticException {

		assert (ReflectionDataUtils.isClassLoaded(state, initializingClassType, getLocation()));

		SymbolicExpression clazz = param;

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		AnalysisState<A> noExceptionState = state.bottomExecution();

		AnalysisState<A> tmp = state;

                if (!ReflectionDataUtils.isClassReflectionDataCached(interprocedural, tmp, clazz, this)) {

			if (initializingClassType instanceof UnitType ut) {
				AnalysisState<A> fieldsLoaded = loadGlobals(interprocedural, tmp, expressions,
						getAllFields(ut.getUnit()), clazz);

				AnalysisState<A> methodsLoaded = loadMethods(interprocedural, fieldsLoaded, expressions,
						getAllMethods(ut.getUnit()), clazz);

				AnalysisState<A> superclassesInit = initializeSuperclasses(interprocedural, methodsLoaded, expressions,
						clazz, ut);

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

	Collection<Global> getAllFields(
			CompilationUnit unit) {
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
                JavaReferenceType refFieldArrType = new JavaReferenceType(fieldArrType);

		GlobalVariable declaredFieldsVar = new GlobalVariable(Untyped.INSTANCE, "declaredFields", getLocation());

		AnalysisState<A> tmp = state;

		HeapDereference derefClazz = new HeapDereference(classMetaType, clazz, location);
		AccessChild accessFields = new AccessChild(new JavaReferenceType(fieldArrType), derefClazz, declaredFieldsVar,
				location);

                // allocate the field array
		IntLiteral zero = new IntLiteral(getCFG(), location, 0);
                Constant c = new Constant(JavaIntType.INSTANCE, globals.size(), location);
		JavaNewArray newArr = new JavaNewArray(getCFG(), synGen.nextLocation(), zero, refFieldArrType);

		AnalysisState<A> fieldsAllocated = newArr.fwdUnarySemantics(interprocedural, tmp, c, expressions);
		for (SymbolicExpression expr : fieldsAllocated.getExecutionExpressions()) {
			tmp = analysis.assign(fieldsAllocated, accessFields, expr, this);
		}
		tmp = tmp.forgetIdentifiers(newArr.getMetaVariables(), this);

		HeapDereference derefArr = new HeapDereference(fieldArrType, accessFields, location);

		int nextIdx = 0;
		for (Global global : globals) {

			Constant idx = new Constant(JavaIntType.INSTANCE, nextIdx, location);

			AccessChild accessIdx = new AccessChild(wrappedFieldType, derefArr, idx, getLocation());

			LoadField loadField = new LoadField(global, getCFG(), getLocation(), this);

			AnalysisState<A> t = loadField.fwdUnarySemantics(interprocedural, tmp, clazz, expressions);

			// assign initialized field to the next index of the array
			for (SymbolicExpression initializedField : t.getExecutionExpressions()) {
				tmp = analysis.assign(t, accessIdx, initializedField, this);
			}

			++nextIdx;
		}

		return tmp;
	}

	Collection<CodeMember> getAllMethods(
			CompilationUnit unit) {

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
                JavaReferenceType refMethodArrType = new JavaReferenceType(methodArrType);

		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", getLocation());
		GlobalVariable declaredMethodsVar = new GlobalVariable(Untyped.INSTANCE, "declaredMethods", getLocation());

		HeapDereference derefClazz = new HeapDereference(classMetaType, clazz, location);
		AccessChild accessMethods = new AccessChild(new JavaReferenceType(methodArrType), derefClazz,
				declaredMethodsVar, location);

                AnalysisState<A> tmp = state;

                IntLiteral zero = new IntLiteral(getCFG(), location, 0);
                Constant c = new Constant(JavaIntType.INSTANCE, methods.size(), location);
                JavaNewArray newArr = new JavaNewArray(getCFG(), synGen.nextLocation(), zero, refMethodArrType);

                AnalysisState<A> methodsAllocated = newArr.fwdUnarySemantics(interprocedural, tmp, c, expressions);
                for (SymbolicExpression expr : methodsAllocated.getExecutionExpressions()) {
                        tmp = analysis.assign(methodsAllocated, accessMethods, expr, this);
                }
                tmp = tmp.forgetIdentifiers(newArr.getMetaVariables(), this);

		HeapDereference derefArr = new HeapDereference(methodArrType, accessMethods, location);

		int nextIdx = 0;
		for (CodeMember method : methods) {

			Constant idx = new Constant(JavaIntType.INSTANCE, nextIdx, location);

			AccessChild accessIdx = new AccessChild(wrappedMethodType, derefArr, idx, getLocation());

			LoadMethod loadMethod = new LoadMethod(method.getDescriptor(), getCFG(), getLocation(), this);

			AnalysisState<A> t = loadMethod.fwdUnarySemantics(interprocedural, tmp, clazz, expressions);

			// assign initialized method to the next index of the array
			for (SymbolicExpression initializedMethod : t.getExecutionExpressions()) {
				tmp = analysis.assign(t, accessIdx, initializedMethod, this);
			}

			++nextIdx;

		}

		return tmp;

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

		// we removed the interfaces, we know there is just one superclass
		assert (ancestors.size() == 1);
		CompilationUnit superclassUnit = ancestors.iterator().next();

		assert (superclassUnit instanceof ClassUnit);

		JavaClassType superclassType = JavaClassType.lookup(superclassUnit.getName());

		GlobalVariable superclassVar = new GlobalVariable(Untyped.INSTANCE, "superClass", location);

		HeapDereference derefClazz = new HeapDereference(classMetaType, clazz, location);

		// ancestor clazz Class object
		AccessChild accessSuperclass = new AccessChild(refClassMetaType, derefClazz, superclassVar, location);

		AnalysisState<A> tmp = state;

		// initialize the class if not already initialized
                if (!ReflectionDataUtils.isClassReflectionDataCached(interprocedural, tmp, accessSuperclass, this)) {

			SymbolicExpression expr = new HeapReference(refClassMetaType, accessSuperclass, location);

			AnalysisState<A> fieldsLoaded = loadGlobals(interprocedural, state, expressions,
					getAllFields(superclassUnit), expr);

			AnalysisState<A> methodsLoaded = loadMethods(interprocedural, fieldsLoaded, expressions,
					getAllMethods(superclassUnit), expr);

			tmp = methodsLoaded;
		}

		return initializeSuperclasses(interprocedural, tmp, expressions, accessSuperclass, superclassType);
	}

}

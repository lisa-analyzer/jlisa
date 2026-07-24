package it.unive.jlisa.program.java.constructs.classmetatype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import it.unive.jlisa.program.LoadedClassSet;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.JavaNewArray;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.InterfaceUnit;
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
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;

public class LoadClass extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("java.lang.LoadClass");

	private Type loadingType;

	// NOTE: this is technically a duplicate of loadingType.toString(), except in one case:
	// A$B (nested classes). In that case (only reachable via forName), the type is:
	// A.B, but the name is A$B
	private String loadingClazzName;

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

		loadingClazzName = loadingType.toString();
	}

	public LoadClass(
			Type t,
			String clazzName,
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

		loadingClazzName = clazzName;
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

		if (LoadedClassSet.isClassLoaded(state, loadingType)) {
			SymbolicExpression accessClazz = LoadedClassSet.getLoadedClassHandle(loadingType, location);
			return analysis.smallStepSemantics(state, accessClazz, this);
		}

		AnalysisState<A> callState = allocateClass(interprocedural, state, expressions);

		AnalysisState<A> tmp = callState;

		// TODO if loadingType instanceof JavaArrayType, then the superclass shall
		// be java.lang.Object

		if (loadingType instanceof UnitType loadingClazz) {

			SymbolicExpression currentClazz = callState.getExecutionExpressions().iterator().next();
			HeapDereference derefClazz = new HeapDereference(JavaClassType.getClassMetaType(), currentClazz, location);

			Collection<CompilationUnit> ancestors = loadingClazz.getUnit().getImmediateAncestors();

			ArrayList<ExpressionSet> loadedInterfaces = new ArrayList<>();

			// find the superclass
			for (CompilationUnit ancestor : ancestors) {

				// found a superclass
				if (ancestor instanceof ClassUnit) {

					JavaClassType superClass = JavaClassType.lookup(ancestor.getName());

					LoadClass loadClass = new LoadClass(superClass, getCFG(), location);
					tmp = loadClass.forwardSemanticsAux(interprocedural, tmp, new ExpressionSet[0], expressions);

					GlobalVariable superClassVar =
						new GlobalVariable(Untyped.INSTANCE, "superClass", location);

					assert(tmp.getExecutionExpressions().size() == 1);
					SymbolicExpression superClazz = tmp.getExecutionExpressions().iterator().next();

					AccessChild accessSuperClazz = new AccessChild(new JavaReferenceType(JavaClassType.getClassMetaType()), derefClazz, superClassVar, location);

					tmp = analysis.assign(tmp, accessSuperClazz, superClazz, this);
				}
				else if (ancestor instanceof InterfaceUnit) {
					// load interface Class

					JavaInterfaceType interf = JavaInterfaceType.lookup(ancestor.getName());

					LoadClass loadClass = new LoadClass(interf, getCFG(), location);
					tmp = loadClass.forwardSemanticsAux(interprocedural, tmp, new ExpressionSet[0], expressions);

					loadedInterfaces.add(tmp.getExecutionExpressions());
				}
			}

			// if we loaded interfaces, assign them to the `interfaces` array of the current Class
			if (!loadedInterfaces.isEmpty()) {

				GlobalVariable interfacesVar = new GlobalVariable(Untyped.INSTANCE, "interfaces", location);
				GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);


				JavaReferenceType refArrType = JavaArrayType.CLASS_ARRAY;
				JavaReferenceType refClassType = new JavaReferenceType(JavaClassType.getClassMetaType());

				AccessChild accessInterfaces = new AccessChild(refArrType, derefClazz, interfacesVar, location);
				HeapDereference derefInterfaces = new HeapDereference(refArrType.getInnerType(), accessInterfaces, location);

				AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE, derefInterfaces, lengthVar, location);

				// update the length of the array
				Constant c = new Constant(JavaIntType.INSTANCE, loadedInterfaces.size(), location);
				tmp = analysis.assign(tmp, accessLen, c, this);

				int i = 0;
				// assign the single values
				for(ExpressionSet loadedInterfaceAllocs : loadedInterfaces) {

					Constant idxConstant = new Constant(JavaIntType.INSTANCE, i, location);

					for (SymbolicExpression loadedInterface : loadedInterfaceAllocs) {
						AccessChild accessIdx = new AccessChild(refClassType, derefInterfaces, idxConstant, location);

						tmp = analysis.assign(tmp, accessIdx, loadedInterface, this);
					}

					++i;
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

		// class name is always a constant
		Constant clazzName = new Constant(JavaClassType.getStringType(), loadingClazzName, getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type stringType = JavaClassType.getStringType();
		Type classMetaType = JavaClassType.getClassMetaType();

		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);
		JavaReferenceType refStringType = new JavaReferenceType(stringType);
		JavaReferenceType refClassArray = JavaArrayType.CLASS_ARRAY;

		Type fieldType = JavaClassType.getFieldMetaType();
		Type refFieldType = new JavaReferenceType(fieldType);
		JavaArrayType fieldArray = JavaArrayType.lookup(refFieldType, 1);
		JavaReferenceType refFieldArray = new JavaReferenceType(fieldArray);

		JavaReferenceType refMethodArray = new JavaReferenceType(JavaArrayType.lookup(new JavaReferenceType(JavaClassType.getMethodType()), 1));

		GlobalVariable isArrayVar = new GlobalVariable(Untyped.INSTANCE, "isArray", location);
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		GlobalVariable superClassVar = new GlobalVariable(Untyped.INSTANCE, "superClass", location);
		GlobalVariable interfacesVar = new GlobalVariable(Untyped.INSTANCE, "interfaces", location);
		GlobalVariable declaredFieldsVar = new GlobalVariable(Untyped.INSTANCE, "declaredFields", location);
		GlobalVariable declaredMethodsVar = new GlobalVariable(Untyped.INSTANCE, "declaredMethods", location);


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
		tmp = tmp.forgetIdentifiers(allocString.getMetaVariables(), this);

		// assign the isArray field
		AccessChild accessIsArray = new AccessChild(JavaBooleanType.INSTANCE, derefThisClazz, isArrayVar, location);
		Constant isArrayConstant = new Constant(JavaBooleanType.INSTANCE, loadingType instanceof JavaArrayType, location);

		AnalysisState<A> assigned = analysis.assign(tmp, accessIsArray, isArrayConstant, this);
		tmp = tmp.lub(assigned);

		// assign the superClass field with null
		AccessChild accessSuperClass = new AccessChild(refClassMetaType, derefThisClazz, superClassVar, location);
		NullConstant nc = new NullConstant(location);
		Set<Type> types = new HashSet<>();
		types.add(refClassMetaType);
		TypeTokenType typeToken = new TypeTokenType(types);

		Constant castTo = new Constant(typeToken, refClassMetaType, location);

		BinaryExpression castAs = new BinaryExpression(refClassMetaType, nc, castTo, TypeCast.INSTANCE, location);

		tmp = analysis.assign(tmp, accessSuperClass, castAs, this);


		// assign the array of interfaces that the class implements.
		// Default is an empty array
		AccessChild accessInterfaces = new AccessChild(refClassArray, derefThisClazz, interfacesVar, location);

		IntLiteral len = new IntLiteral(getCFG(), location, 0);
		Constant c = new Constant(JavaIntType.INSTANCE, 0, location);
		JavaNewArray newArr = new JavaNewArray(getCFG(), synGen.nextLocation(), len, refClassArray);


		AnalysisState<A> interfacesAllocated = newArr.fwdUnarySemantics(interprocedural, tmp, c, expressions);
		for (SymbolicExpression expr : interfacesAllocated.getExecutionExpressions()) {
			tmp = analysis.assign(interfacesAllocated, accessInterfaces, expr, this);
		}

		tmp = tmp.forgetIdentifiers(newArr.getMetaVariables(), this);

		// allocate an empty array of fields
		AccessChild accessDeclaredFields = new AccessChild(refFieldArray, derefThisClazz, declaredFieldsVar, location);

		IntLiteral zero = new IntLiteral(getCFG(), location, 0);
		c = new Constant(JavaIntType.INSTANCE, 0, location);
		newArr = new JavaNewArray(getCFG(), synGen.nextLocation(), zero, refFieldArray);

		AnalysisState<A> fieldsAllocated = newArr.fwdUnarySemantics(interprocedural, tmp, c, expressions);
		for (SymbolicExpression expr : fieldsAllocated.getExecutionExpressions()) {
			tmp = analysis.assign(fieldsAllocated, accessDeclaredFields, expr, this);
		}
		tmp = tmp.forgetIdentifiers(newArr.getMetaVariables(), this);


		// allocate an empty array of methods
		AccessChild accessDeclaredMethods = new AccessChild(refMethodArray, derefThisClazz, declaredMethodsVar, location);

		newArr = new JavaNewArray(getCFG(), synGen.nextLocation(), zero, refMethodArray);

		AnalysisState<A> methodsAllocated = newArr.fwdUnarySemantics(interprocedural, tmp, c, expressions);
		for (SymbolicExpression expr : methodsAllocated.getExecutionExpressions()) {
			tmp = analysis.assign(methodsAllocated, accessDeclaredMethods, expr, this);
		}
		tmp = tmp.forgetIdentifiers(newArr.getMetaVariables(), this);


		// assign the Class object to a global variable
		String internalGlobalVarName = "__" + loadingClazzName;

		GlobalVariable clazzVar = new GlobalVariable(refClassMetaType, internalGlobalVarName, getLocation());
		AnalysisState<A> t = analysis.assign(tmp, clazzVar, clazz, this);
		tmp = tmp.lub(t);

		tmp = tmp.forgetIdentifier(clazz, this);

		tmp = tmp.withExecutionExpression(clazzVar);

		tmp = LoadedClassSet.addLoadedClass(tmp, interprocedural, loadingType);

		return tmp;
	}

}


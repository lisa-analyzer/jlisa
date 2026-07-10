package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.GhostTypeLookupOperator;
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
import it.unive.lisa.program.Global;
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
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class LoadField extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("java.lang.reflect.Field");

	private Global fieldData;

	protected LoadField(
			Global g,
			CFG cfg,
			CodeLocation location,
			Expression[] subExpressions) {
		super(cfg, location, "loadField", subExpressions);
		fieldData = g;
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

		Type thisFieldType = fieldData.getStaticType();
		if (thisFieldType instanceof JavaReferenceType jrt)
			thisFieldType = jrt.getInnerType();

		assert(params.length == 4);

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

		Type intType = JavaIntType.INSTANCE;
		Type stringType = getProgram().getTypes().getStringType();
		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType refFieldMetaType = new JavaReferenceType(fieldMetaType);
		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);
		JavaReferenceType refStringType = new JavaReferenceType(stringType);

		GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", getLocation());
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
		GlobalVariable typeVar = new GlobalVariable(Untyped.INSTANCE, "type", getLocation());
		GlobalVariable modifiersVar = new GlobalVariable(Untyped.INSTANCE, "modifiers", getLocation());
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());

		AnalysisState<A> resultState = state.bottomExecution();


		MemoryAllocation created = new MemoryAllocation(fieldMetaType, synGen.nextLocation(), false);
		HeapReference ref = new HeapReference(refFieldMetaType, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver field = new InstrumentedReceiver(refFieldMetaType, false, synGen.nextLocation());
		AnalysisState<A> fieldAllocated = analysis.assign(allocated, field, ref, this);

		HeapDereference derefThisField = new HeapDereference(fieldMetaType, field, getLocation());


		AnalysisState<A> tmp = fieldAllocated.bottomExecution();

		// assign field clazz
		AccessChild accessThisFieldClazz = new AccessChild(refClassMetaType, derefThisField, clazzVar, getLocation());

		AnalysisState<A> sem = analysis.assign(fieldAllocated, accessThisFieldClazz, exprs[0], this);

		// assign field name
		sem = allocateSubField(interprocedural, sem, derefThisField, nameVar, refStringType, expressions);

		AccessChild accessThisFieldName = new AccessChild(refStringType, derefThisField, nameVar, getLocation());

		HeapDereference derefFieldName = new HeapDereference(stringType, accessThisFieldName, getLocation());
		AccessChild dst = new AccessChild(stringType, derefFieldName, valueVar, getLocation());

		sem = analysis.assign(sem, dst, exprs[1], this);


		// assign field type
		AccessChild accessThisFieldType = new AccessChild(refClassMetaType, derefThisField, typeVar, getLocation());
		sem = lazyLoadClass(fieldData.getStaticType(), interprocedural, sem, expressions);

		assert(sem.getExecutionExpressions().size() == 1);
		SymbolicExpression fieldClazzExpr = sem.getExecutionExpressions().iterator().next();
		sem = analysis.assign(sem, accessThisFieldType, fieldClazzExpr, this);

		// assign field modifiers
		AccessChild accessThisFieldModifiers = new AccessChild(intType, derefThisField, modifiersVar, getLocation());
		sem = analysis.assign(sem, accessThisFieldModifiers, exprs[3], this);

		tmp = tmp.lub(sem);

		resultState = tmp.withExecutionExpression(field);

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

		LoadClass loadClass = new LoadClass(t, getCFG(), getLocation());

		AnalysisState<A> classLoaded = loadClass.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		return classLoaded;
	}

}

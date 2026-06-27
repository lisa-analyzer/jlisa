package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.SyntheticCodeLocation;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.jlisa.program.java.constructs.string.StringCopyConstructor;
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
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class InternalClassConstructorWithName extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	// private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("java.lang.reflect.Field");

	public InternalClassConstructorWithName(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "Class");
	}

	public static InternalClassConstructorWithName build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new InternalClassConstructorWithName(cfg, location);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
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

		assert(params.length == 1);
		SymbolicExpression param = params[0].iterator().next();

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type stringType = JavaClassType.getStringType();
		Type classMetaType = JavaClassType.getClassMetaType();

		JavaReferenceType refClassType = new JavaReferenceType(classMetaType);
		JavaReferenceType refStringType = new JavaReferenceType(stringType);

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());


		// allocate the Class object
		MemoryAllocation created = new MemoryAllocation(refClassType.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(refClassType, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver clazz = new InstrumentedReceiver(refClassType, false, getLocation());
		AnalysisState<A> clazzAllocated = analysis.assign(allocated, clazz, ref, this);

		HeapDereference derefThisClazz = new HeapDereference(classMetaType, clazz, getLocation());

		AnalysisState<A> tmp = state.bottomExecution();

		AccessChild accessThisClazzName = new AccessChild(stringType, derefThisClazz, nameVar, location);

		// allocate String object for field `name`
		// StringCopyConstructor call = new StringCopyConstructor(getCFG(), location, null, null);
		// AnalysisState<A> callState =
		// 	call.fwdBinarySemantics(interprocedural, clazzAllocated, accessThisClazzName, param, expressions);

		// HeapDereference derefClazzName = new HeapDereference(stringType, accessThisClazzName, location);
		// AccessChild accessStringValue = new AccessChild(stringType, derefClazzName, valueVar, location);

		clazzAllocated = analysis.assign(clazzAllocated, accessThisClazzName, param, this);

		tmp = clazzAllocated.withExecutionExpression(clazz);

		return tmp;
	}
}


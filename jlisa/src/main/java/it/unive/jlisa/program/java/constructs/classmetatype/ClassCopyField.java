package it.unive.jlisa.program.java.constructs.classmetatype;

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
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
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

public class ClassCopyField extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {

	protected Statement originating;

	public ClassCopyField(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "copyField", JavaClassType.getFieldMetaType(), expr);
	}

	public static ClassCopyField build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassCopyField(cfg, location, params[0]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type intType = JavaIntType.INSTANCE;
		JavaReferenceType refStringType = new JavaReferenceType(JavaClassType.getStringType());
		Type fieldMetaType = JavaClassType.getFieldMetaType();
		JavaReferenceType refFieldMetaType = new JavaReferenceType(fieldMetaType);
		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);

		GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", location);
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable typeVar = new GlobalVariable(Untyped.INSTANCE, "type", location);
		GlobalVariable modifiersVar = new GlobalVariable(Untyped.INSTANCE, "modifiers", location);

		AnalysisState<A> result = state.bottomExecution();

		// allocate a new Field
		MemoryAllocation created = new MemoryAllocation(fieldMetaType, getLocation(), false);
		HeapReference ref = new HeapReference(refFieldMetaType, created, location);

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver field = new InstrumentedReceiver(refFieldMetaType, false, getLocation());
		AnalysisState<A> fieldAllocated = analysis.assign(allocated, field, ref, this);

		HeapDereference derefThisField = new HeapDereference(fieldMetaType, field, location);
		HeapDereference derefOther = new HeapDereference(fieldMetaType, expr, location);

		// shallow copy clazz
		result = copyField(analysis, fieldAllocated, derefOther, derefThisField, clazzVar, refClassMetaType,
				expressions);

		// shallow copy name
		result = result.lub(copyField(analysis, fieldAllocated, derefOther, derefThisField, nameVar, refStringType,
				expressions));

		// shallow copy fieldType
		result = result.lub(copyField(analysis, fieldAllocated, derefOther, derefThisField, typeVar, refClassMetaType,
				expressions));

		// copy modifiers
		result = result.lub(
				copyField(analysis, fieldAllocated, derefOther, derefThisField, modifiersVar, intType, expressions));

		return result.forgetIdentifier(field, this).withExecutionExpression(ref);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> copyField(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			HeapDereference source,
			HeapDereference dst,
			GlobalVariable var,
			Type t,
			StatementStore<A> expressions)
			throws SemanticException {

		SymbolicExpression ref = new AccessChild(t, source, var, getLocation());
		if (t instanceof JavaReferenceType)
			ref = new HeapReference(t, ref, getLocation());

		AccessChild accessDst = new AccessChild(t, dst, var, getLocation());

		AnalysisState<A> tmp = analysis.assign(state, accessDst, ref, this);
		return tmp;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

}

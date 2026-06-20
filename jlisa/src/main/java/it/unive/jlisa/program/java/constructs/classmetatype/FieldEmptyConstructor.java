package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.SyntheticCodeLocation;
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
import it.unive.lisa.program.SourceCodeLocation;
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

public class FieldEmptyConstructor extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("java.lang.reflect.Field");

	public FieldEmptyConstructor(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "Field");
	}

	public static FieldEmptyConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FieldEmptyConstructor(cfg, location);
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

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		String src = ((SourceCodeLocation) getLocation()).getSourceFile();

		Type stringType = getProgram().getTypes().getStringType();
		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type classMetaType = JavaClassType.getClassMetaType();

		JavaReferenceType refType = new JavaReferenceType(fieldMetaType);

		GlobalVariable fieldClazz = new GlobalVariable(Untyped.INSTANCE, "clazz", getLocation());
		GlobalVariable fieldName = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
		GlobalVariable fieldType = new GlobalVariable(Untyped.INSTANCE, "type", getLocation());

		// allocate the field object
		MemoryAllocation created = new MemoryAllocation(refType.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(refType, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver field = new InstrumentedReceiver(refType, false, getLocation());
		AnalysisState<A> fieldAllocated = analysis.assign(allocated, field, ref, this);

		HeapDereference derefThisField = new HeapDereference(fieldMetaType, field, getLocation());

		AnalysisState<A> tmp = state.bottomExecution();

		// FIXME: using syntheticCodeLocations like this causes multiple
		// allocations of
		// `Field` objects to reside in the same 3 locations.
		// Switch to syntheticCodeLocationManager if this is the correct
		// approach

		// allocate Class object for field `clazz`
		SyntheticCodeLocation s1 = synGen.nextLocation();
		JavaNewObj call1 = new JavaNewObj(getCFG(), s1,
				new JavaReferenceType(classMetaType),
				new Expression[0]);
		AnalysisState<A> callState1 = call1.forwardSemanticsAux(interprocedural, fieldAllocated, new ExpressionSet[0],
				expressions);

		// assign this->clazz to the newly allocated Class object
		AccessChild accessThisFieldClazz = new AccessChild(classMetaType, derefThisField, fieldClazz, getLocation());

		for (SymbolicExpression allocatedClazzExpr : callState1.getExecutionExpressions()) {
			AnalysisState<A> t = analysis.assign(callState1, accessThisFieldClazz, allocatedClazzExpr, this);
			tmp = tmp.lub(t);
		}

		// allocate String object for field `name`
		SyntheticCodeLocation s2 = synGen.nextLocation();
		JavaNewObj call2 = new JavaNewObj(getCFG(), s2,
				new JavaReferenceType(stringType),
				new Expression[0]);
		AnalysisState<
				A> callState2 = call2.forwardSemanticsAux(interprocedural, tmp, new ExpressionSet[0], expressions);

		// assign this->name to the newly allocated String object
		AccessChild accessThisFieldName = new AccessChild(stringType, derefThisField, fieldName, getLocation());

		for (SymbolicExpression allocatedNameExpr : callState2.getExecutionExpressions()) {
			AnalysisState<A> t = analysis.assign(callState2, accessThisFieldName, allocatedNameExpr, this);
			tmp = tmp.lub(t);
		}

		// allocate Class object for field `type`
		SyntheticCodeLocation s3 = synGen.nextLocation();
		JavaNewObj call3 = new JavaNewObj(getCFG(), s3,
				new JavaReferenceType(classMetaType),
				new Expression[0]);
		AnalysisState<
				A> callState3 = call3.forwardSemanticsAux(interprocedural, tmp, new ExpressionSet[0], expressions);

		// assign this->type to the newly allocated Class object
		AccessChild accessThisFieldType = new AccessChild(classMetaType, derefThisField, fieldType, getLocation());

		for (SymbolicExpression allocatedTypeExpr : callState3.getExecutionExpressions()) {
			AnalysisState<A> t = analysis.assign(callState3, accessThisFieldType, allocatedTypeExpr, this);
			tmp = tmp.lub(t);
		}

		tmp = tmp.withExecutionExpressions(fieldAllocated.getExecutionExpressions());

		return tmp;
	}
}

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

		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type classMetaType = JavaClassType.getClassMetaType();

		JavaReferenceType refType = new JavaReferenceType(fieldMetaType);

		GlobalVariable fieldType = new GlobalVariable(Untyped.INSTANCE, "type", getLocation());

		// allocate the field object
		MemoryAllocation created = new MemoryAllocation(refType.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(refType, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver field = new InstrumentedReceiver(refType, false, getLocation());
		AnalysisState<A> fieldAllocated = analysis.assign(allocated, field, ref, this);

		HeapDereference derefThisField = new HeapDereference(fieldMetaType, field, getLocation());

		AnalysisState<A> tmp = state.bottomExecution();

		// allocate Class object for field `type`
		SyntheticCodeLocation s1 = synGen.nextLocation();
		JavaNewObj call = new JavaNewObj(getCFG(), s1,
				new JavaReferenceType(classMetaType),
				new Expression[0]);
		AnalysisState<
				A> callState = call.forwardSemanticsAux(interprocedural, fieldAllocated, new ExpressionSet[0],
						expressions);

		// assign this->type to the newly allocated Class object
		AccessChild accessThisFieldType = new AccessChild(classMetaType, derefThisField, fieldType, getLocation());

		for (SymbolicExpression allocatedTypeExpr : callState.getExecutionExpressions()) {
			AnalysisState<A> t = analysis.assign(callState, accessThisFieldType, allocatedTypeExpr, this);
			tmp = tmp.lub(t);
		}

		tmp = tmp.withExecutionExpressions(fieldAllocated.getExecutionExpressions());
		tmp = tmp.forgetIdentifier(field, this);

		return tmp;
	}
}

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
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class MethodEmptyConstructor extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	private static SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager("java.lang.reflect.Method");

	public MethodEmptyConstructor(
			CFG cfg,
			CodeLocation location,
			Expression exp) {
		super(cfg, location, "Method", exp);
	}

	public static MethodEmptyConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MethodEmptyConstructor(cfg, location, params[0]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		Type stringType = getProgram().getTypes().getStringType();
		Type methodMetaType = JavaClassType.getMethodType();
		Type classMetaType = JavaClassType.getClassMetaType();

		JavaReferenceType refType = new JavaReferenceType(methodMetaType);

		GlobalVariable methodReturnType = new GlobalVariable(Untyped.INSTANCE, "returnType", getLocation());

		// allocate the method object
		MemoryAllocation created = new MemoryAllocation(refType.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(refType, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver method = new InstrumentedReceiver(refType, false, getLocation());
		AnalysisState<A> methodAllocated = analysis.assign(allocated, method, ref, this);

		HeapDereference derefThisMethod = new HeapDereference(methodMetaType, method, getLocation());

		// allocate Class object for field `returnType`
		SyntheticCodeLocation s1 = synGen.nextLocation();
		JavaNewObj call1 = new JavaNewObj(getCFG(), s1,
				new JavaReferenceType(classMetaType),
				new Expression[0]);
		AnalysisState<
				A> callState1 = call1.forwardSemanticsAux(interprocedural, methodAllocated, new ExpressionSet[0],
						expressions);

		// assign this->returnType to the newly allocated Class object
		AccessChild accessThisMethodReturnType = new AccessChild(classMetaType, derefThisMethod, methodReturnType,
				getLocation());

		AnalysisState<A> tmp = callState1.bottomExecution();

		for (SymbolicExpression allocatedTypeExpr : callState1.getExecutionExpressions()) {
			AnalysisState<A> t = analysis.assign(callState1, accessThisMethodReturnType, allocatedTypeExpr, this);
			tmp = tmp.lub(t);
		}

		tmp = tmp.withExecutionExpressions(methodAllocated.getExecutionExpressions());
		return tmp;
	}
}

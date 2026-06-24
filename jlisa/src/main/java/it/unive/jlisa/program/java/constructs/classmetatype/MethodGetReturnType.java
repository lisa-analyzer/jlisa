package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.type.JavaClassType;
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
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class MethodGetReturnType extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public MethodGetReturnType(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "getReturnType", new JavaReferenceType(JavaClassType.getClassMetaType()), expr);
	}

	public static MethodGetReturnType build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MethodGetReturnType(cfg, location, params[0]);
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

		Type methodMetaType = JavaClassType.getFieldMetaType();
		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType reftype = new JavaReferenceType(classMetaType);

		GlobalVariable returnTypeVar = new GlobalVariable(Untyped.INSTANCE, "returnType", getLocation());

		// (*method)->returnType
		HeapDereference derefThisMethod = new HeapDereference(methodMetaType, expr, getLocation());
		AccessChild accessThisMethodReturnType = new AccessChild(reftype, derefThisMethod, returnTypeVar, getLocation());

		HeapReference ref = new HeapReference(reftype, accessThisMethodReturnType, getLocation());

		return analysis.smallStepSemantics(state, ref, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}


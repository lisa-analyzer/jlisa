package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.type.JavaArrayType;
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

public class MethodGetParameterTypes extends it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public MethodGetParameterTypes(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "getParameterTypes", new JavaReferenceType(JavaClassType.getClassMetaType()), expr);
	}

	public static MethodGetParameterTypes build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MethodGetParameterTypes(cfg, location, params[0]);
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

		Type methodMetaType = JavaClassType.getMethodType();
		JavaReferenceType reftype = JavaArrayType.CLASS_ARRAY;

		GlobalVariable paramTypesVar = new GlobalVariable(Untyped.INSTANCE, "parameterTypes", getLocation());

		// (*method)->parameterTypes
		HeapDereference derefThisMethod = new HeapDereference(methodMetaType, expr, getLocation());
		AccessChild accessThisMethodParamTypes = new AccessChild(reftype, derefThisMethod, paramTypesVar,
				getLocation());

		HeapReference ref = new HeapReference(reftype, accessThisMethodParamTypes, getLocation());

		return analysis.smallStepSemantics(state, ref, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

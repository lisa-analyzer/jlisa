package it.unive.jlisa.program.java.constructs.throwable;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class ThrowableGetMessage extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public ThrowableGetMessage(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "getMessage", expr);
	}

	public static it.unive.jlisa.program.java.constructs.throwable.ThrowableGetMessage build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new it.unive.jlisa.program.java.constructs.throwable.ThrowableGetMessage(cfg, location, params[0]);
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
			SymbolicExpression expression,
			StatementStore<A> expressions)
			throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		JavaReferenceType reftype = (JavaReferenceType) new JavaReferenceType(stringType);
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		// the message stored by the constructor of the throwable
		GlobalVariable msgVar = new GlobalVariable(Untyped.INSTANCE, "message", getLocation());
		HeapDereference deref = new HeapDereference(stringType, expression, getLocation());
		AccessChild msgAccess = new AccessChild(stringType, deref, msgVar, getLocation());

		// allocate the string
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(), reftype,
				new Expression[0]);
		AnalysisState<
				A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		AnalysisState<A> tmp = state.bottomExecution();
		for (SymbolicExpression ref : callState.getExecutionExpressions()) {
			AccessChild access = new AccessChild(stringType, ref, var, getLocation());
			AnalysisState<A> sem = analysis.assign(callState, access, msgAccess, context, this);
			tmp = tmp.lub(sem);
		}

		getMetaVariables().addAll(call.getMetaVariables());
		return tmp.withExecutionExpressions(callState.getExecutionExpressions());
	}
}

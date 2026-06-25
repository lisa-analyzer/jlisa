package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;

public class FieldGetValue extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public FieldGetValue(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "get", left, right);
	}

	public static FieldGetValue build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FieldGetValue(cfg, location, params[0], params[1]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		Global field = ReflectionCache.lastField;
		if (field == null)
			return state.topExecution();

		Type objectType = JavaClassType.getObjectType();
		CodeLocation loc = getLocation();

		if (field.isInstance()) {
			HeapDereference container = new HeapDereference(objectType, right, loc);
			GlobalVariable var = field.toSymbolicVariable(loc);
			AccessChild access = new AccessChild(field.getStaticType(), container, var, loc);
			if (field.getStaticType().isPointerType())
				return analysis.smallStepSemantics(state, new HeapReference(field.getStaticType(), access, loc), this);
			return analysis.smallStepSemantics(state, access, this);
		}

		GlobalVariable access = field.toSymbolicVariable(loc);
		if (field.getStaticType().isPointerType())
			return analysis.smallStepSemantics(state, new HeapReference(field.getStaticType(), access, loc), this);
		return analysis.smallStepSemantics(state, access, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

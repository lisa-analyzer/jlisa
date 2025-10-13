package it.unive.jlisa.program.java.constructs.stringbuilder;

import it.unive.jlisa.program.operator.JavaStringInsertFloatOperator;
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
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringBuilderInsertFloat extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringBuilderInsertFloat(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "insert", left, middle, right);
	}

	public static StringBuilderInsertFloat build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringBuilderInsertFloat(cfg, location, params[0], params[1], params[2]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdTernarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefLeft = new HeapDereference(stringType, left, getLocation());
		AccessChild accessLeft = new AccessChild(stringType, derefLeft, var, getLocation());

		it.unive.lisa.symbolic.value.TernaryExpression insert = new it.unive.lisa.symbolic.value.TernaryExpression(
				stringType, accessLeft, middle, right, JavaStringInsertFloatOperator.INSTANCE, getLocation());
		AccessChild leftAccess = new AccessChild(stringType, left, var, getLocation());
		AnalysisState<A> result = interprocedural.getAnalysis().assign(state, leftAccess, insert, originating);

		return analysis.smallStepSemantics(result, left, originating);
	}
}

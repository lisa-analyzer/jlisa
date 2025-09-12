package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.operator.JavaStringLastIndexOfCharFromIndexOperator;
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

public class StringLastIndexOfCharFromIndex extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	protected StringLastIndexOfCharFromIndex(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "replaceAll", left, middle, right);
	}

	public static StringLastIndexOfCharFromIndex build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringLastIndexOfCharFromIndex(cfg, location, params[0], params[1], params[2]);
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

		it.unive.lisa.symbolic.value.TernaryExpression indexOf = new it.unive.lisa.symbolic.value.TernaryExpression(
				getProgram().getTypes().getStringType(),
				accessLeft,
				middle,
				right,
				JavaStringLastIndexOfCharFromIndexOperator.INSTANCE,
				getLocation());

		return analysis.smallStepSemantics(state, indexOf, originating);

	}
}

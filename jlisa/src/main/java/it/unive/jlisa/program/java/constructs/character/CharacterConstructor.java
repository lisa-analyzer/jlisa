package it.unive.jlisa.program.java.constructs.character;

import it.unive.jlisa.program.type.JavaCharType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Untyped;

public class CharacterConstructor extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public CharacterConstructor(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "Character", left, right);
	}

	public static CharacterConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CharacterConstructor(cfg, location, params[0], params[1]);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0; 
	}


	@Override
	public void setOriginatingStatement(Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> fwdBinarySemantics(InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state, SymbolicExpression left, SymbolicExpression right, StatementStore<A> expressions)
			throws SemanticException {
		 GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
         AccessChild access = new AccessChild(JavaCharType.INSTANCE, left, var, getLocation());
         return state.assign(access, right, originating);
	}
}

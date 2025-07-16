package it.unive.jlisa.program.java.constructs.reader.pushbackreader;

import it.unive.lisa.analysis.AbstractState;
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
import it.unive.lisa.symbolic.value.PushAny;

public class PushbackReaderConstructorWithSize extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public PushbackReaderConstructorWithSize(CFG cfg, CodeLocation location, Expression left, Expression middle, Expression right) {
		super(cfg, location, "PushbackReader", left, middle, right);
	}

	public static PushbackReaderConstructorWithSize build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new PushbackReaderConstructorWithSize(cfg, location, params[0], params[1], params[2]);
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
	public <A extends AbstractState<A>> AnalysisState<A> fwdTernarySemantics(InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state, SymbolicExpression left, SymbolicExpression middle, SymbolicExpression right,
			StatementStore<A> expressions) throws SemanticException {
		return state.smallStepSemantics(new PushAny(getStaticType(), getLocation()), originating);
	}
}


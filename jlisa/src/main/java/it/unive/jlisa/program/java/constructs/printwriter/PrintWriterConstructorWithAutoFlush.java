package it.unive.jlisa.program.java.constructs.printwriter;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.PushAny;

public class PrintWriterConstructorWithAutoFlush extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public PrintWriterConstructorWithAutoFlush(CFG cfg, CodeLocation location) {
		super(cfg, location, "PrintWriter");
	}

	public static PrintWriterConstructorWithAutoFlush build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new PrintWriterConstructorWithAutoFlush(cfg, location);
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
	public <A extends AbstractState<A>> AnalysisState<A> forwardSemanticsAux(InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state, ExpressionSet[] params, StatementStore<A> expressions) throws SemanticException {
		return state.smallStepSemantics(new PushAny(getStaticType(), getLocation()), originating);
	}
}

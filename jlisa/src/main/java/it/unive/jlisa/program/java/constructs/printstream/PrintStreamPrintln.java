package it.unive.jlisa.program.java.constructs.printstream;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.VoidType;

public class PrintStreamPrintln extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public PrintStreamPrintln(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "println", JavaClassType.getPrintStreamType());
	}

	public static PrintStreamPrintln build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new PrintStreamPrintln(cfg, location);
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
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					ExpressionSet[] params,
					StatementStore<A> expressions)
					throws SemanticException {
		// nothing to do
		return interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(VoidType.INSTANCE, getLocation()),
				originating);
	}
}

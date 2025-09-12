package it.unive.jlisa.program.java.constructs.printstream;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.PushAny;

public class PrintStreamConstructor extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public PrintStreamConstructor(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "PrintStream", JavaClassType.lookup("PrintStream", null));
	}

	public static PrintStreamConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new PrintStreamConstructor(cfg, location);
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
		return interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(getStaticType(), getLocation()),
				originating);
	}
}

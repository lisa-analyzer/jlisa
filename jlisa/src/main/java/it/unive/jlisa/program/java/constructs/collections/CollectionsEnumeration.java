package it.unive.jlisa.program.java.constructs.collections;

import it.unive.jlisa.program.type.*;
import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;

public class CollectionsEnumeration
		extends
		UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public CollectionsEnumeration(
			CFG cfg,
			CodeLocation location,
			Expression arg) {
		super(cfg, location, "enumeration", arg);
	}

	public static CollectionsEnumeration build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CollectionsEnumeration(cfg, location, params[0]);
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
			SymbolicExpression arg,
			StatementStore<A> expressions)
			throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(
				state,
				new PushAny(new JavaReferenceType(JavaClassType.lookup("java.util.Enumeration")), getLocation()),
				originating);
	}
}

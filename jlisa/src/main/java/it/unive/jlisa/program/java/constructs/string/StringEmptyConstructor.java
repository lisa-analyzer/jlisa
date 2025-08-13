package it.unive.jlisa.program.java.constructs.string;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringEmptyConstructor extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringEmptyConstructor(CFG cfg, CodeLocation location, Expression exp) {
		super(cfg, location, "String", exp);
	}

	public static StringEmptyConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringEmptyConstructor(cfg, location, params[0]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression expr,
			StatementStore<A> expressions) throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		Constant emptyString = new Constant(stringType, "", getLocation());

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		AccessChild access = new AccessChild(stringType, expr, var, getLocation());
		return interprocedural.getAnalysis().assign(state, access, emptyString, originating);
	}
}

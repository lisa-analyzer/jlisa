package it.unive.jlisa.program.java.constructs.character;

import it.unive.jlisa.program.operator.JavaCharacterIsJavaIdentifierPartOperator;
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
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;

public class CharacterIsJavaIdentifierStart extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public CharacterIsJavaIdentifierStart(CFG cfg, CodeLocation location, Expression arg) {
		super(cfg, location, "isJavaIdentifierStart", arg);
	}

	public static CharacterIsJavaIdentifierStart build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CharacterIsJavaIdentifierStart(cfg, location, params[0]);
	}

	@Override
	public void setOriginatingStatement(Statement st) {
		originating = st;
	}


	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression expr,
			StatementStore<A> expressions) throws SemanticException {
		UnaryExpression isJavaId = new UnaryExpression(
				getProgram().getTypes().getBooleanType(), 
				expr, 
				JavaCharacterIsJavaIdentifierPartOperator.INSTANCE, 
				getLocation());
		
		return interprocedural.getAnalysis().smallStepSemantics(state, isJavaId, originating);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}
}

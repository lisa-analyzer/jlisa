package it.unive.jlisa.program.java.constructs.character;

import it.unive.jlisa.program.operator.JavaCharacterEqualsOperator;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
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
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Untyped;

public class CharacterEquals extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public CharacterEquals(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "equals", left, right);
	}

	public static CharacterEquals build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CharacterEquals(cfg, location, params[0], params[1]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression left,
			SymbolicExpression right, StatementStore<A> expressions) throws SemanticException {
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefLeft = new HeapDereference(JavaCharType.INSTANCE, left, getLocation());
		AccessChild accessLeft = new AccessChild(JavaCharType.INSTANCE, derefLeft, var, getLocation());

		HeapDereference derefRight = new HeapDereference(JavaCharType.INSTANCE, right, getLocation());
		AccessChild accessRight = new AccessChild(JavaCharType.INSTANCE, derefRight, var, getLocation());
		
		it.unive.lisa.symbolic.value.BinaryExpression equalsExpr = new it.unive.lisa.symbolic.value.BinaryExpression(
				getProgram().getTypes().getBooleanType(), 
				accessLeft, 
				accessRight, 
				JavaCharacterEqualsOperator.INSTANCE, 
				getLocation());
		return interprocedural.getAnalysis().smallStepSemantics(state, equalsExpr, originating);
	}
}

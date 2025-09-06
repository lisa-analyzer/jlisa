package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.operator.JavaStringIndexOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfOperator;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
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
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringIndexOf extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringIndexOf(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "indexOf", left, right);
	}

	public static StringIndexOf build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringIndexOf(cfg, location, params[0], params[1]);
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
		Type stringType = getProgram().getTypes().getStringType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefLeft = new HeapDereference(stringType, left, getLocation());
		AccessChild accessLeft = new AccessChild(stringType, derefLeft, var, getLocation());

		it.unive.lisa.symbolic.value.BinaryExpression indexOf = null;
		
		if(right.getStaticType() instanceof JavaCharType) {

			indexOf = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaIntType.INSTANCE, 
					accessLeft, 
					right, 
					JavaStringIndexOfCharOperator.INSTANCE, 
					getLocation());
		}
		else if(right.getStaticType().asPointerType().getInnerType().isStringType() ) {
			HeapDereference derefRight = new HeapDereference(stringType, right, getLocation());
			AccessChild accessRight = new AccessChild(stringType, derefRight, var, getLocation());

			
			indexOf = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaIntType.INSTANCE, 
					accessLeft, 
					accessRight, 
					JavaStringIndexOfOperator.INSTANCE, 
					getLocation());
		}
		
		

		return analysis.smallStepSemantics(state, indexOf, originating);
	}
}

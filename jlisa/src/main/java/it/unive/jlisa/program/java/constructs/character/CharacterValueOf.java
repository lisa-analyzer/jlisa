package it.unive.jlisa.program.java.constructs.character;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;


public class CharacterValueOf extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public CharacterValueOf(CFG cfg, CodeLocation location, Expression arg) {
		super(cfg, location, "ValueOf", arg);
	}

	public static CharacterValueOf build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CharacterValueOf(cfg, location, params[0]);
	}

	@Override
	public void setOriginatingStatement(Statement st) {
		originating = st;
	}


	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression expr,
			StatementStore<A> expressions) throws SemanticException {
		Type charType = JavaCharType.INSTANCE;
		
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
			
		// allocate the character
		JavaNewObj call = new JavaNewObj(getCFG(), 
				(SourceCodeLocation) getLocation(),
				"Chararcter",  
				new JavaReferenceType(charType),
				new Expression[0]);
		
		AnalysisState<A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		AnalysisState<A> tmp = state.bottom();
		for (SymbolicExpression ref : callState.getComputedExpressions()) {
			AccessChild access = new AccessChild(charType, ref, var, getLocation());
			AnalysisState<A> sem = interprocedural.getAnalysis().assign(callState, access, expr, this);
			tmp = tmp.lub(sem);
		}

		getMetaVariables().addAll(call.getMetaVariables());		
		return new AnalysisState<>(
				tmp.getState(),
				callState.getComputedExpressions(),
				tmp.getFixpointInformation());	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}
}

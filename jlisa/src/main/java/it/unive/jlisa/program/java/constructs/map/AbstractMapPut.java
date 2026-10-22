package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;

public class AbstractMapPut extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public AbstractMapPut(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "put", left, right);
	}

	public static AbstractMapPut build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new AbstractMapPut(cfg, location, params[0], params[1]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		JavaClassType oobExc = JavaClassType.getUnsupportedOperationExceptionType();
		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
				oobExc.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
				new StatementStore<A>(state));

		AnalysisState<A> exceptionState = state.bottomExecution();

		for (SymbolicExpression th : state.getExecutionExpressions()) {
			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), oobExc.getReference(), getLocation());
			AnalysisState<A> tmp = analysis.assign(state, throwVar, th, originating);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
					.forgetIdentifiers(getLeft().getMetaVariables(), this)
					.forgetIdentifiers(getRight().getMetaVariables(), this);
			exceptionState = exceptionState.lub(analysis.moveExecutionToError(tmp.withExecutionExpression(throwVar),
					new Error(oobExc.getReference(), originating), this));
		}

		return exceptionState;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

}

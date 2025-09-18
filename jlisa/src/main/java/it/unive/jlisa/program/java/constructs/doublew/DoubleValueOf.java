package it.unive.jlisa.program.java.constructs.doublew;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
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
import it.unive.lisa.type.Type;

public class DoubleValueOf extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public DoubleValueOf(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "valueOf", expr);
	}

	public static DoubleValueOf build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new DoubleValueOf(cfg, location, params[0]);
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
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		Type doubleType = JavaClassType.lookup("java.lang.Double");
		JavaReferenceType reftype = (JavaReferenceType) new JavaReferenceType(doubleType);

		// allocate the value
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(), "Double", reftype,
				new Expression[] { getSubExpression() });
		ExpressionSet set = new ExpressionSet(expr);
		AnalysisState<A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[] { set },
				expressions);
		getMetaVariables().addAll(call.getMetaVariables());
		return callState;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

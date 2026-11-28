package it.unive.jlisa.springed.cfg;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.PushAny;
import org.apache.commons.lang3.StringUtils;

public class JavaLambda extends NaryExpression {

	private final CFG lambdaCfg;

	public JavaLambda(
			CFG enclosingCfg,
			CodeLocation location,
			CFG lambdaCfg,
			Expression... outerValues) {
		super(enclosingCfg, location, "lambda", outerValues);
		this.lambdaCfg = lambdaCfg;
	}

	public CFG getLambdaCfg() {
		return lambdaCfg;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		String lambdaName = this.lambdaCfg.getDescriptor().getName();
		String otherLambdaName = ((JavaLambda) o).getLambdaCfg().getDescriptor().getName();

		return lambdaName.compareTo(otherLambdaName);
	}

	@Override
	public String toString() {
		return this.lambdaCfg.getDescriptor().getName()
				+ " holding {" + StringUtils.join(getSubExpressions(), ", ") + "}";
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {

		PushAny placeholder = new PushAny(getStaticType(), getLocation());
		ScopeId<A> context = interprocedural.getCurrentContext();
		AnalysisState<A> newState = interprocedural.getAnalysis()
				.smallStepSemantics(state, placeholder, context, this);

		return newState;
	}
}

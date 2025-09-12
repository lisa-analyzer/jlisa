package it.unive.jlisa.program.cfg.expression;

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
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.MetaVariableCreator;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.type.Type;

public class PrefixSubtraction extends UnaryExpression implements MetaVariableCreator {
	public PrefixSubtraction(
			CFG cfg,
			CodeLocation location,
			Expression subExpression) {
		super(cfg, location, "--", subExpression);
	}

	@Override
	public Identifier getMetaVariable() {
		Expression e = getSubExpression();
		String name = "ret_value@" + this.getLocation();
		Variable var = new Variable(e.getStaticType(), name, getLocation());
		return var;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		if (analysis.getRuntimeTypesOf(state, expr, this).stream().noneMatch(Type::isNumericType))
			return state.bottom();

		state = analysis.assign(
				state,
				expr,
				new BinaryExpression(
						getStaticType(),
						expr,
						new Constant(JavaIntType.INSTANCE, 1, getLocation()),
						NumericNonOverflowingSub.INSTANCE,
						getLocation()),
				this);
		// state = state.assign(meta, expr, this);
		return analysis.smallStepSemantics(state, expr, this);

	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return getConstructName() + getSubExpression();
	}
}

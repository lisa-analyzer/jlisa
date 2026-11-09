package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.operator.JavaArithmeticOperators;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
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
import it.unive.lisa.type.Type;

public class PrefixAddition extends UnaryExpression implements MetaVariableCreator {
	public PrefixAddition(
			CFG cfg,
			CodeLocation location,
			Expression subExpression) {
		super(cfg, location, "++", subExpression);
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
		ScopeId<A> context = interprocedural.getCurrentContext();
		if (analysis.getRuntimeTypesOf(state, expr, context, this).stream().noneMatch(Type::isNumericType))
			return state.bottomExecution();
		Type type = JavaArithmeticOperators.operandType(analysis.getRuntimeTypesOf(state, expr, context, this));
		if (type.isUntyped())
			type = getStaticType();

		state = analysis.assign(
				state,
				expr,
				// the result is narrowed to the type of the value
				JavaArithmeticOperators.narrowed(analysis.getRuntimeTypesOf(state, expr, context, this),
						new BinaryExpression(
								type,
								expr,
								new Constant(JavaIntType.INSTANCE, 1, getLocation()),
								JavaArithmeticOperators.add(type),
								getLocation()),
						getLocation()),
				context, this);

		return analysis.smallStepSemantics(state, expr, context, this);

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

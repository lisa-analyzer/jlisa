package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaShortType;
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
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import java.util.Collections;

/**
 * The increment and decrement operators of Java ({@code ++} and {@code --},
 * prefix and postfix): the variable becomes {@code x + 1} or {@code x - 1},
 * converted back to its type (JLS 15.14.2, 15.15.1), and the value of the
 * expression is the new value (prefix) or the old one (postfix).
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
abstract class JavaIncrementOrDecrement extends UnaryExpression implements MetaVariableCreator {

	private final BinaryOperator operator;

	private final boolean prefix;

	/**
	 * Builds the operator.
	 *
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where this expression is defined
	 * @param constructName {@code ++} or {@code --}
	 * @param operator      the addition or the subtraction of 1
	 * @param prefix        whether the operator comes before the variable
	 *                          ({@code ++x}) or after it ({@code x++})
	 * @param subExpression the incremented or decremented variable
	 */
	JavaIncrementOrDecrement(
			CFG cfg,
			CodeLocation location,
			String constructName,
			BinaryOperator operator,
			boolean prefix,
			Expression subExpression) {
		super(cfg, location, constructName, subExpression);
		this.operator = operator;
		this.prefix = prefix;
	}

	@Override
	public Identifier getMetaVariable() {
		Expression e = getSubExpression();
		String name = "ret_value@" + this.getLocation();
		return new Variable(e.getStaticType(), name, getLocation());
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

		if (prefix) {
			state = analysis.assign(state, expr, next(expr), context, this);
			return analysis.smallStepSemantics(state, expr, context, this);
		}

		// the value of x++ is the one of x before the increment
		Identifier meta = getMetaVariable();
		state = analysis.assign(state, meta, expr, context, this);
		state = analysis.assign(state, expr, next(expr), context, this);
		getMetaVariables().add(meta);
		return analysis.smallStepSemantics(state, meta, context, this);
	}

	// x + 1 or x - 1, converted back to the type of x when it is byte, short or
	// char (e.g., the char 65535 incremented is 0, not 65536)
	private SymbolicExpression next(
			SymbolicExpression expr) {
		CodeLocation location = getLocation();
		Type type = expr.getStaticType();
		Constant one = new Constant(JavaIntType.INSTANCE, 1, location);
		if (!(type instanceof JavaByteType || type instanceof JavaShortType || type instanceof JavaCharType))
			return new BinaryExpression(type, expr, one, operator, location);
		// byte, short and char are promoted to int
		BinaryExpression result = new BinaryExpression(JavaIntType.INSTANCE, expr, one, operator, location);
		Constant token = new Constant(new TypeTokenType(Collections.singleton(type)), type, location);
		return new BinaryExpression(type, result, token, TypeConv.INSTANCE, location);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return Boolean.compare(prefix, ((JavaIncrementOrDecrement) o).prefix);
	}

	@Override
	public String toString() {
		return prefix ? getConstructName() + getSubExpression() : getSubExpression() + getConstructName();
	}
}

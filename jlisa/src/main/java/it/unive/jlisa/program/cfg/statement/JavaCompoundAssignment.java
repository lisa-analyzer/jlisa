package it.unive.jlisa.program.cfg.statement;

import it.unive.jlisa.program.cfg.expression.JavaBitwiseAnd;
import it.unive.jlisa.program.cfg.expression.JavaBitwiseExclusiveOr;
import it.unive.jlisa.program.cfg.expression.JavaBitwiseOr;
import it.unive.jlisa.program.cfg.expression.JavaDivision;
import it.unive.jlisa.program.cfg.expression.JavaShiftLeft;
import it.unive.jlisa.program.cfg.expression.JavaShiftRight;
import it.unive.jlisa.program.cfg.expression.JavaUnsignedShiftRight;
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
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.numeric.Modulo;
import it.unive.lisa.program.cfg.statement.numeric.Multiplication;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A compound assignment ({@code +=}, {@code -=}, {@code <<=}...). Java
 * evaluates its left operand only once (JLS 15.26.2): the left and the right
 * operands are evaluated, they are combined through the operation of the
 * compound assignment, and the result is assigned to the left operand after the
 * implicit conversion to its type.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaCompoundAssignment
		extends
		BinaryExpression {

	/**
	 * Builds the compound assignment.
	 *
	 * @param cfg           the {@link CFG} where this expression lies
	 * @param location      the location where this expression is defined
	 * @param constructName the operator of this expression, e.g. {@code +=}
	 * @param left          the left operand, which is also assigned
	 * @param right         the right operand
	 */
	public JavaCompoundAssignment(
			CFG cfg,
			CodeLocation location,
			String constructName,
			Expression left,
			Expression right) {
		super(cfg, location, constructName, left.getStaticType(), left, right);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> state,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions)
			throws SemanticException {
		// the left operand is evaluated once, then the right one
		AnalysisState<A> leftState = getLeft().forwardSemantics(state, interprocedural, expressions);
		expressions.put(getLeft(), leftState);
		AnalysisState<A> rightState = getRight().forwardSemantics(leftState, interprocedural, expressions);
		expressions.put(getRight(), rightState);

		// the operation and the assignment, on every pair of values of the
		// operands
		AnalysisState<A> result = rightState.bottomExecution();
		for (SymbolicExpression left : leftState.getExecutionExpressions())
			for (SymbolicExpression right : rightState.getExecutionExpressions())
				result = result.lub(fwdBinarySemantics(interprocedural, rightState, left, right, expressions));

		// the meta variables of the operands are moved to this expression
		for (Expression sub : getSubExpressions()) {
			getMetaVariables().addAll(sub.getMetaVariables());
			sub.getMetaVariables().clear();
		}
		return result;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		// the operation and the assignment are built on the fly to reuse their
		// semantics on the values of the operands, which are already computed.
		// They do not change the parent of the operands, since a parent is set
		// only once. This expression is their parent, hence the errors they
		// raise (e.g., division by zero) are raised by this expression
		BinaryExpression op = operation();
		op.setParentStatement(this);
		JavaAssignment assignment = new JavaAssignment(getCFG(), getLocation(), getLeft(), op);
		assignment.setParentStatement(this);

		AnalysisState<A> value = op.fwdBinarySemantics(interprocedural, state, left, right, expressions);
		AnalysisState<A> result = value.bottomExecution();
		for (SymbolicExpression v : value.getExecutionExpressions())
			result = result.lub(assignment.fwdBinarySemantics(interprocedural, value, left, v, expressions));
		return result;
	}

	/**
	 * Returns the operation combining the two operands, built on the operands
	 * of this expression (e.g., the division for {@code /=}).
	 *
	 * @return the operation
	 */
	private BinaryExpression operation() {
		CFG cfg = getCFG();
		CodeLocation location = getLocation();
		Expression left = getLeft();
		Expression right = getRight();
		switch (getConstructName()) {
		case "+=":
			return new JavaAddition(cfg, location, left, right);
		case "-=":
			return new JavaSubtraction(cfg, location, left, right);
		case "*=":
			return new Multiplication(cfg, location, left, right);
		case "/=":
			return new JavaDivision(cfg, location, left, right);
		case "%=":
			return new Modulo(cfg, location, left, right);
		case "&=":
			return new JavaBitwiseAnd(cfg, location, left, right);
		case "|=":
			return new JavaBitwiseOr(cfg, location, left, right);
		case "^=":
			return new JavaBitwiseExclusiveOr(cfg, location, left, right);
		case "<<=":
			return new JavaShiftLeft(cfg, location, left, right);
		case ">>=":
			return new JavaShiftRight(cfg, location, left, right);
		case ">>>=":
			return new JavaUnsignedShiftRight(cfg, location, left, right);
		default:
			throw new IllegalArgumentException("Unknown compound assignment " + getConstructName());
		}
	}
}

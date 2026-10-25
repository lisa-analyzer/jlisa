package it.unive.jlisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A compound assignment ({@code +=}, {@code -=}, {@code <<=}...). Its left
 * operand is evaluated once, combined with its right operand through the given
 * operation, and the result is assigned to the left operand after the implicit
 * conversion to its type. The operation and the assignment are not nodes of the
 * CFG, and they share the location of this expression.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaCompoundAssignment
		extends
		BinaryExpression {

	/**
	 * The operation combining the two operands.
	 */
	private final BinaryExpression operation;

	/**
	 * The assignment of the result of the operation to the left operand.
	 */
	private final JavaAssignment assignment;

	/**
	 * Builds the compound assignment.
	 *
	 * @param cfg           the {@link CFG} where this expression lies
	 * @param location      the location where this expression is defined
	 * @param constructName the operator of this expression, e.g. {@code +=}
	 * @param operation     the operation combining the two operands, whose left
	 *                          and right operands are the ones of this
	 *                          expression
	 */
	public JavaCompoundAssignment(
			CFG cfg,
			CodeLocation location,
			String constructName,
			BinaryExpression operation) {
		this(cfg, location, constructName, operation,
				new JavaAssignment(cfg, location, operation.getLeft(), operation));
	}

	private JavaCompoundAssignment(
			CFG cfg,
			CodeLocation location,
			String constructName,
			BinaryExpression operation,
			JavaAssignment assignment) {
		super(cfg, location, constructName, operation.getLeft().getStaticType(), operation.getLeft(),
				operation.getRight());
		this.operation = operation;
		this.assignment = assignment;
		// the operation and the assignment are not nodes of the CFG, hence this
		// expression must be their root statement. This permits to move to the
		// callers the errors they raise.
		operation.setParentStatement(this);
		assignment.setParentStatement(this);
	}

	/**
	 * Returns the operation combining the two operands.
	 *
	 * @return the operation
	 */
	public BinaryExpression getOperation() {
		return operation;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return operation.compareTo(((JavaCompoundAssignment) o).operation);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		AnalysisState<A> value = operation.fwdBinarySemantics(interprocedural, state, left, right, expressions);
		AnalysisState<A> result = value.bottomExecution();
		for (SymbolicExpression v : value.getExecutionExpressions())
			result = result.lub(assignment.fwdBinarySemantics(interprocedural, value, left, v, expressions));
		return result;
	}
}

package it.unive.jlisa.checkers;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.cfg.statement.asserts.AssertStatement;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.allocations.AllocationSites;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Assert Checker It checks whether an assertion's condition might not hold,
 * and whether the main method might terminate with an uncaught runtime
 * exception. All warnings are possible, and report the violation they refer
 * to: they can be confirmed or refuted by the verification of warnings
 * performed after the checks, if a verifier has been selected. For uncaught
 * runtime exceptions, the checker also records the statement raising the
 * exception ({@link #runtimeExceptionTarget(Message)}), the target of the
 * violation witness of a confirmed warning. The checker can be restricted to
 * one of the two kinds of violations (e.g., when only one property is of
 * interest), skipping the checks of the other kind.
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class AssertChecker<V extends ValueLattice<V>>
		implements
		SemanticCheck<
				SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>> {
	private static final Logger LOG = LogManager.getLogger(AssertChecker.class);

	private static final String POSSIBLE_FAILURE = "the assertion might not hold";

	/**
	 * The statements raising the runtime exceptions reported by the warnings
	 * of this checker, for each exit point of a main method and each
	 * context (in textual form) it has been analyzed in.
	 */
	private final Map<Statement, Map<String, Statement>> exceptionTargets = new ConcurrentHashMap<>();

	private final boolean assertions;

	private final boolean runtimeExceptions;

	/**
	 * Builds the checker, checking both failing assertions and uncaught
	 * runtime exceptions.
	 */
	public AssertChecker() {
		this(true, true);
	}

	/**
	 * Builds the checker, checking only the given kinds of violations.
	 *
	 * @param assertions        whether or not failing assertions are checked
	 * @param runtimeExceptions whether or not uncaught runtime exceptions are
	 *                              checked
	 */
	public AssertChecker(
			boolean assertions,
			boolean runtimeExceptions) {
		this.assertions = assertions;
		this.runtimeExceptions = runtimeExceptions;
	}

	/**
	 * Yields the statement raising the runtime exception reported by the given
	 * warning of this checker, that is the target of its violation witness.
	 * 
	 * @param warning the warning
	 * 
	 * @return the statement, or nothing if the warning is not about an
	 *             uncaught runtime exception, or if the statement could not be
	 *             determined
	 */
	public Optional<Statement> runtimeExceptionTarget(
			Message warning) {
		if (!(warning instanceof StatementMessage))
			return Optional.empty();
		Map<String, Statement> targets = exceptionTargets.get(((StatementMessage) warning).getStatement());
		if (targets == null)
			return Optional.empty();
		return Optional.ofNullable(targets.get(String.valueOf(warning.getContext())));
	}

	@Override
	public boolean visit(
			SemanticTool<
					SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>> tool,
			CFG graph,
			Statement node) {

		// RuntimeException property checker
		if (runtimeExceptions && graph.getProgram().getEntryPoints().contains(graph)
				&& graph.getAllExitpoints().contains(node))
			try {
				checkRuntimeException(tool, graph, node);
			} catch (SemanticException e) {
				e.printStackTrace();
			}

		// assert checker
		if (assertions && node instanceof AssertStatement)
			try {
				checkAssert(tool, graph, (AssertStatement) node);
			} catch (SemanticException e) {
				e.printStackTrace();
			}

		return true;
	}

	private void checkRuntimeException(
			SemanticTool<
					SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>> tool,
			CFG graph,
			Statement node)
			throws SemanticException {

		for (var result : tool.getResultOf(graph)) {
			// errors caught by the error edges leaving the node (e.g., a throw
			// inside a try block) do not escape the method
			AnalysisState<
					SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>> state = tool
							.getAnalysis().removeCaughtErrors(result.getAnalysisStateAfter(node), node);

			// checking if there exists at least one exception state, ignoring
			// the AssertionErrors of failed assertions (reported by the
			// assert checker)
			boolean hasExceptionState = !state.getErrors().isBottom() &&
					!state.getErrors().isTop() &&
					state.getErrors().function.keySet().stream().anyMatch(e -> !isAssertionError(e.getType())) ||
					(!state.getSmashedErrors().isBottom() &&
							!state.getSmashedErrors().isTop() &&
							state.getSmashedErrors().function.keySet().stream().anyMatch(t -> !isAssertionError(t)));

			// if exceptions might have been thrown, we raise a possible warning.
			// Its violation concerns exceptions only: errors (e.g., the
			// AssertionError of a failed assertion) are not runtime
			// exceptions, and they can neither confirm nor prevent the
			// refutation of the warning. Checked exceptions also match
			// Exception, while thrown values whose static type is a supertype
			// of Exception (e.g., Throwable) do not
			if (hasExceptionState) {
				tool.warnOn(node, result.getId(),
						it.unive.lisa.checks.verification.Violation.error(JavaClassType.getExceptionType().getReference()),
						"uncaught runtime exception in main method");
				Optional<Statement> target = RuntimeExceptionTarget.of(tool, state);
				if (target.isPresent())
					exceptionTargets.computeIfAbsent(node, n -> new ConcurrentHashMap<>())
							.put(String.valueOf(result.getId()), target.get());
			}
		}
	}

	private static boolean isAssertionError(
			Type type) {
		return type.canBeAssignedTo(JavaClassType.getAssertionErrorType().getReference());
	}

	private void checkAssert(
			SemanticTool<
					SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>> tool,
			CFG graph,
			AssertStatement node)
			throws SemanticException {
		Expression expr;
		if (node instanceof SimpleAssert)
			expr = ((SimpleAssert) node).getSubExpression();
		else if (node instanceof AssertionStatement)
			expr = ((AssertionStatement) node).getLeft();
		else
			return;
		boolean isAssertFalse = expr.toString().equals("false");

		for (var result : tool.getResultOf(graph)) {
			AnalysisState<
					SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>> state = result
							.getAnalysisStateAfter(expr);

			if (state.getExecutionState().isBottom())
				// the assertion is never evaluated in this result: it cannot
				// fail
				continue;

			// the violation of the assertion: reaching it if it is an assert
			// false, or its condition not holding otherwise
			it.unive.lisa.checks.verification.Violation violation = isAssertFalse
					? it.unive.lisa.checks.verification.Violation.reach()
					: failure(expr, state);

			if (isAssertFalse) {
				// we do not need to query the satisfiability of the expression:
				// the assertion fails whenever it is evaluated
				tool.warnOn((Statement) node, result.getId(), violation, POSSIBLE_FAILURE);
				continue;
			}

			V values = state.getExecutionState().valueState;
			if (values.isBottom()) {
				// the assertion is evaluated, but we have a bottom value state:
				// we cannot do much other than being conservative and say that
				// the assertion might not hold
				tool.warnOn((Statement) node, result.getId(), violation, POSSIBLE_FAILURE);
				LOG.error("The abstract state of assert's expression is BOTTOM");
				continue;
			}

			if (values.isTop()) {
				// the assertion is evaluated, but we have a top value state:
				// we cannot do much other than being conservative and say that
				// the assertion might not hold
				tool.warnOn((Statement) node, result.getId(), violation, POSSIBLE_FAILURE);
				continue;
			}

			Satisfiability overall = Satisfiability.BOTTOM;
			for (SymbolicExpression e : state.getExecutionExpressions())
				overall = overall.lub(tool.getAnalysis().satisfies(state, e, result.getId(), (ProgramPoint) node));

			// only assertions that surely hold do not raise warnings
			if (overall != Satisfiability.SATISFIED)
				tool.warnOn((Statement) node, result.getId(), violation, POSSIBLE_FAILURE);
		}
	}

	/**
	 * Yields the violation of the assertion whose condition is the given
	 * expression: after its evaluation, one of the values it might evaluate to
	 * is false. If the values of the expression are unknown, the violation is
	 * reaching the assertion.
	 */
	private static it.unive.lisa.checks.verification.Violation failure(
			Expression expr,
			AnalysisState<?> state) {
		Type bool = expr.getProgram().getTypes().getBooleanType();
		SymbolicExpression condition = null;
		for (SymbolicExpression e : state.getExecutionExpressions()) {
			SymbolicExpression negated = new UnaryExpression(bool, e, LogicalNegation.INSTANCE, expr.getLocation());
			condition = condition == null ? negated
					: new BinaryExpression(bool, condition, negated, LogicalOr.INSTANCE, expr.getLocation());
		}
		if (condition == null)
			return it.unive.lisa.checks.verification.Violation.reach();
		return it.unive.lisa.checks.verification.Violation.condition(expr, condition);
	}
}

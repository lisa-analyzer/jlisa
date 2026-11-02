package it.unive.jlisa.checkers;

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
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Assert Checker It checks whether an assertion's condition might not hold,
 * and whether the main method might terminate with an uncaught runtime
 * exception. All warnings are possible: they can be confirmed or refuted by
 * the verification of warnings performed after the checks, if a verifier has
 * been selected.
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
	 * The consumer of the definite violations found by this checker. Since
	 * this checker only issues possible warnings, no violation is currently
	 * given to it.
	 */
	@SuppressWarnings("unused")
	private final Consumer<Violation> violations;

	/**
	 * Builds the checker.
	 */
	public AssertChecker() {
		this(violation -> {
		});
	}

	/**
	 * Builds the checker, giving each definite violation it finds to the given
	 * consumer.
	 * 
	 * @param violations the consumer of the definite violations
	 */
	public AssertChecker(
			Consumer<Violation> violations) {
		this.violations = violations;
	}

	@Override
	public boolean visit(
			SemanticTool<
					SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>> tool,
			CFG graph,
			Statement node) {

		// RuntimeException property checker
		if (graph.getProgram().getEntryPoints().contains(graph) && node instanceof Ret)
			try {
				checkRuntimeException(tool, graph, node);
			} catch (SemanticException e) {
				e.printStackTrace();
			}

		// assert checker
		if (node instanceof AssertStatement)
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
			AnalysisState<
					SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>> state = result
							.getAnalysisStateAfter(node);

			// checking if there exists at least one exception state
			boolean hasExceptionState = !state.getErrors().isBottom() &&
					!state.getErrors().isTop() &&
					!state.getErrors().function.isEmpty() ||
					(!state.getSmashedErrors().isBottom() &&
							!state.getSmashedErrors().isTop() &&
							!state.getSmashedErrors().function.isEmpty());

			// if exceptions might have been thrown, we raise a possible warning
			if (hasExceptionState)
				tool.warnOn((Statement) node, "uncaught runtime exception in main method");
		}
	}

	private void checkAssert(
			SemanticTool<
					SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>> tool,
			CFG graph,
			AssertStatement node)
			throws SemanticException {
		for (var result : tool.getResultOf(graph)) {
			AnalysisState<
					SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>> state = null;

			boolean isAssertFalse = false;
			if (node instanceof SimpleAssert) {
				Expression expr = ((SimpleAssert) node).getSubExpression();
				state = result.getAnalysisStateAfter(expr);
				isAssertFalse = expr.toString().equals("false");
			} else if (node instanceof AssertionStatement) {
				Expression expr = ((AssertionStatement) node).getLeft();
				state = result.getAnalysisStateAfter(expr);
				isAssertFalse = expr.toString().equals("false");
			}

			if (state.getExecutionState().isBottom())
				// the assertion is never evaluated in this result: it cannot
				// fail
				continue;

			if (isAssertFalse) {
				// we do not need to query the satisfiability of the expression:
				// the assertion fails whenever it is evaluated
				tool.warnOn((Statement) node, POSSIBLE_FAILURE);
				continue;
			}

			V values = state.getExecutionState().valueState;
			if (values.isBottom()) {
				// the assertion is evaluated, but we have a bottom value state:
				// we cannot do much other than being conservative and say that
				// the assertion might not hold
				tool.warnOn((Statement) node, POSSIBLE_FAILURE);
				LOG.error("The abstract state of assert's expression is BOTTOM");
				continue;
			}

			if (values.isTop()) {
				// the assertion is evaluated, but we have a top value state:
				// we cannot do much other than being conservative and say that
				// the assertion might not hold
				tool.warnOn((Statement) node, POSSIBLE_FAILURE);
				continue;
			}

			Satisfiability overall = Satisfiability.BOTTOM;
			for (SymbolicExpression expr : state.getExecutionExpressions())
				overall = overall.lub(tool.getAnalysis().satisfies(state, expr, result.getId(), (ProgramPoint) node));

			// only assertions that surely hold do not raise warnings
			if (overall != Satisfiability.SATISFIED)
				tool.warnOn((Statement) node, POSSIBLE_FAILURE);
		}
	}
}

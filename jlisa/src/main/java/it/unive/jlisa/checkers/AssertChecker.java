package it.unive.jlisa.checkers;

import it.unive.jlisa.program.cfg.statement.asserts.AssertStatement;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Reachability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.lattices.ReachLattice;
import it.unive.lisa.lattices.ReachLattice.ReachabilityStatus;
import it.unive.lisa.lattices.ReachabilityProduct;
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
 * Assert Checker It checks whether an assertion's condition holds.
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class AssertChecker<V extends ValueLattice<V>>
		implements
		SemanticCheck<
				ReachabilityProduct<
						SimpleAbstractState<
								HeapEnvironment<AllocationSites>,
								V,
								TypeEnvironment<TypeSet>>>,
				Reachability<
						SimpleAbstractDomain<
								HeapEnvironment<AllocationSites>,
								V,
								TypeEnvironment<TypeSet>>,
						SimpleAbstractState<
								HeapEnvironment<AllocationSites>,
								V,
								TypeEnvironment<TypeSet>>>> {
	private static final Logger LOG = LogManager.getLogger(AssertChecker.class);

	/**
	 * The consumer of the definite violations found by this checker.
	 */
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
					ReachabilityProduct<
							SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>>,
					Reachability<
							SimpleAbstractDomain<
									HeapEnvironment<AllocationSites>,
									V,
									TypeEnvironment<TypeSet>>,
							SimpleAbstractState<
									HeapEnvironment<AllocationSites>,
									V,
									TypeEnvironment<TypeSet>>>> tool,
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
					ReachabilityProduct<
							SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>>,
					Reachability<
							SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
							SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>>> tool,
			CFG graph,
			Statement node)
			throws SemanticException {

		for (var result : tool.getResultOf(graph)) {
			AnalysisState<
					ReachabilityProduct<
							SimpleAbstractState<
									HeapEnvironment<AllocationSites>,
									V,
									TypeEnvironment<TypeSet>>>> state = result.getAnalysisStateAfter(node);

			// checking if there exists at least one exception state
			boolean hasExceptionState = !state.getErrors().isBottom() &&
					!state.getErrors().isTop() &&
					!state.getErrors().function.isEmpty() ||
					(!state.getSmashedErrors().isBottom() &&
							!state.getSmashedErrors().isTop() &&
							!state.getSmashedErrors().function.isEmpty());

			ReachabilityProduct<
					SimpleAbstractState<
							HeapEnvironment<AllocationSites>,
							V,
							TypeEnvironment<TypeSet>>> normaleState = state.getExecutionState();

			// if exceptions had been thrown, we raise a warning
			if (hasExceptionState)
				// if the normal state is bottom, we raise a definite error
				if (normaleState.second.isBottom())
					tool.warnOn((Statement) node, "DEFINITE: uncaught runtime exception in main method");
				// otherwise, we raise a possible error (both normal and
				// exception states are not bottom)
				else
					tool.warnOn((Statement) node, "POSSIBLE: uncaught runtime exception in main method");
		}
	}

	private void checkAssert(
			SemanticTool<
					ReachabilityProduct<
							SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>>,
					Reachability<
							SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
							SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>>> tool,
			CFG graph,
			AssertStatement node)
			throws SemanticException {
		for (var result : tool.getResultOf(graph)) {
			AnalysisState<
					ReachabilityProduct<
							SimpleAbstractState<
									HeapEnvironment<AllocationSites>,
									V,
									TypeEnvironment<TypeSet>>>> state = null;

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

			V values = state.getExecutionState().second.valueState;
			ReachabilityStatus reach = state.getExecutionState().first.lattice;

			if (reach == ReachabilityStatus.UNREACHABLE || state.getExecutionState().second.isBottom()) {
				// if the assertion is not reachable, it won't fail
				tool.warnOn((Statement) node, "DEFINITE: the assertion holds");
				continue;
			}

			if (isAssertFalse) {
				// we do not need to query the satisfiability of of the
				// expression:
				// we rely on reachability to determine its status
				if (reach == ReachabilityStatus.REACHABLE) {
					tool.warnOn((Statement) node, "DEFINITE: the assertion DOES NOT hold");
					violations.accept(new Violation(ViolationKind.ASSERT, (Statement) node));
				} else if (reach == ReachabilityStatus.POSSIBLY_REACHABLE) {
					tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
				}
				continue;
			}

			if (values.isBottom()) {
				// the statement is (possibly) reachable, is not an assert
				// false,
				// but we have a bottom value state
				// we cannot do much other than being conservative and
				// say that the assertion might not hold
				tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
				LOG.error("The abstract state of assert's expression is BOTTOM");
				continue;
			}

			if (values.isTop()) {
				// the statement is (possibly) reachable, is not an assert
				// false,
				// but we have a top value state
				// we cannot do much other than being conservative and
				// say that the assertion might not hold
				tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
				continue;
			}

			Satisfiability overall = Satisfiability.BOTTOM;
			for (SymbolicExpression expr : state.getExecutionExpressions())
				overall = overall.lub(tool.getAnalysis().satisfies(state, expr, (ProgramPoint) node));

			if (overall == Satisfiability.SATISFIED)
				tool.warnOn((Statement) node, "DEFINITE: the assertion holds");
			else if (overall == Satisfiability.NOT_SATISFIED) {
				if (reach == ReachLattice.ReachabilityStatus.REACHABLE) {
					tool.warnOn((Statement) node, "DEFINITE: the assertion DOES NOT hold");
					violations.accept(new Violation(ViolationKind.ASSERT, (Statement) node));
				} else if (reach == ReachLattice.ReachabilityStatus.POSSIBLY_REACHABLE) {
					tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
				}
			} else {
				tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
			}
		}
	}
}

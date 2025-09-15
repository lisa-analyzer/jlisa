package it.unive.jlisa.checkers;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.program.cfg.statement.asserts.AssertStatement;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.allocations.AllocationSites;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Assert Checker It checks whether an assertion's condition holds.
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class AssertChecker
		implements
		SemanticCheck<
				SimpleAbstractState<
						HeapEnvironment<AllocationSites>,
						ValueEnvironment<ConstantValue>,
						TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<
						HeapEnvironment<AllocationSites>,
						ValueEnvironment<ConstantValue>,
						TypeEnvironment<TypeSet>>> {

	private static final Logger LOG = LogManager.getLogger(AssertChecker.class);

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<
					SimpleAbstractState<
							HeapEnvironment<AllocationSites>,
							ValueEnvironment<ConstantValue>,
							TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<
							HeapEnvironment<AllocationSites>,
							ValueEnvironment<ConstantValue>,
							TypeEnvironment<TypeSet>>> tool,
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
			CheckToolWithAnalysisResults<
					SimpleAbstractState<HeapEnvironment<AllocationSites>, ValueEnvironment<ConstantValue>,
							TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<HeapEnvironment<AllocationSites>, ValueEnvironment<ConstantValue>,
							TypeEnvironment<TypeSet>>> tool,
			CFG graph,
			Statement node)
			throws SemanticException {

		for (var result : tool.getResultOf(graph)) {
			AnalysisState<
					SimpleAbstractState<
							HeapEnvironment<AllocationSites>,
							ValueEnvironment<ConstantValue>,
							TypeEnvironment<TypeSet>>> state = result.getAnalysisStateAfter(node);

			// checking if there exists at least one exception state
			boolean hasExceptionState = !state.getErrors().isBottom() &&
					!state.getErrors().isTop() &&
					!state.getErrors().function.isEmpty() || 
					(!state.getSmashedErrors().isBottom() &&
					!state.getSmashedErrors().isTop() &&
					!state.getSmashedErrors().function.isEmpty());

			SimpleAbstractState<
					HeapEnvironment<AllocationSites>,
					ValueEnvironment<ConstantValue>,
					TypeEnvironment<TypeSet>> normaleState = state.getExecutionState();

			// if exceptions had been thrown, we raise a warning
			if (hasExceptionState)
				// if the normal state is bottom, we raise a definite error
				if (normaleState.isBottom())
					tool.warnOn((Statement) node, "DEFINITE: uncaught runtime exception in main method");
				// otherwise, we raise a possible error (both normal and
				// exception states are not bottom)
				else
					tool.warnOn((Statement) node, "POSSIBLE: uncaught runtime exception in main method");
		}
	}

	private void checkAssert(
			CheckToolWithAnalysisResults<
					SimpleAbstractState<
							HeapEnvironment<AllocationSites>,
							ValueEnvironment<ConstantValue>,
							TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<
							HeapEnvironment<AllocationSites>,
							ValueEnvironment<ConstantValue>,
							TypeEnvironment<TypeSet>>> tool,
			CFG graph,
			AssertStatement node)
			throws SemanticException {
		for (var result : tool.getResultOf(graph)) {
			AnalysisState<
					SimpleAbstractState<
							HeapEnvironment<AllocationSites>,
							ValueEnvironment<ConstantValue>,
							TypeEnvironment<TypeSet>>> state = null;
			if (node instanceof SimpleAssert)
				state = result.getAnalysisStateAfter(((SimpleAssert) node).getSubExpression());
			else if (node instanceof AssertionStatement) {
				state = result.getAnalysisStateAfter(((AssertionStatement) node).getLeft());
			}

			for (SymbolicExpression expr : state.getExecutionExpressions()) {
				ValueEnvironment<ConstantValue> valueState = state.getExecutionState().valueState;

				Satisfiability sat = tool.getAnalysis().satisfies(state, expr, (ProgramPoint) node);
				if (!valueState.isBottom()) {
					if (!valueState.isTop()) {
						if (sat == Satisfiability.SATISFIED) {
							tool.warnOn((Statement) node, "DEFINITE: the assertion holds");
						} else if (sat == Satisfiability.NOT_SATISFIED) {
							tool.warnOn((Statement) node, "DEFINITE: the assertion DOES NOT hold");
						} else if (sat == Satisfiability.UNKNOWN)
							tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
						else
							LOG.error("Cannot satisfy the expression");
					} else
						tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
				} else {
					// FIXME: property proved
					LOG.error("The abstract state of assert's expression is BOTTOM");
				}
			}
		}
	}
}

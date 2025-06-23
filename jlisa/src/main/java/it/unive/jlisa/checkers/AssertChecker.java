package it.unive.jlisa.checkers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.jlisa.analysis.ConstantPropagation;
import it.unive.jlisa.program.cfg.statement.asserts.AssertStatement;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;

/**
 * Assert Checker
 * 
 * It checks whether an assertion's condition holds.
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class AssertChecker implements
		SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<ConstantPropagation>, TypeEnvironment<InferredTypes>>> {
	
	private static final Logger LOG = LogManager.getLogger(AssertChecker.class);
	
	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<ConstantPropagation>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {

		if (node instanceof AssertStatement)
			try {
				checkAssert(tool, graph, (AssertStatement) node);
			} catch (SemanticException e) {
				e.printStackTrace();
			}

		return true;
	}

	private void checkAssert(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<ConstantPropagation>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, AssertStatement node) throws SemanticException {
		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<ConstantPropagation>, TypeEnvironment<InferredTypes>>> result : tool
				.getResultOf(graph)) {

			AnalysisState<SimpleAbstractState<PointBasedHeap, ValueEnvironment<ConstantPropagation>, TypeEnvironment<InferredTypes>>> state = null;
			if (node instanceof SimpleAssert)
				state = result.getAnalysisStateAfter(((SimpleAssert) node).getSubExpression());
			else if (node instanceof AssertionStatement) {
				state = result.getAnalysisStateAfter(((AssertionStatement) node).getLeft());
			}

			Set<SymbolicExpression> reachableIds = new HashSet<>();
			Iterator<SymbolicExpression> comExprIterator = state.getComputedExpressions().iterator();
			if (comExprIterator.hasNext()) {
				SymbolicExpression boolExpr = comExprIterator.next();
				reachableIds
						.addAll(state.getState().reachableFrom(boolExpr, (Statement) node, state.getState()).elements);

				for (SymbolicExpression s : reachableIds) {
					Set<Type> types = state.getState().getRuntimeTypesOf(s, (Statement) node, state.getState());

					if (types.stream().allMatch(t -> t.isInMemoryType() || t.isPointerType()))
						continue;

					ValueEnvironment<ConstantPropagation> valueState = state.getState().getValueState();

					ConstantPropagation abstractValue = valueState.eval((ValueExpression) s, (ProgramPoint) node,
							state.getState());
					if (!abstractValue.isBottom()) {
						if (!abstractValue.isTop()) {
							Object cnst = abstractValue.getConstant();
							if (cnst != null && cnst instanceof Boolean) {
								Boolean hold = (Boolean) cnst;
								if (hold.booleanValue()) {
									tool.warnOn((Statement) node, "DEFINITE: The assertion is hold.");
								} else {
									tool.warnOn((Statement) node, "DEFINITE: The assertion is NOT hold.");
								}
							}
						} else {
							tool.warnOn((Statement) node, "POSSIBLE: The assertion MAY BE (NOT) hold.");
						}
					} else {
						LOG.error("The abstract state of assert's expression is BOTTOM");
					}

				}
			}
		}
	}
}

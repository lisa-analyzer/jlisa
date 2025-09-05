package it.unive.jlisa.checkers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.jlisa.analysis.ConstantPropagation;
import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.program.cfg.statement.asserts.AssertStatement;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractDomain;
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
				TypeEnvironment<TypeSet>>
		> {
	
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
					TypeEnvironment<TypeSet>>
			> tool,
			CFG graph, 
			Statement node) {
		if (node instanceof AssertStatement)
			try {
				checkAssert(tool, graph, (AssertStatement) node);
			} catch (SemanticException e) {
				e.printStackTrace();
			}

		return true;
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
					TypeEnvironment<TypeSet>>
			> tool,
			CFG graph, 
			AssertStatement node) 
			throws SemanticException {
		for (var result : tool.getResultOf(graph)) {
			AnalysisState<
				SimpleAbstractState<
					HeapEnvironment<AllocationSites>, 
					ValueEnvironment<ConstantValue>, 
					TypeEnvironment<TypeSet>>
			> state = null;
			if (node instanceof SimpleAssert)
				state = result.getAnalysisStateAfter(((SimpleAssert) node).getSubExpression());
			else if (node instanceof AssertionStatement) {
				state = result.getAnalysisStateAfter(((AssertionStatement) node).getLeft());
			}

			Set<SymbolicExpression> reachableIds = new HashSet<>();
			Iterator<SymbolicExpression> comExprIterator = state.getExecutionExpressions().iterator();
			if (comExprIterator.hasNext()) {
				SymbolicExpression boolExpr = comExprIterator.next();
				reachableIds.addAll(tool.getAnalysis().reachableFrom(
						state,		
						boolExpr, 
						(Statement) node)
					.elements);

				for (SymbolicExpression s : reachableIds) {
					Set<Type> types = tool.getAnalysis().getRuntimeTypesOf(state, s, (Statement) node);

					if (types.stream().allMatch(t -> t.isInMemoryType() || t.isPointerType()))
						continue;

					ValueEnvironment<ConstantValue> valueState = state.getExecutionState().valueState;
					ConstantPropagation cp = (ConstantPropagation) tool.getAnalysis().domain.valueDomain;
					SemanticOracle oracle = tool.getAnalysis().domain.makeOracle(state.getExecutionState());
					ConstantValue abstractValue = cp.eval(valueState, (ValueExpression) s, (ProgramPoint) node, oracle);

					if (!abstractValue.isBottom()) {
						if (!abstractValue.isTop()) {
							Object cnst = abstractValue.getValue();
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

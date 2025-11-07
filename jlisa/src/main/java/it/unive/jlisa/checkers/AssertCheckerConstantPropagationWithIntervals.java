package it.unive.jlisa.checkers;

import it.unive.jlisa.lattices.ConstantValueIntInterval;
import it.unive.jlisa.lattices.ReachLattice;
import it.unive.jlisa.lattices.ReachLattice.ReachabilityStatus;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedStaticCall;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.asserts.AssertStatement;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.jlisa.witness.WitnessWriter;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.allocations.AllocationSites;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Assert Checker It checks whether an assertion's condition holds.
 *
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class AssertCheckerConstantPropagationWithIntervals
		implements
		SemanticCheck<
				SimpleAbstractState<
						HeapEnvironment<AllocationSites>,
						ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
						TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<
						HeapEnvironment<AllocationSites>,
						ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
						TypeEnvironment<TypeSet>>> {
	private record VariableInfo(
			String variableName,
			String type,
			String originFile,
			String startLine,
			String assumptionScope) {
	}

	private List<VariableInfo> nonDetVariables = new ArrayList<>();
	private static final Logger LOG = LogManager.getLogger(AssertCheckerConstantPropagationWithIntervals.class);

	private boolean minimalWitnessGenerated = false;
	private boolean witnessGenerated = false;
	private WitnessWriter witness;
	private String workDir;

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<
					SimpleAbstractState<
							HeapEnvironment<AllocationSites>,
							ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
							TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<
							HeapEnvironment<AllocationSites>,
							ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
							TypeEnvironment<TypeSet>>> tool,
			CFG graph,
			Statement node) {

		if (node instanceof JavaUnresolvedStaticCall unresolvedCall
				&& unresolvedCall.getQualifier().equals("org.sosy_lab.sv_benchmarks.Verifier")) {
			if (unresolvedCall.getParentStatement() instanceof JavaAssignment assignment
					&& unresolvedCall.getTargetName().startsWith("nondet")) {
				String type = unresolvedCall.getTargetName().substring(6);
				String variableName = assignment.getLeft().toString();
				String assumptionScope = "java::LMain;.main([Ljava/lang/String;)V";
				// TODO: we should extract this from the cfg signature.
				if (assignment.getLocation() instanceof SourceCodeLocation sc) {
					String originFile = sc.getSourceFile();
					String startLine = String.valueOf(sc.getLine());

					nonDetVariables.add(new VariableInfo(variableName, type, originFile, startLine, assumptionScope));
				}
			}
		}
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
					SimpleAbstractState<
							HeapEnvironment<AllocationSites>,
							ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
							TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<
							HeapEnvironment<AllocationSites>,
							ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
							TypeEnvironment<TypeSet>>> tool,
			CFG graph,
			Statement node)
			throws SemanticException {

		for (var result : tool.getResultOf(graph)) {
			AnalysisState<
					SimpleAbstractState<
							HeapEnvironment<AllocationSites>,
							ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
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
					ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
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
							ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
							TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<
							HeapEnvironment<AllocationSites>,
							ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
							TypeEnvironment<TypeSet>>> tool,
			CFG graph,
			AssertStatement node)
			throws SemanticException {
		for (var result : tool.getResultOf(graph)) {
			AnalysisState<
					SimpleAbstractState<
							HeapEnvironment<AllocationSites>,
							ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
							TypeEnvironment<TypeSet>>> state = null;

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

			ValueLatticeProduct<ReachLattice,
					ValueEnvironment<ConstantValueIntInterval>> valueState = state.getExecutionState().valueState;
			ReachabilityStatus reach = valueState.first.lattice;
			ValueEnvironment<ConstantValueIntInterval> values = valueState.second;

			if (reach == ReachabilityStatus.UNREACHABLE) {
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
					if (!minimalWitnessGenerated) {
						generateMinimalViolationWitness();
						minimalWitnessGenerated = true;
					}
				} else if (reach == ReachabilityStatus.POSSIBLY_REACHABLE) {
					if (!minimalWitnessGenerated && !witnessGenerated) {
						generateViolationWitness();
						witnessGenerated = true;
					}
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
				// We don't know the values here, so it's hard to produce a
				// witness. First attempt: try with a table of values.
				if (!minimalWitnessGenerated && !witnessGenerated) {
					generateViolationWitness();
					witnessGenerated = true;
				}
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
				// We don't know the values here, so it's hard to produce a
				// witness. First attempt: try with a table of values.
				if (!minimalWitnessGenerated && !witnessGenerated) {
					generateViolationWitness();
					witnessGenerated = true;
				}
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
					if (!minimalWitnessGenerated) {
						generateMinimalViolationWitness();
						minimalWitnessGenerated = true;
					}
				} else if (reach == ReachLattice.ReachabilityStatus.POSSIBLY_REACHABLE) {
					tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
					if (!minimalWitnessGenerated && !witnessGenerated) {
						generateViolationWitness();
						witnessGenerated = true;
					}
				}
			} else {
				tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
				if (!minimalWitnessGenerated && !witnessGenerated) {
					generateViolationWitness();
					witnessGenerated = true;
				}
			}
		}
	}

	private void generateMinimalViolationWitness() {
		try {
			witness = new WitnessWriter();
			Map<String, String> violationAttrs = Map.of("violation", "true");
			witness.addNode("sink", null);
			witness.addNode("n0", violationAttrs);
			String witnessFilePath = workDir + "/witness/witness.graphml";
			File outputFile = new File(witnessFilePath);
			outputFile.getParentFile().mkdirs();
			witness.writeToFile(witnessFilePath);
			LOG.info("Minimal violation witness produced.");
		} catch (Exception e) {
			LOG.warn("Failed to produce witness: {}", e);
		}
	}

	/*
	 * This method provides non determinstic values for Verifier's methods.
	 * Ideally, these values should be determine by looking at the analysis
	 * state. However, if the analysis state is TOP, we should "guess" a number.
	 */
	private String getNonDetDefaultValue(
			String type) {
		switch (type) {
		case "Boolean":
			return "false";
		case "Byte":
			return "-128";
		case "Char":
			return "Ж";
		case "Short":
			return "32767";
		case "Int":
			return "1000000000";
		case "Long":
			return "2036854775808";
		case "Float":
			return "2036854775808";
		case "Double":
			return "0.998877665544";
		case "String":
			return "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		default:
			return "0";
		}
	}

	private void generateViolationWitness() {
		try {
			if (!nonDetVariables.isEmpty()) {
				witness = new WitnessWriter();
				Map<String, String> violationAttrs = Map.of("violation", "true");
				Map<String, String> entryAttributes = Map.of("entry", "true");
				witness.addNode("sink", null);
				witness.addNode("n0", entryAttributes);
				for (int i = 1; i < nonDetVariables.size(); i++) {
					witness.addNode("n" + i, null);
				}
				witness.addNode("n" + nonDetVariables.size(), violationAttrs);
				// generate edges
				for (int i = 0; i < nonDetVariables.size(); i++) {
					VariableInfo variable = nonDetVariables.get(i);
					Map<String, String> edgeAttrs = Map.of(
							"originfile", variable.originFile,
							"startline", variable.startLine,
							"threadId", "0",
							"assumption", variable.variableName + " = " + getNonDetDefaultValue(variable.type),
							"assumption.scope", variable.assumptionScope);
					witness.addEdge("n" + i, "n" + (i + 1), edgeAttrs);
				}
				String witnessFilePath = workDir + "/witness/witness.graphml";
				File outputFile = new File(witnessFilePath);
				outputFile.getParentFile().mkdirs();
				witness.writeToFile(witnessFilePath);
				LOG.info("Violation witness produced.");
			} else {
				// the least we can do is to generate an minimal witness
				generateMinimalViolationWitness();
			}
		} catch (Exception e) {
			LOG.warn("Failed to produce witness: {}", e);
		}
	}

	@Override
	public void beforeExecution(
			CheckToolWithAnalysisResults<SimpleAbstractState<HeapEnvironment<AllocationSites>,
					ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
					TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<HeapEnvironment<AllocationSites>,
							ValueLatticeProduct<ReachLattice, ValueEnvironment<ConstantValueIntInterval>>,
							TypeEnvironment<TypeSet>>> tool) {
		this.workDir = tool.getConfiguration().workdir;
		SemanticCheck.super.beforeExecution(tool);
	}
}
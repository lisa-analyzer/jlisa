package it.unive.jlisa.checkers;

import it.unive.jlisa.analysis.value.ExistentialSatisfiabilityProvider;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedStaticCall;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.asserts.AssertStatement;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.witness.WitnessWriter;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Reachability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
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
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.protection.CatchBlock;
import it.unive.lisa.program.cfg.protection.ProtectionBlock;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.NegatableOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.type.Type;
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
	private record VariableInfo(
			String variableName,
			String type,
			String originFile,
			String startLine,
			String assumptionScope) {
	}

	/**
	 * Optional provider of existential satisfiability, used to produce definite
	 * FALSE verdicts when a concrete violating run is guaranteed to exist. May
	 * be {@code null} when the value domain does not support
	 * under-approximation.
	 */
	private final ExistentialSatisfiabilityProvider existentialProvider;

	private List<VariableInfo> nonDetVariables = new ArrayList<>();
	private static final Logger LOG = LogManager.getLogger(AssertChecker.class);

	private boolean minimalWitnessGenerated = false;
	private boolean witnessGenerated = false;
	private WitnessWriter witness;
	private String workDir;

	/**
	 * Constructs an {@link AssertChecker} without existential satisfiability
	 * support. FALSE verdicts will not be generated from under-approximation.
	 */
	public AssertChecker() {
		this.existentialProvider = null;
	}

	/**
	 * Constructs an {@link AssertChecker} with an optional existential
	 * satisfiability provider. When the provider is non-null, the checker will
	 * attempt to produce definite FALSE verdicts for assertions that are
	 * guaranteed to be violated.
	 *
	 * @param provider the existential satisfiability provider, or {@code null}
	 *                     to disable this feature
	 */
	public AssertChecker(
			ExistentialSatisfiabilityProvider provider) {
		this.existentialProvider = provider;
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
			SemanticTool<
					ReachabilityProduct<
							SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>>,
					Reachability<SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
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
					Reachability<SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
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

			if (reach == ReachabilityStatus.UNREACHABLE) {
				if (isAssertFalse) {
					CatchBlock containingCatch = findContainingCatchBlock(graph, (Statement) node);
					if (containingCatch != null && !isSafeTrustUnreachable(containingCatch)) {
						// assert false in a catch block for broadly-caught
						// exceptions
						// (e.g. RuntimeException, Exception): UNREACHABLE may
						// be an
						// artifact of imprecise interprocedural exception
						// modeling.
						// Conservative: report POSSIBLE.
						tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
					} else {
						// Either not in a catch block, or in a catch block
						// whose exception
						// type is precisely modeled by the analysis (e.g.
						// ClassCastException).
						tool.warnOn((Statement) node, "DEFINITE: the assertion holds");
					}
				} else {
					// if the assertion is not reachable, it won't fail
					tool.warnOn((Statement) node, "DEFINITE: the assertion holds");
				}
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
				// Try existential satisfiability for a DEFINITE FALSE verdict.
				// If the domain tracks input-provenance (nondet) values, we can
				// confirm a concrete violating run exists even when the
				// over-approximation is UNKNOWN.
				if (existentialProvider != null && values instanceof ValueEnvironment<?> env) {
					try {
						boolean existsViolation = false;
						// Use only the outermost execution expression so that
						// short-circuit
						// sub-expressions (e.g. the left branch of A||B) are
						// not checked in
						// isolation. Then negate it fully via De Morgan before
						// checking.
						SymbolicExpression outermost = findOutermostExpression(
								state.getExecutionExpressions());
						if (outermost instanceof BinaryExpression be) {
							ValueExpression negated = buildFullNegation(be);
							if (negated != null)
								existsViolation = existentialProvider.existentiallySatisfies(
										env, negated, (ProgramPoint) node, null);
						}
						if (existsViolation) {
							if (reach == ReachabilityStatus.REACHABLE) {
								tool.warnOn((Statement) node, "DEFINITE: the assertion DOES NOT hold");
								if (!minimalWitnessGenerated) {
									generateMinimalViolationWitness();
									minimalWitnessGenerated = true;
								}
							} else {
								tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
								if (!minimalWitnessGenerated && !witnessGenerated) {
									generateViolationWitness();
									witnessGenerated = true;
								}
							}
							continue;
						}
					} catch (SemanticException e) {
						LOG.debug("Existential satisfiability check failed: {}", e.getMessage());
					}
				}
				tool.warnOn((Statement) node, "POSSIBLE: the assertion MAY (NOT) BE hold");
				if (!minimalWitnessGenerated && !witnessGenerated) {
					generateViolationWitness();
					witnessGenerated = true;
				}
			}
		}
	}

	/**
	 * Recursively builds the full logical negation of a value expression using
	 * De Morgan's laws:
	 * <ul>
	 * <li>{@code ¬(A || B) = ¬A ∧ ¬B}</li>
	 * <li>{@code ¬(A && B) = ¬A ∨ ¬B}</li>
	 * <li>{@code ¬(x op c)} by flipping the comparison operator via
	 * {@link NegatableOperator#opposite()}</li>
	 * </ul>
	 * Returns {@code null} when any sub-expression cannot be negated (e.g. an
	 * arithmetic sub-expression that is not a {@link NegatableOperator}).
	 *
	 * @param expr the expression to negate
	 *
	 * @return the fully negated expression, or {@code null} if not applicable
	 */
	private static ValueExpression buildFullNegation(
			ValueExpression expr) {
		if (!(expr instanceof BinaryExpression be))
			return null;
		BinaryOperator op = be.getOperator();
		// De Morgan: ¬(A || B) = ¬A ∧ ¬B
		if (op instanceof LogicalOr) {
			if (!(be.getLeft() instanceof ValueExpression vl)
					|| !(be.getRight() instanceof ValueExpression vr))
				return null;
			ValueExpression negLeft = buildFullNegation(vl);
			ValueExpression negRight = buildFullNegation(vr);
			if (negLeft == null || negRight == null)
				return null;
			return new BinaryExpression(
					JavaBooleanType.INSTANCE, negLeft, negRight,
					LogicalAnd.INSTANCE, be.getCodeLocation());
		}
		// De Morgan: ¬(A && B) = ¬A ∨ ¬B
		if (op instanceof LogicalAnd) {
			if (!(be.getLeft() instanceof ValueExpression vl)
					|| !(be.getRight() instanceof ValueExpression vr))
				return null;
			ValueExpression negLeft = buildFullNegation(vl);
			ValueExpression negRight = buildFullNegation(vr);
			if (negLeft == null || negRight == null)
				return null;
			return new BinaryExpression(
					JavaBooleanType.INSTANCE, negLeft, negRight,
					LogicalOr.INSTANCE, be.getCodeLocation());
		}
		// Atomic comparison: flip the operator (e.g. < → >=)
		if (!(op instanceof NegatableOperator neg))
			return null;
		NegatableOperator negatedOp = neg.opposite();
		if (!(negatedOp instanceof BinaryOperator binaryNeg))
			return null;
		return be.withOperator(binaryNeg);
	}

	/**
	 * From a set of execution expressions, returns the single outermost one —
	 * the expression that is not a sub-expression of any other expression in
	 * the set. This is needed because short-circuit evaluation of compound
	 * conditions (e.g. {@code A || B}) causes LiSA to record both the left
	 * sub-expression {@code A} and the full expression {@code A || B} in the
	 * analysis state; the existential check must only operate on the full
	 * condition.
	 *
	 * @param exprs the execution expressions from the analysis state
	 *
	 * @return the outermost expression, or the last one if ambiguous
	 */
	private static SymbolicExpression findOutermostExpression(
			Iterable<SymbolicExpression> exprs) {
		List<SymbolicExpression> list = new ArrayList<>();
		for (SymbolicExpression e : exprs)
			list.add(e);
		if (list.size() == 1)
			return list.get(0);
		List<SymbolicExpression> outermost = new ArrayList<>();
		for (SymbolicExpression candidate : list) {
			boolean isSub = false;
			for (SymbolicExpression other : list)
				if (other != candidate && isSubExpressionOf(candidate, other)) {
					isSub = true;
					break;
				}
			if (!isSub)
				outermost.add(candidate);
		}
		return outermost.isEmpty() ? list.get(list.size() - 1) : outermost.get(outermost.size() - 1);
	}

	/**
	 * Returns {@code true} if {@code needle} is equal to or a structural
	 * sub-expression of {@code haystack}.
	 *
	 * @param needle   the expression to look for
	 * @param haystack the expression to search in
	 *
	 * @return {@code true} if needle appears anywhere inside haystack
	 */
	private static boolean isSubExpressionOf(
			SymbolicExpression needle,
			SymbolicExpression haystack) {
		if (needle == haystack || needle.equals(haystack))
			return true;
		if (haystack instanceof BinaryExpression be)
			return isSubExpressionOf(needle, be.getLeft())
					|| isSubExpressionOf(needle, be.getRight());
		return false;
	}

	/**
	 * Returns the {@link CatchBlock} containing {@code stmt}, or {@code null}
	 * if {@code stmt} is not inside any catch clause within the given CFG.
	 *
	 * @param cfg  the CFG containing the statement
	 * @param stmt the statement to look up
	 *
	 * @return the enclosing {@link CatchBlock}, or {@code null}
	 */
	private static CatchBlock findContainingCatchBlock(
			CFG cfg,
			Statement stmt) {
		for (ProtectionBlock pb : cfg.getDescriptor().getProtectionBlocks())
			for (var cb : pb.getCatchBlocks())
				if (cb.getBody().getBody().contains(stmt))
					return cb;
		return null;
	}

	/**
	 * Returns {@code true} if the exception types in {@code catchBlock} are all
	 * precisely modeled by our analysis, meaning that an UNREACHABLE result for
	 * an {@code assert false} inside this block can be trusted.
	 * <p>
	 * Two categories qualify:
	 * <ul>
	 * <li><em>User-defined exception types</em> (no package qualifier, i.e. no
	 * dot in the type name, e.g. {@code "A*"}, {@code "B*"}): our analysis has
	 * full visibility of user-defined code, so the catch/throw relationship is
	 * modeled precisely.</li>
	 * <li>{@code ClassCastException}: our type analysis
	 * ({@code JavaInferredTypes}) can determine whether a cast is safe, so if
	 * the catch block is UNREACHABLE the cast provably cannot throw.</li>
	 * </ul>
	 * Standard library broad exception types ({@code java.lang.Exception},
	 * {@code java.lang.RuntimeException}, etc.) do <em>not</em> qualify because
	 * our analysis may miss implicit exception sources (e.g. array bounds
	 * violations inside called methods).
	 *
	 * @param catchBlock the catch block to check
	 *
	 * @return {@code true} if UNREACHABLE can be trusted for this catch block
	 */
	private static boolean isSafeTrustUnreachable(
			CatchBlock catchBlock) {
		Type[] exceptions = catchBlock.getExceptions();
		if (exceptions.length == 0)
			return false;
		for (Type t : exceptions) {
			String name = t.toString();
			// User-defined exception types have no package qualifier (no dots)
			boolean isUserDefined = !name.contains(".");
			// ClassCastException is precisely handled by JavaInferredTypes
			boolean isClassCastException = name.contains("ClassCastException");
			if (!isUserDefined && !isClassCastException)
				return false;
		}
		return true;
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
			SemanticTool<
					ReachabilityProduct<
							SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>>,
					Reachability<SimpleAbstractDomain<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>,
							SimpleAbstractState<HeapEnvironment<AllocationSites>, V, TypeEnvironment<TypeSet>>>> tool) {
		this.workDir = tool.getConfiguration().workdir;
		SemanticCheck.super.beforeExecution(tool);
	}
}
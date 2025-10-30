package it.unive.jlisa.interprocedural.callgraph;

import it.unive.jlisa.lattices.ReachLattice;
import it.unive.jlisa.lattices.ReachLattice.ReachabilityStatus;
import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.CFGResults;
import it.unive.lisa.interprocedural.FixpointResults;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.NoEntryPointException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.ContextSensitivityToken;
import it.unive.lisa.interprocedural.context.LastCallToken;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.Throw;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.program.language.scoping.ScopingStrategy;
import it.unive.lisa.util.StringUtilities;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.TreeSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A context sensitive interprocedural analysis. The context sensitivity is
 * tuned by the kind of {@link ContextSensitivityToken} used. Recursions are
 * approximated applying the iterates of the recursion starting from bottom and
 * using the same widening threshold of cfg fixpoints.
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class JavaContextBasedAnalysis<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		ContextBasedAnalysis<A, D> {

	private static final Logger LOG = LogManager.getLogger(JavaContextBasedAnalysis.class);

	/**
	 * The members that cause a new fixpoint iteration to be triggered after the
	 * current one, as their approximation for at least one context changed
	 * during the iteration.
	 */
	private final Collection<CodeMember> triggers;

	/**
	 * Whether or not a new recursion has been discovered in the latest fixpoint
	 * iteration.
	 */
	private boolean pendingRecursions;

	/**
	 * The results computed by this analysis.
	 */
	protected FixpointResults<A> results;

	/**
	 * The kind of {@link WorkingSet} to use during this analysis.
	 */
	private Class<? extends WorkingSet<Statement>> workingSet;

	/**
	 * The current sensitivity token.
	 */
	protected ContextSensitivityToken token;

	/**
	 * The fixpoint configuration.
	 */
	protected FixpointConfiguration conf;

	/**
	 * Builds the analysis, using {@link LastCallToken}s.
	 */
	public JavaContextBasedAnalysis() {
		this(LastCallToken.getSingleton());
	}

	/**
	 * Builds the analysis.
	 *
	 * @param token an instance of the tokens to be used to partition w.r.t.
	 *                  context sensitivity
	 */
	public JavaContextBasedAnalysis(
			ContextSensitivityToken token) {
		this.token = token;
		triggers = new HashSet<>();
	}

	/**
	 * Builds the analysis by copying the given one.
	 * 
	 * @param other the original analysis to copy
	 */
	protected JavaContextBasedAnalysis(
			JavaContextBasedAnalysis<A, D> other) {
		super(other);
		this.conf = other.conf;
		this.results = other.results;
		this.token = other.token;
		this.triggers = other.triggers;
		this.workingSet = other.workingSet;
		this.pendingRecursions = false;
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			Analysis<A, D> analysis)
			throws InterproceduralAnalysisException {
		super.init(app, callgraph, policy, analysis);
		this.conf = null;
		this.results = null;
		this.token = token.startingId();
		this.workingSet = null;
		this.pendingRecursions = false;
		this.triggers.clear();
	}

	@Override
	public void fixpoint(
			AnalysisState<A> entryState,
			FixpointConfiguration conf)
			throws FixpointException {
		this.workingSet = conf.fixpointWorkingSet;
		this.conf = conf;

		// new fixpoint execution: reset
		CodeUnit unit = new CodeUnit(SyntheticLocation.INSTANCE, app.getPrograms()[0], "singleton");
		CFG singleton = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "singleton"));
		ContextSensitivityToken empty = (ContextSensitivityToken) token.startingId();
		AnalyzedCFG<A> graph = conf.optimize
				? new OptimizedAnalyzedCFG<>(singleton, empty, entryState.bottom(), this)
				: new AnalyzedCFG<>(singleton, empty, entryState);
		CFGResults<A> value = new CFGResults<>(graph);
		this.results = new FixpointResults<>(value.top());

		if (app.getEntryPoints().isEmpty())
			throw new NoEntryPointException();

		Collection<CFG> entryPoints = new TreeSet<>(
				(
						c1,
						c2) -> c1.getDescriptor().getLocation().compareTo(c2.getDescriptor().getLocation()));
		entryPoints.addAll(app.getEntryPoints());

		// hack: we want the reachability to start from REACHABLE
		// which is not the top of the lattice
		entryState = fixReachability(entryState);

		int iter = 0;
		do {
			LOG.info("Performing {} fixpoint iteration", StringUtilities.ordinal(iter + 1));
			triggers.clear();
			pendingRecursions = false;

			processEntrypoints(entryState, empty, entryPoints);

			// if (pendingRecursions) {
			// Set<Recursion<A>> recursions = new HashSet<>();
			//
			// for (Collection<CodeMember> rec : callgraph.getRecursions())
			// try {
			// buildRecursion(entryState, recursions, rec);
			// } catch (SemanticException e) {
			// throw new FixpointException("Unable to build recursion", e);
			// }
			//
			// solveRecursions(recursions);
			// }

			// starting from the callers of the cfgs that needed a lub,
			// find out the complete set of cfgs that might need to be
			// processed again
			Collection<CodeMember> toRemove = callgraph.getCallersTransitively(triggers);
			toRemove.removeAll(triggers);
			toRemove.stream().filter(CFG.class::isInstance).map(CFG.class::cast).forEach(results::forget);

			iter++;
		} while (!triggers.isEmpty());
	}

	// private void solveRecursions(
	// Set<Recursion<A>> recursions) {
	// List<Recursion<A>> orderedRecursions = new
	// ArrayList<>(recursions.size());
	// for (Recursion<A> rec : recursions) {
	// int pos = 0;
	// for (; pos < orderedRecursions.size(); pos++)
	// if
	// (orderedRecursions.get(pos).getMembers().contains(rec.getInvocation().getCFG()))
	// // as the recursion at pos contains the member
	// // invoking rec, rec must be solved before the
	// // recursion at pos
	// break;
	// // if no match is found, add() will place the element at the
	// // end (pos == size())
	// // otherwise, elements will be shifted
	// orderedRecursions.add(pos, rec);
	// }
	//
	// try {
	// for (Recursion<A> rec : orderedRecursions) {
	// new JavaRecursionSolver<>(this, rec).solve();
	// triggers.addAll(rec.getMembers());
	// }
	// } catch (SemanticException e) {
	// throw new AnalysisExecutionException("Unable to solve one or more
	// recursions", e);
	// }
	// }
	//
	// @SuppressWarnings("unchecked")
	// private void buildRecursion(
	// AnalysisState<A> entryState,
	// Set<Recursion<A>> recursions,
	// Collection<CodeMember> rec)
	// throws SemanticException {
	// // these are the calls that start the recursion by invoking
	// // one of its members
	// Collection<Call> starters = callgraph.getCallSites(rec)
	// .stream()
	// .filter(site -> !rec.contains(site.getCFG()))
	// .collect(Collectors.toSet());
	//
	// for (Call starter : starters) {
	// // these are the head of the recursion: members invoked
	// // from outside of it
	// Set<CFG> heads = callgraph.getCallees(starter.getCFG())
	// .stream()
	// .filter(callee -> rec.contains(callee))
	// .filter(CFG.class::isInstance)
	// .map(CFG.class::cast)
	// .collect(Collectors.toSet());
	// Set<Pair<ContextSensitivityToken, CompoundState<A>>> entries = new
	// HashSet<>();
	// for (Entry<ScopeId, AnalyzedCFG<A>> res : results.get(starter.getCFG()))
	// {
	// StatementStore<A> params = new StatementStore<>(entryState.bottom());
	// Expression[] parameters = starter.getParameters();
	// if (conf.optimize)
	// for (Expression actual : parameters)
	// params.put(
	// actual,
	// ((OptimizedAnalyzedCFG<A, D>) res.getValue())
	// .getUnwindedAnalysisStateAfter(actual, conf));
	// else
	// for (Expression actual : parameters)
	// params.put(actual, res.getValue().getAnalysisStateAfter(actual));
	//
	// if (parameters.length == 0)
	// entries.add(Pair.of((ContextSensitivityToken) res.getKey(),
	// CompoundState.of(res.getValue().getAnalysisStateBefore(starter),
	// params)));
	// else
	// entries.add(
	// Pair.of(
	// (ContextSensitivityToken) res.getKey(),
	// CompoundState.of(params.getState(parameters[parameters.length - 1]),
	// params)));
	// }
	//
	// for (CFG head : heads)
	// for (Pair<ContextSensitivityToken, CompoundState<A>> entry : entries) {
	// Recursion<A> recursion = new Recursion<>(starter, entry.getLeft(),
	// entry.getRight(), head, rec);
	// recursions.add(recursion);
	// }
	// }
	// }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private AnalysisState<A> fixReachability(
			AnalysisState<A> entryState)
			throws FixpointException {
		try {
			if (entryState.getLatticeInstance(ReachLattice.class) != null) {
				ProgramState<A> exec = entryState.getExecution();
				SimpleAbstractState sas = (SimpleAbstractState) exec.getState();
				ValueLatticeProduct values = (ValueLatticeProduct) sas.valueState;
				entryState = entryState.withExecution(
						new ProgramState(
								new SimpleAbstractState(
										sas.heapState,
										new ValueLatticeProduct(new ReachLattice(ReachabilityStatus.REACHABLE, null),
												(ValueLattice) values.second),
										sas.typeState),
								exec.getComputedExpressions(),
								exec.getFixpointInformation()));
			}

			return entryState;
		} catch (SemanticException e) {
			throw new FixpointException(e);
		}
	}

	private void processEntrypoints(
			AnalysisState<A> entryState,
			ContextSensitivityToken empty,
			Collection<CFG> entryPoints) {
		for (CFG cfg : IterationLogger.iterate(LOG, entryPoints, "Processing entrypoints", "entries"))
			try {
				token = empty;
				AnalysisState<A> entryStateCFG = prepareEntryStateOfEntryPoint(entryState, cfg);
				results.putResult(
						cfg,
						empty,
						cfg.fixpoint(entryStateCFG, this, WorkingSet.of(workingSet), conf, empty));
			} catch (SemanticException e) {
				throw new AnalysisExecutionException("Error while creating the entrystate for " + cfg, e);
			} catch (FixpointException e) {
				throw new AnalysisExecutionException("Error while computing fixpoint for entrypoint " + cfg, e);
			}
	}

	@Override
	public Collection<AnalyzedCFG<A>> getAnalysisResultsOf(
			CFG cfg) {
		if (results.contains(cfg))
			return results.getState(cfg).getAll();
		else
			return Collections.emptySet();
	}

	/**
	 * Runs a fixpoint over the given {@link CFG}.
	 * 
	 * @param cfg        the target of the fixpoint
	 * @param token      the scope identifier that identifies the computation
	 * @param entryState the entry state for the fixpoint
	 * 
	 * @return the result of the fixpoint computation
	 * 
	 * @throws FixpointException if the fixpoint terminates abruptly
	 * @throws SemanticException if an exception happens while storing the
	 *                               result of the fixpoint
	 */
	private AnalyzedCFG<A> computeFixpoint(
			CFG cfg,
			ContextSensitivityToken token,
			AnalysisState<A> entryState)
			throws FixpointException,
			SemanticException {
		AnalyzedCFG<A> fixpointResult = cfg.fixpoint(entryState, this, WorkingSet.of(workingSet), conf, token);
		if (shouldStoreFixpointResults()) {
			Pair<Boolean, AnalyzedCFG<A>> res = results.putResult(cfg, token, fixpointResult);
			if (shouldStoreFixpointResults() && Boolean.TRUE.equals(res.getLeft()))
				triggers.add(cfg);
			fixpointResult = res.getRight();
		}
		return fixpointResult;
	}

	/**
	 * Whether or not this analysis can avoid computing a fixpoint for the given
	 * cfg when it is invoked by a call, and shortcut to the result for the same
	 * token if it exists and if it was produced with a greater entry state.
	 *
	 * @param cfg the cfg under evaluation
	 * 
	 * @return {@code true} if that condition holds (defaults to {@code true})
	 */
	protected boolean canShortcut(
			CFG cfg) {
		return true;
	}

	/**
	 * Whether or not this analysis should look for recursions when evaluating
	 * calls, immediately returning bottom when one is found.
	 * 
	 * @return {@code true} if that condition holds (defaults to {@code true})
	 */
	protected boolean shouldCheckForRecursions() {
		return true;
	}

	/**
	 * Whether or not this analysis should store the results of fixpoint
	 * executions for them to be returned as part of
	 * {@link #getFixpointResults()}.
	 * 
	 * @return {@code true} if that condition holds (defaults to {@code true})
	 */
	protected boolean shouldStoreFixpointResults() {
		return true;
	}

	@Override
	public FixpointResults<A> getFixpointResults() {
		return results;
	}

	private Pair<AnalysisState<A>, ExpressionSet[]> prepareEntryState(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions,
			ScopeToken scope,
			CFG cfg)
			throws SemanticException {
		Parameter[] formals = cfg.getDescriptor().getFormals();

		// prepare the state for the call: hide the visible variables
		Pair<AnalysisState<A>,
				ExpressionSet[]> scoped = call.getProgram()
						.getFeatures()
						.getScopingStrategy()
						.scope(call, scope, entryState, analysis, parameters);
		AnalysisState<A> callState = scoped.getLeft();
		ExpressionSet[] locals = scoped.getRight();

		// assign parameters between the caller and the callee contexts
		ParameterAssigningStrategy strategy = call.getProgram().getFeatures().getAssigningStrategy();
		Pair<AnalysisState<A>,
				ExpressionSet[]> prepared = strategy.prepare(call, callState, this, expressions, formals, locals);
		return prepared;
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		callgraph.registerCall(call);

		// if (shouldCheckForRecursions()
		// && (call.getTargetedCFGs().stream().anyMatch(call.getCFG()::equals)
		// ||
		// callgraph.getCalleesTransitively(call.getTargets()).contains(call.getCFG())))
		// {
		// // this calls introduces a loop in the call graph -> recursion
		// // we need a special fixpoint to compute its result
		// // we compute that at the end of each fixpoint iteration
		// pendingRecursions = true;
		// LOG.info("Found recursion at " + call.getLocation());
		//
		// // we return bottom for now
		// if (call.returnsVoid(null))
		// return entryState.bottomExecution();
		// else
		// return
		// entryState.bottomExecution().withExecutionExpression(call.getMetaVariable());
		// }

		ContextSensitivityToken callerToken = token;
		token = token.push(call);
		if (((JavaKDepthToken) callerToken).length() == ((JavaKDepthToken) token).length()) {
			if (call.returnsVoid(null))
				return entryState.topExecution();
			else
				return entryState.topExecution().withExecutionExpression(call.getMetaVariable());
		}

		ScopeToken scope = new ScopeToken(call);

		// we exclude erroneous/halting executions from the
		// initial states, since they will not be affected
		// by the call; they are still part of the result
		// and they will be lubbed with the return values
		AnalysisState<A> result = entryState.bottomExecution();
		AnalysisState<A> initialState = entryState.removeAllErrors(true);

		// compute the result over all possible targets, and take the lub of
		// the results
		for (CFG cfg : call.getTargetedCFGs()) {
			CFGResults<A> localResults = results.get(cfg);
			AnalyzedCFG<A> states = localResults == null ? null : localResults.get(token);
			Pair<AnalysisState<A>,
					ExpressionSet[]> prepared = prepareEntryState(
							call,
							initialState,
							parameters,
							expressions,
							scope,
							cfg);

			AnalysisState<A> exitState;

			if (canShortcut(cfg) && states != null && prepared.getLeft().lessOrEqual(states.getEntryState()))
				// no need to compute the fixpoint: we already have an
				// (over-)approximation of the result computed starting from
				// an over-approximation of the entry state
				exitState = states.getExitState();
			else {
				// compute the result with a fixpoint iteration
				AnalyzedCFG<A> fixpointResult = null;
				try {
					fixpointResult = computeFixpoint(cfg, token, prepared.getLeft());
				} catch (FixpointException e) {
					throw new SemanticException("Exception during the interprocedural analysis", e);
				}

				AnalysisState<A> returnExitState = initialState.bottom();
				AnalysisState<A> throwExitState = initialState.bottom();

				for (Statement exit : fixpointResult.getAllExitpoints())
					if (exit instanceof Ret || exit instanceof Return)
						returnExitState = returnExitState.lub(fixpointResult.getAnalysisStateAfter(exit));
					else
						throwExitState = throwExitState.lub(fixpointResult.getAnalysisStateAfter(exit));

				// if the normal state is bottom, we take the throw state
				if (returnExitState.getExecutionState().isBottom())
					exitState = throwExitState;
				// if there are no errors, we also lub the states of the throws
				// without followers
				else if (returnExitState.getErrors().isBottom()) {
					exitState = returnExitState;
					for (Statement exit : fixpointResult.getAllExitpoints())
						if (exit instanceof Throw && exit.getCFG().getOutgoingEdges(exit).isEmpty())
							exitState = exitState.lub(fixpointResult.getAnalysisStateAfter(exit));
				} else
					exitState = returnExitState.lub(throwExitState);
			}

			// save the resulting state
			ScopingStrategy strategy = call.getProgram().getFeatures().getScopingStrategy();
			AnalysisState<A> callres = strategy.unscope(call, scope, exitState, analysis);
			callres = analysis.transferThrowers(callres, call, cfg);
			result = result.lub(callres);
		}

		token = callerToken;
		return result;
	}

}

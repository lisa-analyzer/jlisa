package it.unive.jlisa.interprocedural;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.CFGResults;
import it.unive.lisa.interprocedural.FixpointResults;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.NoEntryPointException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.Recursion;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.KDepthToken;
import it.unive.lisa.interprocedural.events.CFGFixpointEnd;
import it.unive.lisa.interprocedural.events.CFGFixpointStart;
import it.unive.lisa.interprocedural.events.CFGFixpointStored;
import it.unive.lisa.interprocedural.events.FixpointEnd;
import it.unive.lisa.interprocedural.events.FixpointIterationEnd;
import it.unive.lisa.interprocedural.events.FixpointIterationStart;
import it.unive.lisa.interprocedural.events.FixpointStart;
import it.unive.lisa.interprocedural.events.RecursionEnd;
import it.unive.lisa.interprocedural.events.RecursionStart;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.util.StringUtilities;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A {@link ContextBasedAnalysis} where whether a call is a recursion is decided
 * by {@link #isRecursion}, that subclasses can override (e.g. to unroll
 * recursive calls, see {@link JavaEntryStateContextBasedAnalysis}), and whose
 * recursions are solved by {@link JavaRecursionSolver}. As the fixpoint over
 * the calls of LiSA's {@link ContextBasedAnalysis} keeps the cfgs to analyze
 * again and the pending recursions in private fields, and solves the recursions
 * with its own solver, the fixpoint is implemented here (as in LiSA), while the
 * evaluation of the calls that are not recursions is the one of
 * {@link ContextBasedAnalysis}. Differently from LiSA, a recursion is solved
 * only from the calls entering it (its starters) that led to a call found to be
 * a recursion: the results of the other ones (e.g. {@code fib(9)}, unrolled by
 * {@link JavaEntryStateContextBasedAnalysis}) are kept.
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

	// the cfgs whose results changed during a fixpoint iteration
	private final Collection<CodeMember> triggers;

	// whether a recursion has been found during a fixpoint iteration
	private boolean pendingRecursions;

	private WorkingSet<Statement> workingSet;

	// the calls being evaluated, outermost first
	private final Deque<CFGCall> calls = new ArrayDeque<>();

	// the starters of the recursions to solve (see starter), or null if all
	// of them have to be solved
	private Set<Call> starters = new HashSet<>();

	// whether the next check for recursions of ContextBasedAnalysis has to be
	// skipped, as this analysis already checked the call (see
	// getAbstractResultOf)
	private boolean checked;

	/**
	 * Builds the analysis.
	 *
	 * @param k the maximum depth of the call stacks distinguishing the calls
	 *              (the whole stack if negative, see {@link KDepthToken})
	 */
	public JavaContextBasedAnalysis(
			int k) {
		super(k);
		triggers = new HashSet<>();
	}

	/**
	 * Builds the analysis, sharing the results and the state of the fixpoint of
	 * the given one (e.g. to solve a recursion, see
	 * {@link JavaRecursionSolver}).
	 *
	 * @param other the analysis
	 */
	protected JavaContextBasedAnalysis(
			JavaContextBasedAnalysis<A, D> other) {
		super(other);
		this.triggers = other.triggers;
		this.workingSet = other.workingSet;
		this.pendingRecursions = false;
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			EventQueue events,
			Analysis<A, D> analysis)
			throws InterproceduralAnalysisException {
		super.init(app, callgraph, policy, events, analysis);
		this.workingSet = null;
		this.pendingRecursions = false;
		this.checked = false;
		this.triggers.clear();
		this.calls.clear();
		this.starters = new HashSet<>();
	}

	@Override
	public void fixpoint(
			AnalysisState<A> entryState,
			FixpointConfiguration<A, D> conf)
			throws FixpointException {
		if (conf.forwardFixpoint == null)
			throw new IllegalArgumentException("A forward fixpoint is required for this analysis");

		this.workingSet = conf.fixpointWorkingSet;
		this.conf = conf;

		// new fixpoint execution: reset
		CodeUnit unit = new CodeUnit(SyntheticLocation.INSTANCE, app.getPrograms()[0], "singleton");
		CFG singleton = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "singleton"));
		KDepthToken<A> empty = token.startingId();
		AnalyzedCFG<A> graph = conf.usesOptimizedForwardFixpoint()
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

		if (events != null)
			events.post(new FixpointStart());

		int iter = 1;
		do {
			LOG.info("Performing {} fixpoint iteration", StringUtilities.ordinal(iter));
			if (events != null)
				events.post(new FixpointIterationStart(iter));

			triggers.clear();
			pendingRecursions = false;
			starters = new HashSet<>();

			processEntrypoints(entryState, empty, entryPoints);

			if (pendingRecursions) {
				if (events != null)
					events.post(new RecursionStart());

				Set<Recursion<A>> recursions = new HashSet<>();

				for (Collection<CodeMember> rec : callgraph.getRecursions())
					try {
						recursions.addAll(buildRecursionsFor(entryState, rec, results, conf));
					} catch (SemanticException e) {
						throw new FixpointException("Unable to build recursion", e);
					}

				solveRecursions(recursions);

				if (events != null)
					events.post(new RecursionEnd());
			}

			// starting from the callers of the cfgs that needed a lub,
			// find out the complete set of cfgs that might need to be
			// processed again
			Collection<CodeMember> toRemove = callgraph.getCallersTransitively(triggers);
			toRemove.removeAll(triggers);
			toRemove.stream().filter(CFG.class::isInstance).map(CFG.class::cast).forEach(results::forget);

			if (events != null)
				events.post(new FixpointIterationEnd(iter));
			iter++;
		} while (!triggers.isEmpty());

		if (events != null)
			events.post(new FixpointEnd());
	}

	@SuppressWarnings("unchecked")
	private Collection<Recursion<A>> buildRecursionsFor(
			AnalysisState<A> state,
			Collection<CodeMember> rec,
			FixpointResults<A> results,
			FixpointConfiguration<A, D> conf)
			throws SemanticException {
		// these are the calls that start the recursion by invoking
		// one of its members
		Collection<Call> starters = callgraph.getCallSites(rec)
				.stream()
				.filter(site -> !rec.contains(site.getCFG()))
				.filter(site -> this.starters == null || this.starters.contains(root(site)))
				.collect(Collectors.toSet());

		Collection<Recursion<A>> recursions = new LinkedList<>();
		for (Call starter : starters) {
			// these are the head of the recursion: members invoked
			// from outside of it
			Set<CFG> heads = callgraph.getCallees(starter.getCFG())
					.stream()
					.filter(callee -> rec.contains(callee))
					.filter(CFG.class::isInstance)
					.map(CFG.class::cast)
					.collect(Collectors.toSet());
			Set<Pair<KDepthToken<A>, CompoundState<A>>> entries = new HashSet<>();
			for (Entry<ScopeId<A>, AnalyzedCFG<A>> res : results.get(starter.getCFG())) {
				StatementStore<A> params = new StatementStore<>(state.bottom());
				Expression[] parameters = starter.getParameters();
				if (conf.usesOptimizedForwardFixpoint())
					for (Expression actual : parameters)
						params.put(
								actual,
								((OptimizedAnalyzedCFG<A, D>) res.getValue())
										.getUnwindedAnalysisStateAfter(actual, conf));
				else
					for (Expression actual : parameters)
						params.put(actual, res.getValue().getAnalysisStateAfter(actual));

				if (parameters.length == 0)
					entries.add(Pair.of((KDepthToken<A>) res.getKey(),
							CompoundState.of(res.getValue().getAnalysisStateBefore(starter), params)));
				else
					entries.add(
							Pair.of(
									(KDepthToken<A>) res.getKey(),
									CompoundState.of(params.getState(parameters[parameters.length - 1]), params)));
			}

			for (CFG head : heads)
				for (Pair<KDepthToken<A>, CompoundState<A>> entry : entries) {
					Recursion<A> recursion = new Recursion<>(starter, entry.getLeft(), entry.getRight(), head, rec);
					recursions.add(recursion);
				}
		}

		return recursions;
	}

	private void solveRecursions(
			Set<Recursion<A>> recursions) {
		List<Recursion<A>> orderedRecursions = new ArrayList<>(recursions.size());
		for (Recursion<A> rec : recursions) {
			int pos = 0;
			for (; pos < orderedRecursions.size(); pos++)
				if (orderedRecursions.get(pos).getMembers().contains(rec.getInvocation().getCFG()))
					// as the recursion at pos contains the member
					// invoking rec, rec must be solved before the
					// recursion at pos
					break;
			// if no match is found, add() will place the element at the
			// end (pos == size())
			// otherwise, elements will be shifted
			orderedRecursions.add(pos, rec);
		}

		try {
			for (Recursion<A> rec : orderedRecursions)
				// the solver stores the results of the members of the
				// recursion, adding them to triggers when they change
				new JavaRecursionSolver<>(this, rec).solve();
		} catch (SemanticException e) {
			throw new AnalysisExecutionException("Unable to solve one or more recursions", e);
		}
	}

	private void processEntrypoints(
			AnalysisState<A> entryState,
			KDepthToken<A> empty,
			Collection<CFG> entryPoints) {
		for (CFG cfg : IterationLogger.iterate(LOG, entryPoints, "Processing entrypoints", "entries"))
			try {
				token = empty;
				AnalysisState<A> entryStateCFG = prepareEntryStateOfEntryPoint(entryState, cfg);

				if (events != null)
					events.post(new CFGFixpointStart<>(cfg, token, entryState));

				AnalyzedCFG<A> fixpointResult = cfg.fixpoint(entryStateCFG, this, workingSet.mk(), conf, empty);

				if (events != null) {
					events.post(new CFGFixpointEnd<>(cfg, token, entryState, fixpointResult));
					events.post(new CFGFixpointStored<>(cfg, token, entryState, fixpointResult, fixpointResult));
				}

				results.putResult(cfg, empty, fixpointResult);
			} catch (SemanticException e) {
				throw new AnalysisExecutionException("Error while creating the entrystate for " + cfg, e);
			} catch (FixpointException e) {
				throw new AnalysisExecutionException("Error while computing fixpoint for entrypoint " + cfg, e);
			}
	}

	@Override
	protected AnalyzedCFG<A> computeFixpoint(
			CFG cfg,
			KDepthToken<A> token,
			AnalysisState<A> entryState)
			throws FixpointException,
			SemanticException {
		if (events != null)
			events.post(new CFGFixpointStart<>(cfg, token, entryState));

		AnalyzedCFG<A> fixpointResult = cfg.fixpoint(entryState, this, workingSet.mk(), conf, token);

		if (events != null)
			events.post(new CFGFixpointEnd<>(cfg, token, entryState, fixpointResult));

		if (shouldStoreFixpointResults()) {
			Pair<Boolean, AnalyzedCFG<A>> res = results.putResult(cfg, token, fixpointResult);
			if (Boolean.TRUE.equals(res.getLeft()))
				triggers.add(cfg);

			if (events != null)
				events.post(new CFGFixpointStored<>(cfg, token, entryState, fixpointResult, res.getRight()));

			fixpointResult = res.getRight();
		}

		return fixpointResult;
	}

	@Override
	protected boolean shouldCheckForRecursions() {
		if (checked) {
			// the call has already been checked by getAbstractResultOf
			checked = false;
			return false;
		}
		return true;
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		if (shouldCheckForRecursions()) {
			callgraph.registerCall(call);
			if (isRecursion(call, entryState, parameters, expressions)) {
				// this calls introduces a loop in the call graph ->
				// recursion: its result is computed with a special fixpoint
				// at the end of each fixpoint iteration
				pendingRecursions = true;
				LOG.info("Found recursion at " + call.getLocation());
				Call starter = starter(call);
				if (starter == null)
					starters = null;
				else if (starters != null)
					starters.add(starter);

				// we return bottom for now
				if (call.returnsVoid(null))
					return entryState.bottomExecution();
				else
					return entryState.bottomExecution().withExecutionExpression(call.getMetaVariable());
			}
			// the check of ContextBasedAnalysis, that happens first, is
			// skipped
			checked = true;
		}
		calls.addLast(call);
		try {
			return super.getAbstractResultOf(call, entryState, parameters, expressions);
		} finally {
			calls.removeLast();
		}
	}

	// the outermost call being evaluated that enters, from outside, a
	// recursion containing the given call, or null if there is none
	private Call starter(
			CFGCall call) {
		Set<CodeMember> members = new HashSet<>();
		callgraph.getRecursionsContaining(call.getCFG()).forEach(members::addAll);
		for (CFGCall c : calls)
			if (!members.contains(c.getCFG()) && c.getTargetedCFGs().stream().anyMatch(members::contains))
				return root(c);
		return null;
	}

	// the call of the program a call has been resolved from, as registered in
	// the call graph
	private static Call root(
			Call call) {
		Call root = call;
		while (root.getSource() != null)
			root = root.getSource();
		return root;
	}

	/**
	 * Yields whether the given call is a recursion, whose result is computed
	 * with a fixpoint over the recursion: by default, whether the call
	 * introduces a loop in the call graph.
	 *
	 * @param call        the call
	 * @param entryState  the state where the call happens
	 * @param parameters  the values of the arguments
	 * @param expressions the states of the arguments
	 *
	 * @return whether the call is a recursion
	 *
	 * @throws SemanticException if the analysis fails
	 */
	protected boolean isRecursion(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		return call.getTargetedCFGs().stream().anyMatch(call.getCFG()::equals)
				|| callgraph.getCalleesTransitively(call.getTargets()).contains(call.getCFG());
	}
}

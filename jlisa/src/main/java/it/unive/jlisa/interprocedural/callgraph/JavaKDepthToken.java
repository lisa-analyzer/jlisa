package it.unive.jlisa.interprocedural.callgraph;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.util.collections.CollectionUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO: Consider removing this class in the future.
 *
 * This class is used by JavaContextBasedAnalysis to track the recursion depth of
 * the analysis and expose the current length of the CFGCalls collection.
 *
 * Differences between LiSA's KDepthToken:
 * - Provides a method to retrieve the current size of the calls collection.
 * - Primarily needed for JavaContextBasedAnalysis to function properly.
 *
 * Notes:
 * - Its functionality may not be strictly necessary if JavaContextBasedAnalysis
 *   is removed or refactored.
 * - Before the next edition of SVCOMP, revisit whether this class is required,
 *   and remove it if not needed.
 */
public class JavaKDepthToken<A extends AbstractLattice<A>>
		implements
		ScopeId<A> {

	private final List<CFGCall> calls;

	private final int k;

	private JavaKDepthToken(
			int k) {
		this.k = k;
		this.calls = Collections.emptyList();
	}

	private JavaKDepthToken(
			int k,
			JavaKDepthToken<A> source,
			CFGCall newToken) {
		this.k = k;

		if (k == 0) {
			// k = 0 -> insensitive
			this.calls = source.calls;
			return;
		}

		int oldlen = source.calls.size();
		if (k < 0 || oldlen < k) {
			// k < 0 -> full stack
			this.calls = new ArrayList<>(oldlen + 1);
			source.calls.forEach(this.calls::add);
			this.calls.add(newToken);
		} else {
			this.calls = new ArrayList<>(k);
			// we only keep the last k-1 elements
			source.calls.stream().skip(oldlen - k + 1).forEach(this.calls::add);
			this.calls.add(newToken);
		}
	}

	/**
	 * Return an empty token.
	 * 
	 * @param k the maximum depth
	 * 
	 * @return an empty token
	 */
	public static JavaKDepthToken getSingleton(
			int k) {
		return new JavaKDepthToken(k);
	}

	@Override
	public String toString() {
		if (calls.isEmpty())
			return "<empty>";
		return "["
				+ calls.stream().map(call -> call.getLocation())
						.collect(new CollectionUtilities.StringCollector<>(", "))
				+ "]";
	}

	/**
	 * Creates an empty token that can track at most {@code k} calls.
	 *
	 * @param <A> the type of {@link AbstractLattice} handled by the analysis
	 * @param k   the maximum depth
	 *
	 * @return an empty token
	 */
	public static <A extends AbstractLattice<A>> JavaKDepthToken<A> create(
			int k) {
		return new JavaKDepthToken<>(k);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JavaKDepthToken other = (JavaKDepthToken) obj;
		// we ignore k as it does not matter for equality
		if (calls == null) {
			if (other.calls != null)
				return false;
		} else if (!calls.equals(other.calls))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		// we ignore k as it does not matter for equality
		final int prime = 31;
		int result = 1;

		if (calls == null)
			result = prime * result;
		else
			for (CFGCall call : calls)
				// we use the hashcode of the location as the hashcode of the
				// call is based on the ones of its targets, and a CFG hashcode
				// is not consistent between executions - this is a problem as
				// this object's hashcode is used as suffix in some filenames
				result = prime * result + call.getLocation().hashCode();
		return result;
	}

	@Override
	public JavaKDepthToken<A> startingId() {
		return create(k);
	}

	@Override
	public boolean isStartingId() {
		return calls.isEmpty();
	}

	@Override
	public JavaKDepthToken<A> push(
			CFGCall c,
			AnalysisState<A> state) {
		return new JavaKDepthToken<>(k, this, c);
	}

	public int length() {
		return this.calls.size();
	}

}

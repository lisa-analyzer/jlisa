package it.unive.jlisa.witness.validator.violation;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import it.unive.jlisa.witness.validator.model.Interception;
import java.util.List;
import java.util.Optional;

/**
 * Pairs the {@code Verifier.nondet*()} calls of one run with the
 * {@code function_return} waypoints of a witness, in the order of the witness:
 * a call takes the next waypoint not yet passed when it is at its location
 * (same file name, same line and, when the line has several nondet calls and
 * the witness gives a column, the call whose text contains that column); any
 * other call is not fixed by the witness.
 */
public final class WitnessCalls {

	private final List<Interception> interceptions;
	private final WitnessLocations locations;
	private final NondetCallSites sites = new NondetCallSites();
	private int next;

	/**
	 * @param interceptions the waypoints, in the order of the witness
	 * @param locations     the task sources, for the columns of the calls, or
	 *                          null to match lines only
	 */
	public WitnessCalls(
			List<Interception> interceptions,
			WitnessLocations locations) {
		this.interceptions = List.copyOf(interceptions);
		this.locations = locations;
	}

	/**
	 * Returns the waypoint the call from the given location passes, if it is
	 * the next one of the witness; the waypoint is then passed.
	 *
	 * @param caller where the nondet method is called from
	 */
	public Optional<Interception> match(
			Location caller) {
		if (next >= interceptions.size()) {
			return Optional.empty();
		}
		Interception ix = interceptions.get(next);
		if (!matches(ix, caller)) {
			return Optional.empty();
		}
		next++;
		return Optional.of(ix);
	}

	/** Returns the number of waypoints the run has not passed. */
	public int unused() {
		return interceptions.size() - next;
	}

	/** Returns the number of waypoints. */
	public int size() {
		return interceptions.size();
	}

	private boolean matches(
			Interception ix,
			Location caller) {
		String sourceName;
		try {
			sourceName = caller.sourceName();
		} catch (AbsentInformationException e) {
			return false;
		}
		if (caller.lineNumber() != ix.line() || !baseName(ix.fileName()).equals(sourceName)) {
			return false;
		}
		if (ix.column() == null || locations == null) {
			return true;
		}
		String sourcePath;
		try {
			sourcePath = caller.sourcePath();
		} catch (AbsentInformationException e) {
			return true;
		}
		List<int[]> calls = locations.nondetCalls(sourcePath, ix.line());
		if (calls.size() <= 1) {
			return true;
		}
		int[] rank = sites.rank(caller);
		if (rank == null || rank[1] != calls.size()) {
			ValidatorLogger.debug("Call at {}:{}: cannot tell its column, matched by line", sourceName, ix.line());
			return true;
		}
		int chosen = -1;
		for (int i = 0; i < calls.size(); i++) {
			int[] c = calls.get(i);
			// from 1, or from 0 as in some witnesses
			if (c[0] - 1 <= ix.column() && ix.column() <= c[1]) {
				if (chosen >= 0) {
					return true;
				}
				chosen = i;
			}
		}
		return chosen < 0 || chosen == rank[0];
	}

	/** Returns the file name of a path, with either separator. */
	public static String baseName(
			String path) {
		int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return slash < 0 ? path : path.substring(slash + 1);
	}
}

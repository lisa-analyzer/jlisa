package it.unive.lisa.analysis.apron;

import apron.Abstract1;

/**
 * Gives jlisa access to the constructor of {@link Apron} from a native state,
 * that is package-private in LiSA (this class is in the same package): jlisa
 * uses it to remove variables from the environment of a state, that Apron only
 * forgets (keeping a dimension for each of them).
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class ApronAccess {

	private ApronAccess() {
	}

	/**
	 * Returns the state of {@link Apron} wrapping the given native state.
	 *
	 * @param state the native state
	 *
	 * @return the state of Apron
	 */
	public static Apron of(
			Abstract1 state) {
		return new Apron(state);
	}
}

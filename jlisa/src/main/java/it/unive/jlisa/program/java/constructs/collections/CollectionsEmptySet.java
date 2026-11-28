package it.unive.jlisa.program.java.constructs.collections;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The method {@code Collections.emptySet}: see {@link CollectionsFactory}.
 */
public class CollectionsEmptySet extends CollectionsFactory {

	public CollectionsEmptySet(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "emptySet");
	}

	public static CollectionsEmptySet build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CollectionsEmptySet(cfg, location);
	}

	@Override
	protected String collectionType() {
		return "java.util.Set";
	}
}

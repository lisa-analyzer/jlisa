package it.unive.jlisa.program.java.constructs.collections;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The method {@code Collections.emptyList}: see {@link CollectionsFactory}.
 */
public class CollectionsEmptyList extends CollectionsFactory {

	public CollectionsEmptyList(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "emptyList");
	}

	public static CollectionsEmptyList build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CollectionsEmptyList(cfg, location);
	}

	@Override
	protected String collectionType() {
		return "java.util.List";
	}
}

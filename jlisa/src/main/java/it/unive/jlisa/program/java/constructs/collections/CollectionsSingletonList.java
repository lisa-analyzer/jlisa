package it.unive.jlisa.program.java.constructs.collections;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The method {@code Collections.singletonList}: see {@link CollectionsFactory}.
 */
public class CollectionsSingletonList extends CollectionsFactory {

	public CollectionsSingletonList(
			CFG cfg,
			CodeLocation location,
			Expression element) {
		super(cfg, location, "singletonList", element);
	}

	public static CollectionsSingletonList build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CollectionsSingletonList(cfg, location, params[0]);
	}

	@Override
	protected String collectionType() {
		return "java.util.List";
	}
}

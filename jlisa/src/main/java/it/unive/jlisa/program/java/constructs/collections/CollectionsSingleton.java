package it.unive.jlisa.program.java.constructs.collections;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The method {@code Collections.singleton}: see {@link CollectionsFactory}.
 */
public class CollectionsSingleton extends CollectionsFactory {

	public CollectionsSingleton(
			CFG cfg,
			CodeLocation location,
			Expression element) {
		super(cfg, location, "singleton", element);
	}

	public static CollectionsSingleton build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CollectionsSingleton(cfg, location, params[0]);
	}

	@Override
	protected String collectionType() {
		return "java.util.Set";
	}
}

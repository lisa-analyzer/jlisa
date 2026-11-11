package it.unive.jlisa.program.java.constructs.collections;

import it.unive.jlisa.program.java.constructs.lists.ListModel;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * {@code ConcurrentHashMap::newKeySet}: a new empty set that can be changed.
 */
public class ConcurrentHashMapNewKeySet extends CollectionsFactory {

	public ConcurrentHashMapNewKeySet(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "newKeySet");
	}

	public static ConcurrentHashMapNewKeySet build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ConcurrentHashMapNewKeySet(cfg, location);
	}

	@Override
	protected String collectionType() {
		return "java.util.HashSet";
	}

	@Override
	protected int fixed() {
		return ListModel.MODIFIABLE;
	}
}

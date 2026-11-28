package it.unive.jlisa.program.java.constructs.map;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The method {@code Map.Entry.getKey()}: see {@link MapEntryGet}.
 */
public class MapEntryGetKey extends MapEntryGet {

	public MapEntryGetKey(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "getKey", "$key", expr);
	}

	public static MapEntryGetKey build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapEntryGetKey(cfg, location, params[0]);
	}
}

package it.unive.jlisa.program.java.constructs.map;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The view {@code entrySet()} of a map: see {@link MapView}. Its elements are
 * entries with an unknown key and value.
 */
public class MapEntrySet extends MapView {

	public MapEntrySet(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, expr, true);
	}

	public static MapEntrySet build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapEntrySet(cfg, location, params[0]);
	}
}

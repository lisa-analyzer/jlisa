package it.unive.jlisa.program.java.constructs.map;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The method {@code Map.Entry.getValue()}: see {@link MapEntryGet}.
 */
public class MapEntryGetValue extends MapEntryGet {

	public MapEntryGetValue(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "getValue", "$value", expr);
	}

	public static MapEntryGetValue build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapEntryGetValue(cfg, location, params[0]);
	}
}

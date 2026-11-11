package it.unive.jlisa.program.java.constructs.collections;

import it.unive.jlisa.program.java.constructs.lists.ListModel;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * {@code Arrays.asList(a, b, ...)} with more than one element: a list with the
 * given elements, whose size cannot change.
 */
public class ArraysAsListElements extends CollectionsFactory {

	public ArraysAsListElements(
			CFG cfg,
			CodeLocation location,
			Expression... elements) {
		super(cfg, location, "asList", elements);
	}

	public static ArraysAsListElements build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ArraysAsListElements(cfg, location, params);
	}

	@Override
	protected String collectionType() {
		return "java.util.List";
	}

	@Override
	protected int fixed() {
		return ListModel.FIXED_SIZE;
	}
}

package it.unive.jlisa.program.java.constructs.lists;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * {@code Collection::addAll}: as {@link ListAppendElements}, but the argument
 * is a collection, and it raises a {@code NullPointerException} if it is null.
 */
public class ListAppendAll extends ListAppendElements {

	public ListAppendAll(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		super(cfg, location, params);
	}

	public static ListAppendAll build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListAppendAll(cfg, location, params);
	}

	@Override
	protected boolean collectionArgument() {
		return true;
	}
}

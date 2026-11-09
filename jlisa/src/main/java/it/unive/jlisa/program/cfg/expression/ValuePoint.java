package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A program point where an expression assumes a condition to compute one of its
 * values. It is not a statement, hence assuming the condition there does not
 * change reachability: the values of an expression are not branches of the
 * control flow.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
class ValuePoint implements ProgramPoint {

	private final Statement owner;

	/**
	 * Builds the program point.
	 *
	 * @param owner the expression that assumes the condition
	 */
	ValuePoint(
			Statement owner) {
		this.owner = owner;
	}

	@Override
	public CFG getCFG() {
		return owner.getCFG();
	}

	@Override
	public CodeLocation getLocation() {
		return owner.getLocation();
	}
}

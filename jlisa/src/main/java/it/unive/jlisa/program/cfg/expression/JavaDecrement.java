package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;

/**
 * The decrement operator of Java, {@code --x} or {@code x--}.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaDecrement extends JavaIncrementOrDecrement {

	/**
	 * Builds the decrement.
	 *
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where this expression is defined
	 * @param prefix        whether it is {@code --x} (or {@code x--})
	 * @param subExpression the decremented variable
	 */
	public JavaDecrement(
			CFG cfg,
			CodeLocation location,
			boolean prefix,
			Expression subExpression) {
		super(cfg, location, "--", NumericNonOverflowingSub.INSTANCE, prefix, subExpression);
	}
}

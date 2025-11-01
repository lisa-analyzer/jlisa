package it.unive.jlisa.analysis.value;

import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;

public class JavaNumericInterval extends Interval {

	@Override
	public IntInterval evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Number) {
			Double i = ((Number) constant.getValue()).doubleValue();
			return new IntInterval(new MathNumber(i), new MathNumber(i));
		}
		// If the constant is not a number, return BOTTOM.
		// TOP represents any possible number, but since the constant is not
		// numeric, BOTTOM is more appropriate.
		return IntInterval.BOTTOM;
	}

}

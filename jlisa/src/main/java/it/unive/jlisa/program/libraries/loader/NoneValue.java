package it.unive.jlisa.program.libraries.loader;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;

public class NoneValue implements Value {

	@Override
	public String toString() {
		return "NullValue";
	}

	@Override
	public Expression toLiSAExpression(
			CFG init) {
		return new NullLiteral(init, init.getDescriptor().getLocation());
	}
}

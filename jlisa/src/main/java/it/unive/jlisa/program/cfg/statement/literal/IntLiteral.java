package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;

public class IntLiteral extends Literal<Integer> {
	public IntLiteral(
			CFG cfg,
			CodeLocation location,
			int value) {
		super(cfg, location, value, JavaIntType.INSTANCE);
	}
}
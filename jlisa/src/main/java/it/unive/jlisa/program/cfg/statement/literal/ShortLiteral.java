package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.type.ShortType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;

public class ShortLiteral extends Literal<Integer> {
    public ShortLiteral(
            CFG cfg,
            CodeLocation location,
            int value) {
        super(cfg, location, value, ShortType.INSTANCE);
    }
}
package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.type.LongType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;

public class LongLiteral extends Literal<Long> {
    public LongLiteral(
            CFG cfg,
            CodeLocation location,
            long value) {
        super(cfg, location, value, LongType.INSTANCE);
    }
}
package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;

public class DoubleLiteral extends Literal<Double> {
    public DoubleLiteral(
            CFG cfg,
            CodeLocation location,
            double value) {
        super(cfg, location, value, JavaDoubleType.INSTANCE);
    }
}
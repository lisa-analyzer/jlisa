package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;

public class FloatLiteral extends Literal<Float> {
    public FloatLiteral(
            CFG cfg,
            CodeLocation location,
            float value) {
        super(cfg, location, value, JavaFloatType.INSTANCE);
    }
}
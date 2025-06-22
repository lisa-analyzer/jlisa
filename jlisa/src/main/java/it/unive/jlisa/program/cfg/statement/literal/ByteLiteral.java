package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.type.JavaByteType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;


public class ByteLiteral extends Literal<Byte> {
    public ByteLiteral(
            CFG cfg,
            CodeLocation location,
            byte value) {
        super(cfg, location, value, JavaByteType.INSTANCE);
    }
}

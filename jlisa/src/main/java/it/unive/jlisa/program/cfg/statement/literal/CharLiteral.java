package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.type.JavaCharType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.type.Type;

public class CharLiteral extends Literal<Character> {
    /**
     * Builds a char literal, consisting of a char constant value, happening at the
     * given location in the program.
     *
     * @param cfg        the cfg that this expression belongs to
     * @param location   the location where the expression is defined within the
     *                   program
     * @param value      the value of this literal
     */
    public CharLiteral(CFG cfg, CodeLocation location, char value) {
        super(cfg, location, value, JavaCharType.INSTANCE);
    }
}

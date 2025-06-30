package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.type.JavaCharType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;

public class CharLiteral extends Literal<Integer> {
    /**
     * Builds a char literal, consisting of a char constant value, happening at the
     * given location in the program.
     *
     * @param cfg        the cfg that this expression belongs to
     * @param location   the location where the expression is defined within the
     *                   program
     * @param value      the value of this literal
     */
    public CharLiteral(CFG cfg, CodeLocation location, int value) {
        super(cfg, location, value, JavaCharType.INSTANCE);
    }
    
    @Override
    public String toString() {
    	return "'" + Character.toString(getValue()) + "'";
    }
}

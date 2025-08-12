package it.unive.jlisa.program.type;

import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

import java.util.Collections;
import java.util.Set;

public class JavaIntType implements JavaNumericType {
    public static final JavaIntType INSTANCE = new JavaIntType();

    protected JavaIntType() {}

    @Override
    public String toString() {
        return "int";
    }

    @Override
    public Set<Type> allInstances(TypeSystem types) {
        return Collections.singleton(this);
    }

    @Override
    public boolean is8Bits() {
        return false;
    }

    @Override
    public boolean is16Bits() {
        return false;
    }

    @Override
    public boolean is32Bits() {
        return true;
    }

    @Override
    public boolean is64Bits() {
        return false;
    }

    @Override
    public boolean isUnsigned() {
        return false;
    }

    @Override
    public boolean isIntegral() {
        return true;
    }
    
    @Override
    public Expression defaultValue(CFG cfg, CodeLocation location) {
    	return new IntLiteral(cfg, location, 0);
    }

}

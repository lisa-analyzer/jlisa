package it.unive.jlisa.type;

import it.unive.jlisa.program.cfg.statement.literal.ByteLiteral;
import it.unive.jlisa.program.cfg.statement.literal.DoubleLiteral;
import it.unive.jlisa.program.cfg.statement.literal.FloatLiteral;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.jlisa.program.cfg.statement.literal.LongLiteral;
import it.unive.jlisa.program.cfg.statement.literal.ShortLiteral;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.FalseLiteral;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class JavaTypeSystem extends TypeSystem {

    @Override
    public BooleanType getBooleanType() {
        return BoolType.INSTANCE;
    }

    @Override
    public StringType getStringType() {
        return StringType.INSTANCE;
    }

    @Override
    public NumericType getIntegerType() {
        return JavaIntType.INSTANCE;
    }

    @Override
    public boolean canBeReferenced(Type type) {
        return type.isInMemoryType();
    }

    public static Literal<?> getDefaultLiteral(Type type, CFG currentCFG, CodeLocation location) {
        if (type == JavaIntType.INSTANCE) {
            return new IntLiteral(currentCFG, location, 0);
        } else if (type == BoolType.INSTANCE) {
            return new FalseLiteral(currentCFG, location);
        } else if (type == JavaByteType.INSTANCE) {
            return new ByteLiteral(currentCFG, location, (byte) 0);
        } else if (type == JavaShortType.INSTANCE) {
            return new ShortLiteral(currentCFG, location, (short) 0);
        } else if (type == JavaLongType.INSTANCE) {
            return new LongLiteral(currentCFG, location, 0L);
        } else if (type == JavaFloatType.INSTANCE) {
            return new FloatLiteral(currentCFG, location, 0.0f);
        } else if (type == JavaDoubleType.INSTANCE) {
            return new DoubleLiteral(currentCFG, location, 0.0);
        }
        return new NullLiteral(currentCFG, location);
    }
}

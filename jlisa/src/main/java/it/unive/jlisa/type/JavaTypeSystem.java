package it.unive.jlisa.type;

import it.unive.jlisa.program.cfg.statement.literal.*;
import it.unive.jlisa.program.type.*;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.*;
import it.unive.lisa.program.type.*;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.NumericType;

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
        return IntType.INSTANCE;
    }

    @Override
    public boolean canBeReferenced(Type type) {
        return type.isInMemoryType();
    }

    public static Literal<?> getDefaultLiteral(Type type, CFG currentCFG, CodeLocation location) {
        if (type == IntType.INSTANCE) {
            return new IntLiteral(currentCFG, location, 0);
        } else if (type == BoolType.INSTANCE) {
            return new FalseLiteral(currentCFG, location);
        } else if (type == ByteType.INSTANCE) {
            return new ByteLiteral(currentCFG, location, (byte) 0);
        } else if (type == ShortType.INSTANCE) {
            return new ShortLiteral(currentCFG, location, (short) 0);
        } else if (type == LongType.INSTANCE) {
            return new LongLiteral(currentCFG, location, 0L);
        } else if (type == FloatType.INSTANCE) {
            return new FloatLiteral(currentCFG, location, 0.0f);
        } else if (type == DoubleType.INSTANCE) {
            return new DoubleLiteral(currentCFG, location, 0.0);
        }
        return new NullLiteral(currentCFG, location);
    }
}

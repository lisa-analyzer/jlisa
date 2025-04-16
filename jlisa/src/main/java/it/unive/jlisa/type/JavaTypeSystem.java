package it.unive.jlisa.type;

import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int64Type;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;

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
        return Int32Type.INSTANCE;
    }

    @Override
    public boolean canBeReferenced(Type type) {
        return type.isInMemoryType();
    }

    public boolean booleanDefaultValue() { return false; }

    public int intDefaultValue() { return 0; }

    public static Object getDefaultValue(Type type) {
        if (type == Int32Type.INSTANCE) {
            return 0;
        }
        if (type == BoolType.INSTANCE) {
            return false;
        }
        if (type == Int64Type.INSTANCE) {
            return 0L;
        }
        return null;
    }
}

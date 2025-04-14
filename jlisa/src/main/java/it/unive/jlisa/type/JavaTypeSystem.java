package it.unive.jlisa.type;

import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.type.*;

public class JavaTypeSystem extends TypeSystem {

    @Override
    public BooleanType getBooleanType() {
        return BoolType.INSTANCE;
    }

    @Override
    public StringType getStringType() {
        return it.unive.lisa.program.type.StringType.INSTANCE;
    }

    @Override
    public NumericType getIntegerType() {
        return Int32Type.INSTANCE;
    }

    @Override
    public boolean canBeReferenced(Type type) {
        return type.isInMemoryType();
    }
}

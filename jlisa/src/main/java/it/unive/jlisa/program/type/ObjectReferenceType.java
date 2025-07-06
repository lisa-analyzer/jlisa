package it.unive.jlisa.program.type;

import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.type.ReferenceType;

public class ObjectReferenceType {
    public static ReferenceType get() {
        return new ReferenceType(JavaClassType.lookup("Object").orElseThrow());
    }
}
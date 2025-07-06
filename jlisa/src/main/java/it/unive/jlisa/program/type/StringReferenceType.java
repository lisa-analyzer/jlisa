package it.unive.jlisa.program.type;

import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.StringType;

public class StringReferenceType {
    public static ReferenceType get() {
      return new ReferenceType(JavaClassType.lookup("String").orElseThrow());
    }
}
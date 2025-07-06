package it.unive.jlisa.program.type;

import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.Type;

public class JavaInstrumentedStringType extends StringType {
    public static JavaInstrumentedStringType INSTANCE = new JavaInstrumentedStringType();
    
    @Override
	public boolean canBeAssignedTo(
			Type other) {
		return !other.isPointerType() && (other.isStringType() || other.isUntyped());
	}

}

package it.unive.jlisa.program.type;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class ByteType implements JavaNumericType {
	public static final ByteType INSTANCE = new ByteType();

	@Override
	public String toString() {
		return "byte";
	}

	@Override
	public int getNBits() {
		return 8;
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
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}
}

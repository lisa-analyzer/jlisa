package it.unive.jlisa.program.type;

import it.unive.jlisa.program.cfg.statement.literal.ByteLiteral;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaByteType implements JavaNumericType {
	public static final JavaByteType INSTANCE = new JavaByteType();

	protected JavaByteType() {
	}

	/**
	 * Checks whether {@code value} fits the Java's {@code byte} type.
	 * 
	 * @param value the long value to check
	 * 
	 * @return {@code true} if {@code value} is within the range of a
	 *             {@code byte}, {@code false} otherwise
	 */
	public static boolean fitsInType(
			long value) {
		return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
	}

	@Override
	public String toString() {
		return "byte";
	}

	@Override
	public boolean is8Bits() {
		return true;
	}

	@Override
	public boolean is16Bits() {
		return false;
	}

	@Override
	public boolean is32Bits() {
		return false;
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
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}

	@Override
	public Expression defaultValue(
			CFG cfg,
			CodeLocation location) {
		return new ByteLiteral(cfg, location, (byte) 0);
	}
}

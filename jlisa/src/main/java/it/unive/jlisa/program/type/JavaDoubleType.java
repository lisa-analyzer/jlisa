package it.unive.jlisa.program.type;

import it.unive.jlisa.program.cfg.statement.literal.DoubleLiteral;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaDoubleType implements JavaNumericType {
	public static final JavaDoubleType INSTANCE = new JavaDoubleType();

	protected JavaDoubleType() {
	}

	@Override
	public String toString() {
		return "double";
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}

	@Override
	public int getNBits() {
		return 64;
	}

	@Override
	public boolean isUnsigned() {
		return false;
	}

	@Override
	public boolean isIntegral() {
		return false;
	}

	@Override
	public Expression defaultValue(
			CFG cfg,
			CodeLocation location) {
		return new DoubleLiteral(cfg, location, 0.0);
	}
}

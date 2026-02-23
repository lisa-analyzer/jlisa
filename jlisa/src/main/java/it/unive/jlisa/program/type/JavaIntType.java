package it.unive.jlisa.program.type;

import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaIntType implements JavaNumericType {
	public static final JavaIntType INSTANCE = new JavaIntType();

	protected JavaIntType() {
	}

	@Override
	public String toString() {
		return "int";
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}

	@Override
	public int getNBits() {
		return 32;
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
	public boolean canBeAssignedTo(
			Type other) {
		if (other == JavaCharType.INSTANCE)
			return true;
		return JavaNumericType.super.canBeAssignedTo(other);
	}

	@Override
	public Expression defaultValue(
			CFG cfg,
			CodeLocation location) {
		return new IntLiteral(cfg, location, 0);
	}

}

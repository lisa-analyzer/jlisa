package it.unive.jlisa.program.type;

import it.unive.jlisa.program.cfg.statement.literal.LongLiteral;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaLongType implements JavaNumericType {
	public static final JavaLongType INSTANCE = new JavaLongType();

	protected JavaLongType() {
	}

	@Override
	public String toString() {
		return "long";
	}

	@Override
	public boolean canBeAssignedTo(
			Type other) {
		return (other.isUntyped() || other instanceof JavaLongType || other instanceof JavaDoubleType
				|| other instanceof JavaFloatType);
	}

	@Override
	public Type commonSupertype(
			Type other) {
		if (other instanceof JavaFloatType) {
			return other;
		}
		return JavaNumericType.super.commonSupertype(other);
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
		return true;
	}

	@Override
	public Expression defaultValue(
			CFG cfg,
			CodeLocation location) {
		return new LongLiteral(cfg, location, 0L);
	}
}

package it.unive.jlisa.program.type;

import it.unive.jlisa.program.cfg.statement.literal.JavaNullLiteral;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.HashSet;
import java.util.Set;

public class JavaReferenceType extends ReferenceType {

	public JavaReferenceType(
			Type t) {
		super(t);
	}

	@Override
	public Expression defaultValue(
			CFG cfg,
			CodeLocation location) {
		return new JavaNullLiteral(cfg, location);
	}

	@Override
	public Expression unknownValue(
			CFG cfg,
			CodeLocation location) {
		return getInnerType().unknownValue(cfg, location);
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		Set<Type> instances = new HashSet<>();
		for (Type inner : getInnerType().allInstances(types))
			instances.add(new JavaReferenceType(inner));
		instances.add(this);
		return instances;
	}
}

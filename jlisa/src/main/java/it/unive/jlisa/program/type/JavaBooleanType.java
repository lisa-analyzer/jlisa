package it.unive.jlisa.program.type;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.literal.FalseLiteral;
import it.unive.lisa.program.type.BoolType;

public class JavaBooleanType extends BoolType {

	public static final JavaBooleanType INSTANCE = new JavaBooleanType();

	@Override
	public Expression defaultValue(
			CFG cfg,
			CodeLocation location) {
		return new FalseLiteral(cfg, location);
	}
}

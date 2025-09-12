package it.unive.jlisa.program.type;

import it.unive.jlisa.program.cfg.statement.literal.JavaNullLiteral;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;

public class JavaReferenceType extends ReferenceType {

	public JavaReferenceType(Type t) {
		super(t);
	}

	@Override
	public Expression defaultValue(CFG cfg, CodeLocation location) {
		return new JavaNullLiteral(cfg, location);
	}
	
	@Override
	public Expression unknownValue(CFG cfg, CodeLocation location) {
		return getInnerType().unknownValue(cfg, location);
	}
}

package it.unive.jlisa.program.type;

import it.unive.jlisa.analysis.JavaNullLiteral;
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
}

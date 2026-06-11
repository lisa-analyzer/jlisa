package it.unive.jlisa.program.type;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

public class JavaMetaClassType extends JavaClassType {

	public JavaMetaClassType(
			CompilationUnit unit) {
		super("java.lang.Class", unit);

		// we update the Class unit if only if not already registered
		if (!types.containsKey("java.lang.Class"))
			types.put("java.lang.Class", this);
	}

	@Override
	public Type commonSupertype(
			Type other) {
		return other instanceof JavaMetaClassType ? this : Untyped.INSTANCE;
	}

	@Override
	public String toString() {
		return "java.lang.Class";
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}

	@Override
	public CompilationUnit getUnit() {
		return this.unit;
	}

	@Override
	public Expression defaultValue(
			CFG cfg,
			CodeLocation location) {
		return new NullLiteral(cfg, location);
	}
}

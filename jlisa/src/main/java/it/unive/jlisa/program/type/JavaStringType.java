package it.unive.jlisa.program.type;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.type.InMemoryType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

public class JavaStringType extends JavaClassType implements StringType, InMemoryType, UnitType {

	public JavaStringType(
			CompilationUnit unit) {
		// TODO: this should work also with java.lang.String
		super("String", unit);

		// we update the String unit if only if not already registered
		if (!types.containsKey("String"))
			types.put("String", this);
	}

	@Override
	public Type commonSupertype(
			Type other) {
		return other.isStringType() ? this : Untyped.INSTANCE;
	}

	@Override
	public String toString() {
		return "String";
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

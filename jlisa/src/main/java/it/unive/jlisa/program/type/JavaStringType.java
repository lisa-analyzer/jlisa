package it.unive.jlisa.program.type;

import java.util.Collections;
import java.util.Set;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.type.InMemoryType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;

public class JavaStringType extends JavaClassType implements StringType, InMemoryType, UnitType {

	public JavaStringType(CompilationUnit unit) {
		super("String", unit);
		// TODO: this should work also with java.lang.String
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
}

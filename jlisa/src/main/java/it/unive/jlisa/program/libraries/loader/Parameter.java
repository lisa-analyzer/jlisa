package it.unive.jlisa.program.libraries.loader;

import it.unive.jlisa.program.cfg.JavaParameter;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import java.util.Objects;

public class Parameter {

	private final String name;
	private final Type type;
	private final Value value;
	private boolean isVararg = false;

	public Parameter(
			String name,
			Type type) {
		this(name, type, null, false);
	}

	public Parameter(
			String name,
			Type type,
			Value value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}

	public Parameter(
			String name,
			Type type,
			boolean isVararg) {
		this(name, type, null, isVararg);
	}

	public Parameter(
			String name,
			Type type,
			Value value,
			boolean isVararg) {
		this.name = name;
		this.type = type;
		this.value = value;
		this.isVararg = isVararg;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public Value getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, value, isVararg);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parameter other = (Parameter) obj;
		return Objects.equals(name, other.name) && Objects.equals(type, other.type)
				&& Objects.equals(value, other.value) && isVararg == other.isVararg;
	}

	@Override
	public String toString() {
		return "Parameter [name=" + name + ", type=" + type + ", value=" + value + ", isVararg=" + isVararg + "]";
	}

	public JavaParameter toLiSAParameter(
			Program program,
			CodeLocation location,
			CFG init) {
		Expression defValue = null;
		if (this.value != null) {
			defValue = this.value.toLiSAExpression(init);
		}

		return new JavaParameter(location, this.name, this.type.toLiSAType(program), defValue,
				new Annotations(), this.isVararg);
	}
}

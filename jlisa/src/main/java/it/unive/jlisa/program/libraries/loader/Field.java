package it.unive.jlisa.program.libraries.loader;

import java.util.Objects;

import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeLocation;

public class Field {

	private final boolean instance;
	private final String name;
	private final Type type;

	public Field(
			boolean instance,
			String name,
			Type type) {
		this.instance = instance;
		this.name = name;
		this.type = type;
	}

	public boolean isInstance() {
		return instance;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(instance, name, type);
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
		Field other = (Field) obj;
		return instance == other.instance && Objects.equals(name, other.name) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "Field [instance=" + instance + ", name=" + name + ", type=" + type + "]";
	}

	public Global toLiSAObject(
			Program program,
			CodeLocation location,
			Unit container) {
		it.unive.lisa.type.Type type = this.type.toLiSAType(program);
		return new Global(location, container, name, this.instance, type);
	}
}

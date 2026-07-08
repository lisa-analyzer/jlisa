package it.unive.jlisa.program.libraries.loader;

import it.unive.jlisa.program.libraries.loader.extensions.GlobalWithDefault;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import java.util.Objects;

public class Field {

	private final boolean instance;
	private final String name;
	private final Type type;
	private final Value value;

	public Field(
			boolean instance,
			String name,
			Type type) {
		this(instance, name, type, null);
	}

	public Field(
			boolean instance,
			String name,
			Type type,
			Value value) {
		this.instance = instance;
		this.name = name;
		this.type = type;
		this.value = value;
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

	public Value getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(instance, name, type, value);
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
		return instance == other.instance && Objects.equals(name, other.name) && Objects.equals(type, other.type)
				&& Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "Field [instance=" + instance + ", name=" + name + ", type=" + type + ", value=" + value + "]";
	}

	public Global toLiSAObject(
			Program program,
			CodeLocation location,
			CFG init,
			Unit container) {
		it.unive.lisa.type.Type type = this.type.toLiSAType(program);

		Expression defValue;
		if (this.value == null) {
			return new Global(location, container, name, this.instance, type);
		} else {
			defValue = this.value.toLiSAExpression(init);
			return new GlobalWithDefault(location, container, name, this.instance, type, defValue);
		}
	}
}

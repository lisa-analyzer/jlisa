package it.unive.jlisa.program.libraries.loader;

import it.unive.jlisa.program.libraries.LibrarySpecificationParser.LibraryCreationException;
import it.unive.lisa.program.Program;
import java.lang.reflect.Field;
import java.util.Objects;

public class LiSAType implements Type {
	private final String name;
	private final String field;

	public LiSAType(
			String name,
			String field) {
		this.name = name;
		this.field = field;
	}

	public String getName() {
		return name;
	}

	public String getField() {
		return field;
	}

	@Override
	public int hashCode() {
		return Objects.hash(field, name);
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
		LiSAType other = (LiSAType) obj;
		return Objects.equals(field, other.field) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "LiSAType [name=" + name + ", field=" + field + "]";
	}

	@Override
	public it.unive.lisa.type.Type toLiSAType(
			Program program) {
		try {
			Class<?> type = Class.forName(this.name);
			Field field = type.getField(this.field);
			return (it.unive.lisa.type.Type) field.get(null);
		} catch (ClassNotFoundException
				| NoSuchFieldException
				| SecurityException
				| IllegalArgumentException
				| IllegalAccessException e) {
			throw new LibraryCreationException(e);
		}
	}
}
package it.unive.jlisa.program.libraries.loader;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import it.unive.jlisa.program.libraries.JavaLibraryUnitType;
import it.unive.jlisa.program.libraries.LibrarySpecificationParser.LibraryCreationException;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;

public class Library {
	private final String name;
	private final String location;
	private final Collection<Method> methods = new HashSet<>();
	private final Collection<Field> fields = new HashSet<>();
	private final Collection<ClassDef> classes = new HashSet<>();

	public Library(
			String name,
			String location) {
		this.name = name;
		this.location = location;
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public Collection<Method> getMethods() {
		return methods;
	}

	public Collection<Field> getFields() {
		return fields;
	}

	public Collection<ClassDef> getClasses() {
		return classes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(classes, fields, location, methods, name);
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
		Library other = (Library) obj;
		return Objects.equals(classes, other.classes) && Objects.equals(fields, other.fields)
				&& Objects.equals(location, other.location) && Objects.equals(methods, other.methods)
				&& Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "Library [name=" + name + ", location=" + location + "]";
	}

	public CodeUnit toLiSAUnit(
			Program program,
			AtomicReference<CompilationUnit> rootHolder) {
		CodeLocation location = new SourceCodeLocation(this.location, 0, 0);
		CodeUnit unit = new CodeUnit(location, program, name);
		program.addUnit(unit);

		for (ClassDef cls : this.classes) {
			CompilationUnit c = cls.toLiSAUnit(location, program, rootHolder);
			program.addUnit(c);
			// type registration is a side effect of the constructor
			if (cls.getTypeName() == null)
				new JavaLibraryUnitType(unit, c);
			else
				try {
					Class<?> type = Class.forName(cls.getTypeName());
					Constructor<?> constructor = type.getConstructor(CompilationUnit.class);
					constructor.newInstance(c);
				} catch (ClassNotFoundException
						| SecurityException
						| IllegalArgumentException
						| IllegalAccessException
						| NoSuchMethodException
						| InstantiationException
						| InvocationTargetException e) {
					throw new LibraryCreationException(e);
				}
		}

		return unit;
	}

	public void populateUnit(
			CFG init,
			CompilationUnit root,
			CodeUnit lib) {
		CodeLocation location = new SourceCodeLocation(this.location, 0, 0);

		for (Method mtd : this.methods) {
			NativeCFG construct = mtd.toLiSACfg(location, init, lib);
			if (construct.getDescriptor().isInstance())
				throw new LibraryCreationException();
			lib.addCodeMember(construct);
		}

		for (Field fld : this.fields) {
			Global field = fld.toLiSAObject(location, lib);
			if (field.isInstance())
				throw new LibraryCreationException();
			lib.addGlobal(field);
		}

		for (ClassDef cls : this.classes)
			cls.populateUnit(location, init, root);
	}
}

package it.unive.jlisa.program.libraries.loader;


import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.libraries.JavaLibraryUnitType;
import it.unive.jlisa.program.libraries.LibrarySpecificationParser.LibraryCreationException;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.NativeCFG;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Library {
	private final String name;
	private final String location;
	private final Collection<Method> methods = new HashSet<>();
	private final Collection<Field> fields = new HashSet<>();
	private final Collection<ClassDef> classes = new HashSet<>();
	private final SourceCodeLocationManager locationManager;

	public Library(
			String name,
			SourceCodeLocationManager locationManager) {
		this.name = name;

		this.locationManager = locationManager;
		this.location = locationManager.getRoot().getCodeLocation();
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
		CodeUnit unit = new CodeUnit(locationManager.getRoot(), program, name);
		program.addUnit(unit);

		for (ClassDef cls : this.classes) {
			CompilationUnit c = cls.toLiSAUnit(locationManager.nextRow(), program, rootHolder);
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

		for (Method mtd : this.methods) {
			NativeCFG construct = mtd.toLiSACfg(locationManager.nextRow(), init, lib);
			if (construct.getDescriptor().isInstance())
				throw new LibraryCreationException();
			lib.addCodeMember(construct);
		}

		for (Field fld : this.fields) {
			Global field = fld.toLiSAObject(locationManager.nextRow(), lib);
			if (field.isInstance())
				throw new LibraryCreationException();
			lib.addGlobal(field);
		}

		for (ClassDef cls : this.classes)
			cls.populateUnit(locationManager, init, root);
	}
}

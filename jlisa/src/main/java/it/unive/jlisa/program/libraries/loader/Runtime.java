package it.unive.jlisa.program.libraries.loader;

import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.libraries.LibrarySpecificationParser.LibraryCreationException;
import it.unive.jlisa.program.type.JavaClassType;
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

public class Runtime {

	private final Collection<Method> methods = new HashSet<>();
	private final Collection<Field> fields = new HashSet<>();
	private final Collection<ClassDef> classes = new HashSet<>();
	private final SourceCodeLocationManager locationManager;

	public Runtime(
			SourceCodeLocationManager locationManager) {
		this.locationManager = locationManager;
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
		return Objects.hash(classes, fields, methods);
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
		Runtime other = (Runtime) obj;
		return Objects.equals(classes, other.classes) && Objects.equals(fields, other.fields)
				&& Objects.equals(methods, other.methods);
	}

	public void fillProgram(
			Program program,
			AtomicReference<CompilationUnit> rootHolder) {

		for (ClassDef cls : this.classes) {
			CompilationUnit c = cls.toLiSAUnit(locationManager.nextRow(), program, rootHolder);
			program.addUnit(c);
			// create the corresponding type
			if (cls.getTypeName() == null)
				JavaClassType.lookup(c.getName(), c);
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
	}

	public void populateProgram(
			Program program,
			CFG init,
			CompilationUnit root) {
		for (Method mtd : this.methods) {
			NativeCFG construct = mtd.toLiSACfg(locationManager.nextRow(), init, program);
			if (construct.getDescriptor().isInstance())
				throw new LibraryCreationException();
			program.addCodeMember(construct);
		}

		for (Field fld : this.fields) {
			Global field = fld.toLiSAObject(locationManager.nextRow(), program);
			if (field.isInstance())
				throw new LibraryCreationException();
			program.addGlobal(field);
		}

		for (ClassDef cls : this.classes)
			cls.populateUnit(locationManager, init, root);
	}
}

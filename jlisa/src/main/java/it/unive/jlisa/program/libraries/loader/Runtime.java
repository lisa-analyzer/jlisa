package it.unive.jlisa.program.libraries.loader;

import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.libraries.LibrarySpecificationParser.LibraryCreationException;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.NativeCFG;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

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

	public void addRuntimeMembers(
			Program program,
			CFG init,
			CompilationUnit root) {
		for (Method mtd : this.methods) {
			NativeCFG construct = mtd.toLiSACfg(program, locationManager.nextRow(), init, program);
			if (construct.getDescriptor().isInstance())
				throw new LibraryCreationException();
			program.addCodeMember(construct);
		}

		for (Field fld : this.fields) {
			Global field = fld.toLiSAObject(program, locationManager.nextRow(), program);
			if (field.isInstance())
				throw new LibraryCreationException();
			program.addGlobal(field);
		}
	}
}

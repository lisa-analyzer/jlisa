package it.unive.jlisa.program.libraries.loader;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.NativeCFG;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ClassDef {
	private final SyntheticCodeLocationManager locationManager;
	private final boolean root;
	private final boolean sealed;
	private final String typeName;
	private final String name;
	private final String base;
	private final boolean isInterface;
	private final List<String> interfaces;
	private final Collection<Method> methods = new HashSet<>();
	private final Collection<Field> fields = new HashSet<>();

	public ClassDef(
			SyntheticCodeLocationManager locationManager,
			boolean root,
			boolean sealed,
			String typeName,
			String name,
			String base,
			boolean isInterface) {
		this.locationManager = locationManager;
		this.root = root;
		this.sealed = sealed;
		this.typeName = typeName;
		this.name = name;
		this.base = base;
		this.isInterface = isInterface;
		this.interfaces = new ArrayList<String>();
	}

	public boolean isRoot() {
		return root;
	}

	public boolean isSealed() {
		return sealed;
	}

	public String getName() {
		return name;
	}

	public String getBase() {
		return base;
	}

	public boolean isInterface() {
		return isInterface;
	}

	public List<String> getInterfaces() {
		return interfaces;
	}

	public String getTypeName() {
		return typeName;
	}

	public Collection<Method> getMethods() {
		return methods;
	}

	public Collection<Field> getFields() {
		return fields;
	}

	@Override
	public int hashCode() {
		return Objects.hash(base, fields, methods, name, root, sealed, typeName);
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
		ClassDef other = (ClassDef) obj;
		return Objects.equals(base, other.base) && Objects.equals(fields, other.fields)
				&& Objects.equals(methods, other.methods) && Objects.equals(name, other.name) && root == other.root
				&& sealed == other.sealed && Objects.equals(typeName, other.typeName);
	}

	@Override
	public String toString() {
		return "ClassDef [root=" + root + ", sealed=" + sealed + ", typeName=" + typeName + ", name=" + name + ", base="
				+ base + ", methods=" + methods + ", fields=" + fields + "]";
	}

	public CompilationUnit toLiSAUnit(
			Program program,
			AtomicReference<CompilationUnit> rootHolder) {
		CompilationUnit unit;
		if (this.isInterface) {
			unit = new InterfaceUnit(locationManager.nextLocation(), program, name, this.sealed);
		} else {
			unit = new ClassUnit(locationManager.nextLocation(), program, name, this.sealed);
			if (this.root)
				if (rootHolder.get() != null)
					throw new IllegalStateException("More than one root class defined as hierarchy root");
				else
					rootHolder.set(unit);
		}
		return unit;
	}

	public CompilationUnit populateUnit(
			Program program,
			CFG init,
			CompilationUnit root) {

		CompilationUnit unit;
		if (this.isInterface) {
			unit = JavaInterfaceType.lookup(this.name).getUnit();
		} else {
			unit = JavaClassType.lookup(this.name).getUnit();
		}

		if (this.base != null) {
			LibrarySpecificationProvider.importClass(program, this.base);
			unit.addAncestor(JavaClassType.lookup(this.base).getUnit());
		} else if (root != null && unit != root)
			unit.addAncestor(root);

		for (String interf : this.interfaces) {
			LibrarySpecificationProvider.importClass(program, interf);
			JavaInterfaceType interfType = JavaInterfaceType.lookup(interf);
			assert (interfType != null);
			CompilationUnit interfUnit = interfType.getUnit();
			if (!unit.getImmediateAncestors().contains(interfUnit)) {
				unit.addAncestor(interfUnit);
			}
		}

		for (Method mtd : this.methods) {
			NativeCFG construct = mtd.toLiSACfg(program, locationManager.nextLocation(), init, unit);
			if (construct.getDescriptor().isInstance())
				unit.addInstanceCodeMember(construct);
			else
				unit.addCodeMember(construct);
		}

		for (Field fld : this.fields) {
			Global field = fld.toLiSAObject(program, locationManager.nextLocation(), unit);
			if (field.isInstance())
				unit.addInstanceGlobal(field);
			else
				unit.addGlobal(field);
		}

		return unit;
	}
}

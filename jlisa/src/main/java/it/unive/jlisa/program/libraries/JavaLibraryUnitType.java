package it.unive.jlisa.program.libraries;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;

public class JavaLibraryUnitType extends JavaClassType {

	private final String libraryName;

	private Integer hash = null;

	public JavaLibraryUnitType(
			CodeUnit library,
			CompilationUnit unit) {
		super(unit.getName(), unit);
		this.libraryName = library.getName();

		types.put(unit.getName(), this);
	}

	public String getLibraryName() {
		return libraryName;
	}

	@Override
	public int hashCode() {
		if (hash != null)
			return hash;
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((libraryName == null) ? 0 : libraryName.hashCode());
		hash = result;
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		JavaLibraryUnitType other = (JavaLibraryUnitType) obj;
		if (libraryName == null) {
			if (other.libraryName != null)
				return false;
		} else if (!libraryName.equals(other.libraryName))
			return false;
		return true;
	}

	public static boolean is(
			Type t,
			String lib,
			boolean includeReferences) {
		if (!LibrarySpecificationProvider.isLibraryLoaded(lib))
			return false;
		if (t instanceof JavaLibraryUnitType)
			return ((JavaLibraryUnitType) t).getLibraryName().equals(lib);
		else if (includeReferences && t instanceof ReferenceType)
			return is(((ReferenceType) t).getInnerType(), lib, true);
		else
			return false;
	}
}

package it.unive.jlisa.frontend.util;

import it.unive.lisa.util.frontend.LocalVariableTracker.LocalVariable;
import java.util.Objects;

public class VariableInfo {
	String name;
	LocalVariable localVariable;

	public VariableInfo(
			String name,
			LocalVariable localVariable) {
		this.name = name;
		this.localVariable = localVariable;
	}

	public String getName() {
		return name;
	}

	public LocalVariable getLocalVariable() {
		return localVariable;
	}

	@Override
	public int hashCode() {
		return Objects.hash(localVariable, name);
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
		VariableInfo other = (VariableInfo) obj;
		return Objects.equals(localVariable, other.localVariable) && Objects.equals(name, other.name);
	}
}

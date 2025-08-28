package it.unive.jlisa.program.libraries.loader;

import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.jlisa.program.libraries.LibrarySpecificationParser.LibraryCreationException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Method {

	private final boolean instance;
	private final boolean sealed;
	private final String name;
	private final String implementation;
	private final Type type;
	private final List<Parameter> params = new LinkedList<>();

	public Method(
			boolean instance,
			boolean sealed,
			String name,
			String implementation,
			Type type) {
		this.instance = instance;
		this.sealed = sealed;
		this.name = name;
		this.implementation = implementation;
		this.type = type;
	}

	public boolean isInstance() {
		return instance;
	}

	public boolean isSealed() {
		return sealed;
	}

	public String getName() {
		return name;
	}

	public String getImplementation() {
		return implementation;
	}

	public Type getType() {
		return type;
	}

	public Collection<Parameter> getParams() {
		return params;
	}

	@Override
	public int hashCode() {
		return Objects.hash(implementation, instance, name, params, sealed, type);
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
		Method other = (Method) obj;
		return Objects.equals(implementation, other.implementation) && instance == other.instance
				&& Objects.equals(name, other.name) && Objects.equals(params, other.params) && sealed == other.sealed
				&& Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "Method [instance=" + instance + ", sealed=" + sealed + ", name=" + name + ", implementation="
				+ implementation + ", type=" + type + ", params=" + params + "]";
	}

	@SuppressWarnings("unchecked")
	public NativeCFG toLiSACfg(
			CodeLocation location,
			CFG init,
			Unit container) {
		it.unive.lisa.program.cfg.Parameter[] pars = new it.unive.lisa.program.cfg.Parameter[params.size()];
		for (int i = 0; i < pars.length; i++)
			pars[i] = this.params.get(i).toLiSAParameter(location, init);

		CodeMemberDescriptor desc = new CodeMemberDescriptor(
				location,
				container,
				this.instance,
				this.name,
				this.type.toLiSAType(),
				pars);

		desc.setOverridable(!this.sealed);

		try {
			return new NativeCFG(desc,
					(Class<? extends NaryExpression>) Class.forName(this.implementation));
		} catch (ClassNotFoundException e) {
			throw new LibraryCreationException(e);
		}
	}
}

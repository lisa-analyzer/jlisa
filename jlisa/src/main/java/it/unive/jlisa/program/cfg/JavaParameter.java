package it.unive.jlisa.program.cfg;

import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class JavaParameter extends Parameter {

	private boolean isVarargs = false;

	public JavaParameter(
			CodeLocation location,
			String name,
			Type staticType,
			Expression defaultValue,
			Annotations annotations,
			boolean isVarargs) {
		super(location, name, staticType, defaultValue, annotations);
		this.isVarargs = isVarargs;
	}

	public JavaParameter(
			CodeLocation location,
			String name) {
		this(location, name, Untyped.INSTANCE, null, new Annotations());
	}

	public JavaParameter(
			CodeLocation location,
			String name,
			Type staticType) {
		super(location, name, staticType);
	}

	public JavaParameter(
			CodeLocation location,
			String name,
			Type staticType,
			Expression defaultValue,
			Annotations annotations) {
		super(location, name, staticType, defaultValue, annotations);
	}

	public boolean isVarargs() {
		return isVarargs;
	}

}

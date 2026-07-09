package it.unive.jlisa.program.libraries.loader.extensions;

import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Type;

public class CompileTimeGlobal extends Global {

	private final Constant defaultValue;

	public CompileTimeGlobal(
			CodeLocation location,
			Unit container,
			String name,
			boolean isInstance,
			Type staticType,
			Constant defaultValue) {
		super(location, container, name, isInstance, staticType);
		this.defaultValue = defaultValue;
	}

	public Constant getDefaultValue() {
		return defaultValue;
	}
}

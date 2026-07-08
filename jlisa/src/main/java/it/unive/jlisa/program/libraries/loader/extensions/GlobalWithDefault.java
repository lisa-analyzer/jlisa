package it.unive.jlisa.program.libraries.loader.extensions;

import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;

public class GlobalWithDefault extends Global {

	private final Expression defaultValue;

	public GlobalWithDefault(
			CodeLocation location,
			Unit container,
			String name,
			boolean isInstance,
			Type staticType,
			Expression defaultValue) {
		super(location, container, name, isInstance, staticType);
		this.defaultValue = defaultValue;
	}

	public Expression getDefaultValue() {
		return defaultValue;
	}
}

package it.unive.jlisa.frontend.annotations;

import java.util.Collections;
import java.util.Map;

public class AnnotationInfo {

	private final String name;
	private final Map<String, String> params;

	// This is the ONLY constructor we want
	public AnnotationInfo(
			String name,
			Map<String, String> params) {
		this.name = name;
		// Make params non-null and unmodifiable
		this.params = params == null
				? Collections.emptyMap()
				: Collections.unmodifiableMap(params);
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getParams() {
		return params;
	}

	@Override
	public String toString() {
		// Text shown in the CFG / debug output
		return "@" + name + params;
	}
}

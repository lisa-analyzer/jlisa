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

	public String toSpringLikeString() {
		if (params == null || params.isEmpty())
			return "@" + name;

		String v = null;
		if (params.containsKey("value"))
			v = params.get("value");
		else if (params.containsKey("path"))
			v = params.get("path");

		if (v != null) {
			String vv = v.trim();
			if (!((vv.startsWith("\"") && vv.endsWith("\"")) || (vv.startsWith("'") && vv.endsWith("'"))))
				vv = "\"" + vv + "\"";
			if (vv.startsWith("'") && vv.endsWith("'"))
				vv = "\"" + vv.substring(1, vv.length() - 1) + "\"";
			return "@" + name + "(" + vv + ")";
		}

		// fallback: key="value"
		StringBuilder sb = new StringBuilder();
		sb.append("@").append(name).append("(");
		boolean first = true;
		for (var e : params.entrySet()) {
			if (!first)
				sb.append(", ");
			first = false;
			sb.append(e.getKey()).append("=\"").append(e.getValue()).append("\"");
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public String toString() {
		// Text shown in the CFG / debug output
		return toSpringLikeString();
	}
}

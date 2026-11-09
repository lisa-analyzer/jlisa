package it.unive.jlisa.springed.webmvc.p1.serialization;

import com.fasterxml.jackson.annotation.JsonValue;
import it.unive.jlisa.springed.webmvc.p1.Registry;
import java.util.LinkedHashMap;
import java.util.Map;

public class P1webmvcOutput {

	private final Map<String, Registry> output = new LinkedHashMap<>();

	public void addRegistry(
			String projectName,
			Registry registry) {
		output.put(projectName, registry);
	}

	@JsonValue
	public Map<String, Registry> get() {
		return output;
	}
}

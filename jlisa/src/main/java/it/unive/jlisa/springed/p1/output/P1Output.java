package it.unive.jlisa.springed.p1.output;

import com.fasterxml.jackson.annotation.JsonValue;
import it.unive.jlisa.springed.p1.constructs.Registry;
import java.util.LinkedHashMap;
import java.util.Map;

public class P1Output {

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

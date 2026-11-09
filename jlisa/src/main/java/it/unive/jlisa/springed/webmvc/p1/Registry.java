package it.unive.jlisa.springed.webmvc.p1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.unive.jlisa.springed.webmvc.p1.serialization.RegistryJsonSerializer;
import it.unive.lisa.program.cfg.CodeMember;
import java.util.ArrayList;
import java.util.List;

@JsonSerialize(using = RegistryJsonSerializer.class)
public class Registry {

	private final List<RegistryRecord> mappings = new ArrayList<>();

	public Registry() {
	}

	public List<RegistryRecord> getMappings() {
		return this.mappings;
	}

	public void insert(
			RegistryRecord mapping) {
		this.mappings.add(mapping);
	}

	public CodeMember getMethod(
			RegistryRecord mapping) {
		return mapping.getMethod();
	}
}

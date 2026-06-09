package it.unive.jlisa.springed.p1.constructs;

import it.unive.lisa.program.cfg.CodeMember;
import java.util.ArrayList;
import java.util.List;

public class Registry {

	private final List<Mapping> mappings = new ArrayList<>();

	public Registry() {
	}

	public List<Mapping> getMappings() {
		return mappings;
	}

	public void insert(
			Mapping mapping) {
		mappings.add(mapping);
	}

	public CodeMember getMethod(
			Mapping mapping) {
		return mapping.getMethod();
	}
}

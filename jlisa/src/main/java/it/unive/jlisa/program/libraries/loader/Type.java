package it.unive.jlisa.program.libraries.loader;

import it.unive.lisa.program.Program;

public interface Type {

	it.unive.lisa.type.Type toLiSAType(Program program);
}

package it.unive.jlisa.frontend;

import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CodeLocation;

public class EnumUnit extends ClassUnit {

	public EnumUnit(CodeLocation location, Program program, String name, boolean sealed) {
		super(location, program, name, sealed);
	}
}

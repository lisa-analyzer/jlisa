package it.unive.jlisa.program;

import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.type.UnitType;

public class ReflectionCache {

	static public Global lastField;
	static public CodeMember lastMethod;
	static public UnitType lastClass;
}

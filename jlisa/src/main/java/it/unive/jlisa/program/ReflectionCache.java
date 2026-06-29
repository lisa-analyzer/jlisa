package it.unive.jlisa.program;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CodeMember;

public class ReflectionCache {

	static public Global lastField;
	static public CodeMember lastMethod;
	static public JavaClassType lastClass;
}

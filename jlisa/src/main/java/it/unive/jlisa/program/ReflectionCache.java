package it.unive.jlisa.program;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CodeMember;

public class ReflectionCache {

	static private Global lastField;
	static private CodeMember lastMethod;
	static private JavaClassType lastClass;

	public static Global getLastField() {
		return lastField;
	}

	public static CodeMember getLastMethod() {
		return lastMethod;
	}

	public static JavaClassType getLastClass() {
		return lastClass;
	}

	public static void setLastField(
			Global x) {
		lastField = x;
	}

	public static void setLastMethod(
			CodeMember x) {
		lastMethod = x;
	}

	public static void setLastClass(
			JavaClassType x) {
		lastClass = x;
	}

}

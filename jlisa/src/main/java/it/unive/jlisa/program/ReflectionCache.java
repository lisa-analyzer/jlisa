package it.unive.jlisa.program;

import java.util.HashMap;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.UnitType;

public class ReflectionCache {

	public static Global lastField;
	public static CodeMember lastMethod;
	public static UnitType lastClass;

	public static final HashMap<UnitType, SymbolicExpression> loadedClasses = new HashMap<>();

	public static boolean isLastClassLoaded() {
		return loadedClasses.containsKey(lastClass);
	}

	public static void cacheLastClass(SymbolicExpression expr) {
		loadedClasses.put(lastClass, expr);
	}

	public static SymbolicExpression getCachedLastClass() {
		return loadedClasses.get(lastClass);
	}

	public static void loadLastClass(String clazzName) {
		boolean classLookup = true;
		boolean interfaceLookup = true;

		JavaClassType foundClass = null;
		JavaInterfaceType foundInterface = null;
		// TODO AP: this can even be a primitive type!

		try {
			foundClass = JavaClassType.lookup(clazzName);
		} catch (IllegalArgumentException e) {
			classLookup = false;
		}
		try {
			foundInterface = JavaInterfaceType.lookup(clazzName);
		} catch (IllegalArgumentException e) {
			interfaceLookup = false;
		}

		if (classLookup || interfaceLookup) {
			assert(foundClass != null || foundInterface != null);
			ReflectionCache.lastClass = (classLookup) ? foundClass : foundInterface;
		}
	}
}


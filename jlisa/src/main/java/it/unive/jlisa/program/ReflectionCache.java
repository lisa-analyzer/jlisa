package it.unive.jlisa.program;

import java.util.HashMap;
import java.util.HashSet;

import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

public class ReflectionCache {

	public static Global lastField;
	public static CodeMember lastMethod;
	public static Type lastClass;

	private static final HashMap<Type, SymbolicExpression> loadedClasses = new HashMap<>();
	private static final HashSet<Type> initializedClasses = new HashSet<>();

	public static boolean isLastClassLoaded() {
		return loadedClasses.containsKey(lastClass);
	}

	public static boolean isClassLoaded(Type t) {
		return loadedClasses.containsKey(t);
	}

	public static void cacheLastClass(SymbolicExpression expr) {
		loadedClasses.put(lastClass, expr);
	}

	public static void cacheLoadedClass(Type t, SymbolicExpression expr) {
		loadedClasses.put(t, expr);
	}

	public static SymbolicExpression getCachedClass(Type t) {
		return loadedClasses.get(t);
	}

	public static void addInitializedClass(Type clazz) {
		boolean added = initializedClasses.add(clazz);
		assert(added);
	}

	public static boolean isClassInitialized(Type clazz) {
		return initializedClasses.contains(clazz);
	}
}


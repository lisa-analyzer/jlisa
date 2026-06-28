package it.unive.jlisa.program;

import java.util.HashMap;
import java.util.HashSet;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.type.JavaTypeSystem;
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

	public static void cacheLastClass(SymbolicExpression expr) {
		loadedClasses.put(lastClass, expr);
	}

	public static SymbolicExpression getCachedLastClass() {
		return loadedClasses.get(lastClass);
	}

	public static void loadLastClass(String clazzName) {
		boolean classLookup = false;
		boolean interfaceLookup = false;
		boolean primitiveLookup = false;

		JavaClassType foundClass = null;
		JavaInterfaceType foundInterface = null;
		Type foundPrimitive = null;

		try {
			foundClass = JavaClassType.lookup(clazzName);
			classLookup = true;
		} catch (IllegalArgumentException e) { }
		try {
			foundInterface = JavaInterfaceType.lookup(clazzName);
			interfaceLookup = true;
		} catch (IllegalArgumentException e) { }

		if (!classLookup && !interfaceLookup) {
			for (int i = 0; i < JavaTypeSystem.PRIMITIVE_TYPES.length; ++i) {
				Type primitiveType = JavaTypeSystem.PRIMITIVE_TYPES[i];
				if (clazzName.equals(primitiveType.toString())) {
					primitiveLookup = true;
					// assign the found primitive Type
					foundPrimitive = primitiveType;
					break;
				}

			}
		}

		if (classLookup || interfaceLookup || primitiveLookup) {
			assert(foundClass != null || foundInterface != null || foundPrimitive != null);
			ReflectionCache.lastClass = (classLookup) ? foundClass :
				((interfaceLookup) ? foundInterface : foundPrimitive);

			// ReflectionCache.lastClass = (classLookup) ? foundClass : foundInterface;
		}
	}

	public static void addInitializedClass(Type clazz) {
		boolean added = initializedClasses.add(clazz);
		assert(added);
	}

	public static boolean isClassInitialized(Type clazz) {
		return initializedClasses.contains(clazz);
	}
}


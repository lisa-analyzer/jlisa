package it.unive.jlisa.types;
import java.util.*;
import java.util.stream.Collectors;

import it.unive.jlisa.program.JavaProgram;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.unit.ObjectClassUnit;
import it.unive.jlisa.program.unit.StringClassUnit;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.type.InMemoryType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;

public final class JavaClassType implements InMemoryType, UnitType {
    private static Map<String, JavaClassType> types = new HashMap<>();
    private static ObjectClassUnit objectClassUnit = null;

    /**
     * Clears the cache of {@link JavaClassType}s created up to now.
     */
    public static void clearAll() {
        types.clear();
    }

    public static void init(JavaProgram program) {
        types = new HashMap<>();
        objectClassUnit = new ObjectClassUnit(program);
        new StringClassUnit(program);
    }

    /**
     * Yields all the {@link JavaClassType}s defined up to now.
     *
     * @return the collection of all the class types
     */
    public static Collection<JavaClassType> all() {
        return types.values();
    }

    public static List<ClassUnit> getClassUnits() {
        return types.values().stream()
            .map(JavaClassType::getUnit)
            .collect(Collectors.toList());
    }

    public static ObjectClassUnit getObjectClassUnit() {
        return objectClassUnit;
    }

    public static JavaClassType put(JavaClassType classType) {
        return types.put(classType.name, classType);
    }

    public static void registerClassType(JavaClassType classType) {
        types.put(classType.name, classType);
    }
    /**
     * Yields a unique instance (either an existing one or a fresh one) of
     * {@link JavaClassType} representing a class with the given {@code name},
     * representing the given {@code unit}.
     *
     * @param name the name of the class
     * @param unit the unit underlying this type
     *
     * @return the unique instance of {@link JavaClassType} representing the class
     *             with the given name
     */
    public static JavaClassType lookup(
            String name,
            ClassUnit unit) {
                if (!types.containsKey(name)) {
                    return new JavaClassType(name, unit);
                } 
        return types.get(name);
    }

    public static Optional<JavaClassType> lookup(String name) {
        return Optional.ofNullable(types.get(name));
    }

    private final String name;

    private final ClassUnit unit;

    private JavaClassType(
            String name,
            ClassUnit unit) {
        Objects.requireNonNull(name, "The name of a class type cannot be null");
        Objects.requireNonNull(unit, "The unit of a class type cannot be null");
        this.name = name;
        this.unit = unit;
        types.put(name, this);
    }

    @Override
    public ClassUnit getUnit() {
        return unit;
    }

    @Override
    public final boolean canBeAssignedTo(
            Type other) {
        if (other instanceof JavaClassType)
            return subclass((JavaClassType) other);

        if (other instanceof JavaInterfaceType)
            return subclass((JavaInterfaceType) other);

        return false;
    }

    private boolean subclass(
            JavaClassType other) {
        return this == other || unit.isInstanceOf(other.unit);
    }

    private boolean subclass(
            JavaInterfaceType other) {
        return unit.isInstanceOf(other.getUnit());
    }

    @Override
    public Type commonSupertype(
            Type other) {
        if (other.isNullType())
            return this;

        if (!other.isUnitType())
            return Untyped.INSTANCE;

        if (canBeAssignedTo(other))
            return other;

        if (other.canBeAssignedTo(this))
            return this;

        return scanForSupertypeOf((UnitType) other);
    }

    private Type scanForSupertypeOf(
            UnitType other) {
        WorkingSet<JavaClassType> ws = FIFOWorkingSet.mk();
        Set<JavaClassType> seen = new HashSet<>();
        ws.push(this);
        JavaClassType current;
        while (!ws.isEmpty()) {
            current = ws.pop();
            if (!seen.add(current))
                continue;

            if (other.canBeAssignedTo(current))
                return current;

            // null since we do not want to create new types here
            current.unit.getImmediateAncestors().forEach(u -> ws.push(lookup(u.getName(), null)));
        }

        return Untyped.INSTANCE;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((unit == null) ? 0 : unit.hashCode());
        return result;
    }

    @Override
    public boolean equals(
            Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JavaClassType other = (JavaClassType) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (unit == null) {
            if (other.unit != null)
                return false;
        } else if (!unit.equals(other.unit))
            return false;
        return true;
    }

    @Override
    public Set<Type> allInstances(
            TypeSystem types) {
        Set<Type> instances = new HashSet<>();
        for (Unit in : unit.getInstances())
            instances.add(lookup(in.getName(), null));
        return instances;
    }
}

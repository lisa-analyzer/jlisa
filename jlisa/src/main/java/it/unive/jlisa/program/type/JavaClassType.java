package it.unive.jlisa.program.type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import it.unive.jlisa.program.cfg.statement.literal.JavaNullLiteral;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;

public class JavaClassType implements UnitType {

    protected static final Map<String, JavaClassType> types = new HashMap<>();

    /**
     * Clears the cache of {@link JavaClassType}s created up to now.
     */
    public static void clearAll() {
        types.clear();
    }

    /**
     * Yields all the {@link JavaClassType}s defined up to now.
     *
     * @return the collection of all the class types
     */
    public static Collection<JavaClassType> all() {
        return types.values();
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
            CompilationUnit unit) {
        return types.computeIfAbsent(name, x -> new JavaClassType(name, unit));
    }
    
    public static boolean hasType(String name) {
    	return types.containsKey(name);
    }

    protected final String name;

    protected final CompilationUnit unit;

    protected JavaClassType(
            String name,
            CompilationUnit unit) {
        Objects.requireNonNull(name, "The name of a class type cannot be null");
        Objects.requireNonNull(unit, "The unit of a class type cannot be null");
        this.name = name;
        this.unit = unit;
    }

    @Override
    public CompilationUnit getUnit() {
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

	public Type getReference() {
		return new JavaReferenceType(this);
	}
	
	@Override
	public Expression defaultValue(CFG cfg, CodeLocation location) {
		return new JavaNullLiteral(cfg, location);
	}
	
	public static JavaClassType getClassCastExceptionType() {
		return lookup("ClassCastException", null);
	}
	
	public static JavaClassType getNullPoiterExceptionType() {
		return lookup("NullPointerException", null);
	}
	
	public static JavaClassType getNegativeArraySizeExceptionType() {
		return lookup("NegativeArraySizeException", null);
	}
}

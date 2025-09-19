package it.unive.jlisa.program.type;

import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class JavaInterfaceType implements UnitType {

	private static final Map<String, JavaInterfaceType> types = new HashMap<>();

	/**
	 * Clears the cache of {@link JavaInterfaceType}s created up to now.
	 */
	public static void clearAll() {
		types.clear();
	}

	/**
	 * Yields all the {@link JavaInterfaceType}s defined up to now.
	 *
	 * @return the collection of all the interface types
	 */
	public static Collection<JavaInterfaceType> all() {
		return types.values();
	}

	/**
	 * Yields a unique instance of {@link JavaInterfaceType} representing a
	 * class with the given {@code name}, representing the given {@code unit}.
	 * If no unit with the given name exits, the given unit is returned after
	 * updating the internal map. If a unit with the given name has already been
	 * registered, an {@link IllegalArgumentException} is thrown.
	 *
	 * @param name the name of the interface
	 * @param name the name of the class
	 * @param unit the unit underlying this type
	 *
	 * @return the unique instance of {@link JavaInterfaceType} representing the
	 *             interface with the given name
	 * 
	 * @throws IllegalArgumentException if an interface with the given name has
	 *                                      already been registered
	 */
	public static JavaInterfaceType register(
			String name,
			InterfaceUnit unit) {
		if (types.containsKey(name))
			throw new IllegalArgumentException("An interface type " + name + " has already been registered");
		return types.computeIfAbsent(name, x -> new JavaInterfaceType(name, unit));
	}

	/**
	 * Yields a unique instance of {@link JavaInterfaceType} representing a
	 * class with the given {@code name}. If no class with the given name has
	 * been registered yet, an {@link IllegalStateException} is thrown.
	 *
	 * @param name the name of the class
	 *
	 * @return the unique instance of {@link JavaClassType} representing the
	 *             class with the given name
	 * 
	 * @throws IllegalArgumentException if no class with the given name has been
	 *                                     registered yet
	 */
	public static JavaInterfaceType lookup(
			String name) {
		JavaInterfaceType type = types.get(name);
		if (type == null)
			throw new IllegalArgumentException("No interface type " + name + " has been registered");
		return type;
	}

	private final String name;

	private final InterfaceUnit unit;

	private JavaInterfaceType(
			String name,
			InterfaceUnit unit) {
		Objects.requireNonNull(name, "The name of an interface type cannot be null");
		Objects.requireNonNull(unit, "The unit of a interface type cannot be null");
		this.name = name;
		this.unit = unit;
	}

	@Override
	public InterfaceUnit getUnit() {
		return unit;
	}

	@Override
	public final boolean canBeAssignedTo(
			Type other) {
		if (other instanceof JavaInterfaceType)
			return subclass((JavaInterfaceType) other);

		return false;
	}

	private boolean subclass(
			JavaInterfaceType other) {
		return this == other || unit.isInstanceOf(other.unit);
	}

	@Override
	public Type commonSupertype(
			Type other) {
		if (other.isNullType())
			return this;

		if (!other.isUnitType())
			return Untyped.INSTANCE;

		if (other.canBeAssignedTo(this))
			return this;

		return scanForSupertypeOf((UnitType) other);
	}

	private Type scanForSupertypeOf(
			UnitType other) {
		WorkingSet<JavaInterfaceType> ws = FIFOWorkingSet.mk();
		Set<JavaInterfaceType> seen = new HashSet<>();
		ws.push(this);
		JavaInterfaceType current;
		while (!ws.isEmpty()) {
			current = ws.pop();
			if (!seen.add(current))
				continue;

			if (other.canBeAssignedTo(current))
				return current;

			// null since we do not want to create new types here
			current.unit.getImmediateAncestors().forEach(u -> ws.push(lookup(u.getName())));
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
		JavaInterfaceType other = (JavaInterfaceType) obj;
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
		for (Unit un : unit.getInstances())
			if (un instanceof InterfaceUnit)
				instances.add(lookup(un.getName()));
			else
				instances.add(JavaClassType.lookup(un.getName()));
		return instances;
	}

	@Override
	public Expression defaultValue(
			CFG cfg,
			CodeLocation location) {
		return new NullLiteral(cfg, location);
	}

	public JavaReferenceType getReference() {
		return new JavaReferenceType(this);
	}
}

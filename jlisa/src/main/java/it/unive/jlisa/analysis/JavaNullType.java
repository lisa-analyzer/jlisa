package it.unive.jlisa.analysis;

import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class JavaNullType extends NullType {
	
	/**
	 * Unique instance of {@link NullType}.
	 */
	public static final JavaNullType INSTANCE = new JavaNullType();

	/**
	 * Builds the type. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaNullType() {
	}
	
	
	@Override
	public boolean canBeAssignedTo(
			Type other) {
		return other.isInMemoryType();
	}

	@Override
	public Type commonSupertype(
			Type other) {
		return other != null && other.isInMemoryType() ? other : Untyped.INSTANCE;
	}
}

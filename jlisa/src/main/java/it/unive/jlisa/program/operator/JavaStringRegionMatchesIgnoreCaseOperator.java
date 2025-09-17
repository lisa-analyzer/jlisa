package it.unive.jlisa.program.operator;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

public class JavaStringRegionMatchesIgnoreCaseOperator implements NaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStringRegionMatchesIgnoreCaseOperator INSTANCE = new JavaStringRegionMatchesIgnoreCaseOperator();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected JavaStringRegionMatchesIgnoreCaseOperator() {
	}

	@Override
	public String toString() {
		return "str-region-matches";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type>[] operands) {
		if (operands.length != 6)
			throw new IllegalArgumentException("Incorrect number of operands!!");

		if (operands[0].stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		if (operands[1].stream().noneMatch(t -> t.equals(types.getBooleanType())))
			return Collections.emptySet();
		if (operands[2].stream().noneMatch(t -> t.equals(types.getIntegerType())))
			return Collections.emptySet();
		if (operands[3].stream().noneMatch(t -> t.equals(types.getStringType())))
			return Collections.emptySet();
		if (operands[4].stream().noneMatch(t -> t.equals(types.getIntegerType())))
			return Collections.emptySet();
		if (operands[5].stream().noneMatch(t -> t.equals(types.getIntegerType())))
			return Collections.emptySet();

		return Collections.singleton(types.getBooleanType());
	}

}

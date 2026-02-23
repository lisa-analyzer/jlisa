package it.unive.jlisa.type;

import it.unive.jlisa.program.type.*;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.type.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class JavaTypeSystem extends TypeSystem {

	@Override
	public BooleanType getBooleanType() {
		return JavaBooleanType.INSTANCE;
	}

	@Override
	public StringType getStringType() {
		return (StringType) JavaClassType.getStringType();
	}

	@Override
	public NumericType getIntegerType() {
		return JavaIntType.INSTANCE;
	}

	@Override
	public CharacterType getCharacterType() {
		return JavaCharType.INSTANCE;
	}

	@Override
	public ReferenceType getReference(
			Type type) {
		return new JavaReferenceType(type);
	}

	@Override
	public int distanceBetweenTypes(
			Type first,
			Type second) {
		if (first instanceof Untyped)
			return 0;
		if (second instanceof Untyped)
			return 0;
		if (second instanceof JavaNumericType numericParam)
			if (first instanceof JavaNumericType numericFormal) {
				int paramDist = numericParam.distance(numericFormal);
				if (paramDist < 0)
					return -1; // incomparable
				return paramDist;
			} else
				return -1;
		else if (second.isBooleanType() && first.isBooleanType())
			return 0;
		else if (JavaClassType.isWrapperOf(first, second))
			// boxing
			return 10;
		else if (JavaClassType.isWrapperOf(second, first))
			// unboxing
			return 10;
		else if (second instanceof ReferenceType refTypeParam
				&& first instanceof ReferenceType refTypeFormal) {
			if (refTypeParam.getInnerType().isNullType())
				return 0;
			else if (refTypeParam.getInnerType() instanceof JavaArrayType actualInner
					&& refTypeFormal.getInnerType() instanceof JavaArrayType formalInner)
				return actualInner.equals(formalInner) ? 0 : -1;

			// from here on, we should suppose that the inner types are
			// units
			UnitType paramUnitType = refTypeParam.getInnerType().asUnitType();
			UnitType formalUnitType = refTypeFormal.getInnerType().asUnitType();
			if (paramUnitType != null && formalUnitType != null) {
				int paramDist = distanceBetweenCompilationUnits(formalUnitType.getUnit(), paramUnitType.getUnit());
				if (paramDist < 0)
					return -1;
				return paramDist;
			} else
				return -1;
		} else
			return -1;
	}

	/**
	 * Computes the inheritance distance between two compilation units.
	 * <p>
	 * The distance is the number of steps from {@code first} up its hierarchy
	 * until {@code second} is reached. If {@code second} is not reachable,
	 * returns -1.
	 * </p>
	 *
	 * @param first  the starting compilation unit
	 * @param second the target compilation unit
	 *
	 * @return the distance, or {@code -1} if no relationship exists
	 */
	private int distanceBetweenCompilationUnits(
			CompilationUnit first,
			CompilationUnit second) {
		if (first.equals(second))
			return 0;
		for (CompilationUnit ancestor : second.getImmediateAncestors()) {
			int dist = distanceBetweenCompilationUnits(first, ancestor);
			if (dist < 0)
				continue;
			return 1 + dist;
		}
		return -1;
	}

	@Override
	public boolean canBeReferenced(
			Type type) {
		return type.isInMemoryType();
	}

	/**
	 * Simulates a conversion operation, where an expression with possible
	 * runtime types {@code types} is being converted to one of the possible
	 * type tokens in {@code tokens}. All types in {@code tokens} that are not
	 * {@link TypeTokenType}s, according to {@link Type#isTypeTokenType()}, will
	 * be ignored. The returned set contains a subset of the types in
	 * {@code tokens}, keeping only the ones such that there exists at least one
	 * type in {@code types} that can be assigned to it.
	 *
	 * @param types  the types of the expression being converted
	 * @param tokens the tokens representing the operand of the type conversion
	 *
	 * @return the set of possible types after the type conversion
	 */
	public Set<Type> convert(
			Set<Type> types,
			Set<Type> tokens) {
		Set<Type> result = new HashSet<>();
		Set<Type> filtered = tokens.stream()
				.filter(Type::isTypeTokenType)
				.flatMap(t -> t.asTypeTokenType().getTypes().stream())
				.collect(Collectors.toSet());

		for (Type token : filtered)
			// note assuming compiling code and no reflection, all
			// conversions can be done.
			//
			// if (t.canBeAssignedTo(token))
			result.add(token);

		return result;
	}
}

package it.unive.jlisa.type;

import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.TypeTokenType;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
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
	public ReferenceType getReference(
			Type type) {
		return new JavaReferenceType(type);
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

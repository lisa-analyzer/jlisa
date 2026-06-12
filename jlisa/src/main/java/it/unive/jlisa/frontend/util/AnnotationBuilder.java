package it.unive.jlisa.frontend.util;

import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.values.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.*;

public final class AnnotationBuilder {

	public static Annotations fromDeclarationModifiers(
			List<?> modifiers) {
		Annotations anns = new Annotations();

		for (Object modifier : modifiers) {
			if (modifier instanceof org.eclipse.jdt.core.dom.Annotation jdtAnn) {
				anns.addAnnotation(fromJdt(jdtAnn));
			}
		}

		return anns;
	}

	// TODO: support @Inherited annotations.
	public static Annotation fromJdt(
			org.eclipse.jdt.core.dom.Annotation jdtAnn) {
		String annName = jdtAnn.getTypeName().getFullyQualifiedName();

		return switch (jdtAnn) {
		case MarkerAnnotation markerAnn -> new Annotation(annName);

		case SingleMemberAnnotation singleMemberAnn -> {
			// "value" is the JLS-defined member name for this type of
			// annotation: @Foo(22) means @Foo(value = 22).
			String memberName = "value";
			AnnotationMember member = new AnnotationMember(memberName,
					parseAnnotationValue(singleMemberAnn.getValue()));
			yield new Annotation(annName, List.of(member));
		}

		case NormalAnnotation normalAnn -> {
			List<AnnotationMember> members = new ArrayList<>();
			for (Object o : normalAnn.values()) {
				MemberValuePair jdtMember = (MemberValuePair) o;
				members.add(new AnnotationMember(
						jdtMember.getName().getIdentifier(),
						parseAnnotationValue(jdtMember.getValue())));
			}
			yield new Annotation(annName, members);
		}

		default -> throw new IllegalStateException(
				"Unexpected JDT annotation kind: " + jdtAnn.getClass().getName() + " at " + annName);
		};
	}

	private static AnnotationValue parseAnnotationValue(
			Expression expr) {
		return switch (expr) {
		case StringLiteral sl -> new StringAnnotationValue(sl.getEscapedValue());
		case CharacterLiteral cl -> new CharAnnotationValue(cl.charValue());
		case BooleanLiteral bl -> new BoolAnnotationValue(bl.booleanValue());
		case NumberLiteral nl -> getNumericAnnotationValue(nl);
		case ArrayInitializer ai -> getArrayAnnotationValue(ai);
		default -> throw new RuntimeException("Unsupported annotation value type: " + expr.getClass());
		};
	}

	/*
	 * TODO: This was taken from ExpressionVisitor line 528-569. As the need to
	 * parse NumericLiteral to some more concrete type (i.e. Double, Int
	 * Literal), then a separate common shared utility for this would be useful
	 * to have.
	 */
	private static BasicAnnotationValue getNumericAnnotationValue(
			NumberLiteral literal) {
		String token = literal.getToken();
		if ((token.endsWith("f") || token.endsWith("F")) && !token.startsWith("0x")) {
			return new FloatAnnotationValue(Float.parseFloat(token));
		}
		if (token.contains(".")
				|| ((token.contains("e") || token.contains("E") || token.endsWith("d") || token.endsWith("D"))
						&& !token.startsWith("0x"))) {
			return new DoubleAnnotationValue(Double.parseDouble(token));
		}
		if (token.endsWith("l") || token.endsWith("L")) {
			// drop 'l' or 'L'
			String value = token.substring(0, token.length() - 1);
			long parsed = (value.startsWith("0x") || value.startsWith("0X"))
					? Long.parseUnsignedLong(value.substring(2), 16)
					: Long.decode(value);
			return new LongAnnotationValue(parsed);
		}
		try {
			long value = Long.decode(token); // handles 0x, 0b, octal, decimal
			return (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE)
					? new IntAnnotationValue((int) value)
					: new LongAnnotationValue(value);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Could not parse " + token + ": not a valid Number Literal", e);
		}
	}

	private static ArrayAnnotationValue getArrayAnnotationValue(
			ArrayInitializer arrayInitializer) {
		List<?> exprs = arrayInitializer.expressions();

		BasicAnnotationValue[] annValues = new BasicAnnotationValue[exprs.size()];
		for (int i = 0; i < exprs.size(); i++) {
			Expression expr = (Expression) exprs.get(i);
			AnnotationValue parsedAnnValue = parseAnnotationValue(expr);
			annValues[i] = (BasicAnnotationValue) parsedAnnValue;
		}

		return new ArrayAnnotationValue(annValues);
	}
}

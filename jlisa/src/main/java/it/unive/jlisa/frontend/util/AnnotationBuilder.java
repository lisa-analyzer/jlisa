package it.unive.jlisa.frontend.util;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.UnsupportedAnnotationException;
import it.unive.jlisa.frontend.visitors.expression.QualifiedNameVisitor;
import it.unive.jlisa.frontend.visitors.expression.TypeASTVisitor;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.values.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.*;

public final class AnnotationBuilder {

	private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(AnnotationBuilder.class);

	private static ParsingEnvironment env;
	private static UnitScope scope;

	public static Annotations fromDeclarationModifiers(
			List<?> modifiers,
			ParsingEnvironment env,
			UnitScope scope) {

		Annotations anns = new Annotations();
		for (Object modifier : modifiers) {
			if (modifier instanceof org.eclipse.jdt.core.dom.Annotation jdtAnn) {
				anns.addAnnotation(fromJdt(jdtAnn, env, scope));
			}
		}

		return anns;
	}

	// TODO: support @Inherited annotations.
	public static Annotation fromJdt(
			org.eclipse.jdt.core.dom.Annotation jdtAnn,
			ParsingEnvironment env,
			UnitScope scope) {
		AnnotationBuilder.env = env;
		AnnotationBuilder.scope = scope;

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
		case StringLiteral sl -> new StringAnnotationValue(sl.getLiteralValue());
		case CharacterLiteral cl -> new CharAnnotationValue(cl.charValue());
		case BooleanLiteral bl -> new BoolAnnotationValue(bl.booleanValue());
		case NumberLiteral nl -> getNumericAnnotationValue(nl);
		case TypeLiteral tl -> new CompilationUnitAnnotationValue(tl.getType().toString());
		case ArrayInitializer ai -> getArrayAnnotationValue(ai);
		case QualifiedName qn -> getQualifiedNameValue(qn);
		case ParenthesizedExpression pe -> parseAnnotationValue(pe.getExpression());
		case SimpleName sn -> resolveSimpleNameValue(sn);
		case InfixExpression ie -> resolveInfixExpression(ie);
		default -> throw new UnsupportedAnnotationException("Unsupported annotation value type: " + expr.getClass());
		};
	}

	private static AnnotationValue resolveInfixExpression(
			InfixExpression ie) {
		if (ie.getOperator() != InfixExpression.Operator.PLUS)
			throw new UnsupportedAnnotationException(
					"Only String concatenation is supported in annotation values: " + ie);

		List<Expression> operands = new ArrayList<>();
		operands.add(ie.getLeftOperand());
		operands.add(ie.getRightOperand());
		for (Object extended : ie.extendedOperands())
			operands.add((Expression) extended);

		AnnotationValue folded = null;
		for (Expression operand : operands) {
			AnnotationValue value = parseAnnotationValue(operand);
			if (folded == null)
				folded = value;
			else if (folded instanceof StringAnnotationValue || value instanceof StringAnnotationValue)
				folded = new StringAnnotationValue(folded.toString() + value);
			else
				throw new UnsupportedAnnotationException(
						"Only String concatenation is supported in annotation values: " + ie);
		}
		return folded;
	}

	private static AnnotationValue resolveSimpleNameValue(
			SimpleName sn) {
		String identifier = sn.getIdentifier();
		ASTNode node = sn.getParent();

		while (node != null) {
			// Fields declared in this class (then in each enclosing class)
			if (node instanceof TypeDeclaration type) {
				for (FieldDeclaration field : type.getFields()) {
					for (Object rawFragment : field.fragments()) {
						VariableDeclarationFragment fragment = (VariableDeclarationFragment) rawFragment;
						if (fragment.getName().getIdentifier().equals(identifier))
							return parseAnnotationValue(fragment.getInitializer());
					}
				}

				// Fields inherited from superclasses and interfaces
				Unit unit = env.parserContext().getProgram().getUnit(buildUnitName(type));
				if (unit instanceof it.unive.lisa.program.CompilationUnit lisaUnit) {
					Expression inherited = findInheritedFieldInitializer(lisaUnit, sn);
					if (inherited != null)
						return parseAnnotationValue(inherited);
				}
			}

			node = node.getParent();
		}

		return new StringAnnotationValue(sn.getIdentifier());
	}

	private static Expression findInheritedFieldInitializer(
			it.unive.lisa.program.CompilationUnit unit,
			SimpleName sn) {
		AST ast = sn.getAST();
		for (it.unive.lisa.program.CompilationUnit ancestor : unit.getImmediateAncestors()) {
			QualifiedName inheritedName = ast.newQualifiedName(ast.newName(ancestor.getName()),
					ast.newSimpleName(sn.getIdentifier()));
			Expression initializer = env.parserContext().evaluate(inheritedName, new QualifiedNameVisitor(env, scope));
			if (initializer == null)
				initializer = findInheritedFieldInitializer(ancestor, sn);
			if (initializer != null)
				return initializer;
		}
		return null;
	}

	private static String buildUnitName(
			AbstractTypeDeclaration type) {
		String outer = null;
		ASTNode parent = type.getParent();

		while (parent instanceof AbstractTypeDeclaration enclosing) {
			outer = outer == null ? enclosing.getName().getIdentifier()
					: enclosing.getName().getIdentifier() + "." + outer;
			parent = parent.getParent();
		}

		CompilationUnit cu = (CompilationUnit) type.getRoot();
		String pkg = cu.getPackage() == null ? "" : cu.getPackage().getName().getFullyQualifiedName();
		return FQNUtils.buildFQN(pkg, outer, type.getName().getIdentifier());
	}

	/*
	 * TODO: Class-level annotations are built while units are still being
	 * populated, so a constant reference to a type whose file has not been
	 * visited yet resolves to a null unit and falls back to the symbolic value
	 * below.
	 */
	private static AnnotationValue getQualifiedNameValue(
			QualifiedName qn) {

		Unit unit = TypeASTVisitor.getUnit(qn.getQualifier().getFullyQualifiedName(),
				env.parserContext().getProgram(), scope);

		if (unit instanceof EnumUnit enumUnit)
			return new EnumAnnotationValue(enumUnit.getName(), qn.getName().getIdentifier());

		Expression init = env.parserContext().evaluate(qn, new QualifiedNameVisitor(env, scope));

		if (init != null)
			return parseAnnotationValue(init);

		LOG.warn("Annotation-buildup: Failure to find/resolve value for following QualifiedName:{}. " +
				"Keeping it as a symbolic reference.", qn.getFullyQualifiedName());

		return new StringAnnotationValue(qn.getFullyQualifiedName());
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

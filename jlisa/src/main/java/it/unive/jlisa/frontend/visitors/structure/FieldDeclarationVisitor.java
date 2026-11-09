package it.unive.jlisa.frontend.visitors.structure;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.util.AnnotationBuilder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.TypeASTVisitor;
import it.unive.jlisa.frontend.visitors.scope.ClassScope;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.program.*;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Type;
import java.util.Set;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FieldDeclarationVisitor extends ScopedVisitor<ClassScope> {

	Set<String> visitedFieldNames;

	public FieldDeclarationVisitor(
			ParsingEnvironment environment,
			ClassScope scope,
			Set<String> visitedFieldNames) {
		super(environment, scope);
		this.visitedFieldNames = visitedFieldNames;
	}

	@Override
	public boolean visit(
			FieldDeclaration node) {
		int modifiers = node.getModifiers();
		TypeASTVisitor typeVisitor = new TypeASTVisitor(getEnvironment(), getScope().getUnitScope());
		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();
		if (type.isInMemoryType())
			type = new JavaReferenceType(type);

		Annotations annotations = AnnotationBuilder.fromDeclarationModifiers(node.modifiers(), getEnvironment(),
				getScope().getUnitScope());

		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String identifier = fragment.getName().getIdentifier();

			if (visitedFieldNames.contains(identifier))
				throw new ParsingException("variable-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
						"Global variable " + identifier + " already exists in the cfg", getSourceCodeLocation(node));
			else
				visitedFieldNames.add(identifier);

			type = typeVisitor.liftToArray(type, fragment);
			boolean isStatic = Modifier.isStatic(modifiers)
					|| (getScope().getLiSACompilationUnit() instanceof InterfaceUnit);
			// fields of interfaces are implicitly final
			boolean isFinal = Modifier.isFinal(modifiers)
					|| (getScope().getLiSACompilationUnit() instanceof InterfaceUnit);
			// static final fields of primitive types initialized with literals
			// are constants (JLS 4.12.4): their accesses yield the constant
			Constant constant = isStatic && isFinal
					? constantOf(type, fragment.getInitializer(), getSourceCodeLocation(fragment))
					: null;
			Global global = constant != null
					? new ConstantGlobal(getSourceCodeLocation(fragment), getScope().getLiSACompilationUnit(),
							identifier, constant, annotations)
					: new Global(getSourceCodeLocation(fragment), getScope().getLiSACompilationUnit(), identifier,
							!isStatic, type,
							annotations, isFinal);
			if (isStatic) {
				getScope().getLiSACompilationUnit().addGlobal(global);
			} else {
				getScope().getLiSACompilationUnit().addInstanceGlobal(global);
			}
		}

		return false;
	}

	/**
	 * Yields the value of a field of the given type initialized with the given
	 * expression, if the expression is a literal (possibly negated) of a
	 * primitive type that can initialize the field, as the constant the
	 * literals of the frontend yield for the type of the field (e.g.,
	 * {@code char}, {@code short} and {@code byte} values are integers).
	 * Yields {@code null} otherwise.
	 *
	 * @param type        the type of the field
	 * @param initializer the initializer (can be {@code null})
	 * @param location    the location of the field
	 *
	 * @return the constant, or {@code null}
	 */
	static Constant constantOf(
			Type type,
			org.eclipse.jdt.core.dom.Expression initializer,
			CodeLocation location) {
		org.eclipse.jdt.core.dom.Expression init = initializer;
		while (init instanceof ParenthesizedExpression)
			init = ((ParenthesizedExpression) init).getExpression();
		boolean negated = false;
		if (init instanceof PrefixExpression
				&& ((PrefixExpression) init).getOperator() == PrefixExpression.Operator.MINUS) {
			negated = true;
			init = ((PrefixExpression) init).getOperand();
		}
		Object value;
		if (init instanceof BooleanLiteral && !negated)
			return type instanceof JavaBooleanType
					? new Constant(type, ((BooleanLiteral) init).booleanValue(), location)
					: null;
		else if (init instanceof CharacterLiteral && !negated)
			value = (long) ((CharacterLiteral) init).charValue();
		else if (init instanceof NumberLiteral) {
			value = number(((NumberLiteral) init).getToken());
			if (value == null)
				return null;
		} else
			return null;

		if (value instanceof Double) {
			double d = negated ? -(Double) value : (Double) value;
			if (type instanceof JavaDoubleType)
				return new Constant(type, d, location);
			return type instanceof JavaFloatType && value instanceof Double && isFloat(initializer)
					? new Constant(type, (float) d, location)
					: null;
		}
		long l = negated ? -(Long) value : (Long) value;
		if (type instanceof JavaIntType)
			return new Constant(type, (int) l, location);
		if (type instanceof JavaLongType)
			return new Constant(type, l, location);
		if (type instanceof JavaShortType && l == (short) l)
			return new Constant(type, (int) l, location);
		if (type instanceof JavaByteType && l == (byte) l)
			return new Constant(type, (int) l, location);
		if (type instanceof JavaCharType && l == (char) l)
			return new Constant(type, (int) l, location);
		if (type instanceof JavaDoubleType)
			return new Constant(type, (double) l, location);
		if (type instanceof JavaFloatType)
			return new Constant(type, (float) l, location);
		return null;
	}

	/**
	 * Yields whether the given initializer is a literal of type {@code float}.
	 */
	private static boolean isFloat(
			org.eclipse.jdt.core.dom.Expression initializer) {
		String text = initializer.toString().trim();
		return text.endsWith("f") || text.endsWith("F");
	}

	/**
	 * Yields the value of the given number literal: a {@link Double} for
	 * floating point literals (also of type {@code float}), a {@link Long}
	 * (the bits of the literal, for hexadecimal, octal and binary ones) for
	 * integer literals, or {@code null} if it cannot be parsed.
	 */
	private static Object number(
			String token) {
		String t = token.replace("_", "");
		boolean hex = t.startsWith("0x") || t.startsWith("0X");
		try {
			if (!hex && (t.contains(".") || t.contains("e") || t.contains("E") || t.endsWith("f")
					|| t.endsWith("F") || t.endsWith("d") || t.endsWith("D")))
				return Double.parseDouble(t);
			if (hex && (t.contains(".") || t.contains("p") || t.contains("P")))
				return Double.parseDouble(t);
			if (t.endsWith("l") || t.endsWith("L"))
				t = t.substring(0, t.length() - 1);
			if (hex)
				return Long.parseUnsignedLong(t.substring(2), 16);
			if (t.startsWith("0b") || t.startsWith("0B"))
				return Long.parseUnsignedLong(t.substring(2), 2);
			if (t.length() > 1 && t.startsWith("0"))
				return Long.parseUnsignedLong(t.substring(1), 8);
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}

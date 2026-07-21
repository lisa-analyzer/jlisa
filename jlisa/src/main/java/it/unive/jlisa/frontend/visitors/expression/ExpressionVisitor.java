package it.unive.jlisa.frontend.visitors.expression;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.scope.ClassScope;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.BitwiseNot;
import it.unive.jlisa.program.cfg.expression.InstanceOf;
import it.unive.jlisa.program.cfg.expression.JavaAnd;
import it.unive.jlisa.program.cfg.expression.JavaArrayAccess;
import it.unive.jlisa.program.cfg.expression.JavaBitwiseAnd;
import it.unive.jlisa.program.cfg.expression.JavaBitwiseExclusiveOr;
import it.unive.jlisa.program.cfg.expression.JavaBitwiseOr;
import it.unive.jlisa.program.cfg.expression.JavaCastExpression;
import it.unive.jlisa.program.cfg.expression.JavaConditionalExpression;
import it.unive.jlisa.program.cfg.expression.JavaDivision;
import it.unive.jlisa.program.cfg.expression.JavaNewArray;
import it.unive.jlisa.program.cfg.expression.JavaNewArrayWithInitializer;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.expression.JavaOr;
import it.unive.jlisa.program.cfg.expression.JavaShiftLeft;
import it.unive.jlisa.program.cfg.expression.JavaShiftRight;
import it.unive.jlisa.program.cfg.expression.JavaUnsignedShiftRight;
import it.unive.jlisa.program.cfg.expression.PostfixAddition;
import it.unive.jlisa.program.cfg.expression.PostfixSubtraction;
import it.unive.jlisa.program.cfg.expression.PrefixAddition;
import it.unive.jlisa.program.cfg.expression.PrefixPlus;
import it.unive.jlisa.program.cfg.expression.PrefixSubtraction;
import it.unive.jlisa.program.cfg.statement.JavaAddition;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.JavaSubtraction;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessInstanceGlobal;
import it.unive.jlisa.program.cfg.statement.literal.CharLiteral;
import it.unive.jlisa.program.cfg.statement.literal.DoubleLiteral;
import it.unive.jlisa.program.cfg.statement.literal.FloatLiteral;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.jlisa.program.cfg.statement.literal.JavaNullLiteral;
import it.unive.jlisa.program.cfg.statement.literal.JavaStringLiteral;
import it.unive.jlisa.program.cfg.statement.literal.LongLiteral;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.*;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.comparison.GreaterOrEqual;
import it.unive.lisa.program.cfg.statement.comparison.GreaterThan;
import it.unive.lisa.program.cfg.statement.comparison.LessOrEqual;
import it.unive.lisa.program.cfg.statement.comparison.LessThan;
import it.unive.lisa.program.cfg.statement.comparison.NotEqual;
import it.unive.lisa.program.cfg.statement.literal.FalseLiteral;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.program.cfg.statement.logic.Not;
import it.unive.lisa.program.cfg.statement.numeric.Addition;
import it.unive.lisa.program.cfg.statement.numeric.Modulo;
import it.unive.lisa.program.cfg.statement.numeric.Multiplication;
import it.unive.lisa.program.cfg.statement.numeric.Negation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.function.TriFunction;
import org.eclipse.jdt.core.dom.*;

public class ExpressionVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<Expression> {
	@FunctionalInterface
	private interface BinaryFactory {
		Expression build(
				CFG cfg,
				SourceCodeLocation loc,
				Expression left,
				Expression right);
	}

	private static final Map<InfixExpression.Operator, BinaryFactory> INFIX_OPS = Map.ofEntries(
			Map.entry(InfixExpression.Operator.TIMES, Multiplication::new),
			Map.entry(InfixExpression.Operator.DIVIDE, JavaDivision::new),
			Map.entry(InfixExpression.Operator.REMAINDER, Modulo::new),
			Map.entry(InfixExpression.Operator.PLUS, JavaAddition::new),
			Map.entry(InfixExpression.Operator.MINUS, JavaSubtraction::new),
			Map.entry(InfixExpression.Operator.RIGHT_SHIFT_SIGNED, JavaShiftRight::new),
			Map.entry(InfixExpression.Operator.LEFT_SHIFT, JavaShiftLeft::new),
			Map.entry(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, JavaUnsignedShiftRight::new),
			Map.entry(InfixExpression.Operator.LESS, LessThan::new),
			Map.entry(InfixExpression.Operator.GREATER, GreaterThan::new),
			Map.entry(InfixExpression.Operator.LESS_EQUALS, LessOrEqual::new),
			Map.entry(InfixExpression.Operator.GREATER_EQUALS, GreaterOrEqual::new),
			Map.entry(InfixExpression.Operator.EQUALS, Equal::new),
			Map.entry(InfixExpression.Operator.NOT_EQUALS, NotEqual::new),
			Map.entry(InfixExpression.Operator.AND, JavaBitwiseAnd::new),
			Map.entry(InfixExpression.Operator.XOR, JavaBitwiseExclusiveOr::new),
			Map.entry(InfixExpression.Operator.OR, JavaBitwiseOr::new),
			Map.entry(InfixExpression.Operator.CONDITIONAL_AND, JavaAnd::new),
			Map.entry(InfixExpression.Operator.CONDITIONAL_OR, JavaOr::new));

	private Expression expression;

	public ExpressionVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			ArrayAccess node) {
		Expression left = getParserContext().evaluate(node.getArray(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		Expression right = getParserContext().evaluate(node.getIndex(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		expression = new JavaArrayAccess(getScope().getCFG(),
				getSourceCodeLocationManager(node.getArray(), true).getCurrentLocation(),
				left, right);
		return false;
	}

	@Override
	public boolean visit(
			ArrayInitializer node) {
		List<Expression> parameters = new ArrayList<>();

		Type contentType = Untyped.INSTANCE;
		for (Object args : node.expressions()) {
			ASTNode e = (ASTNode) args;
			Expression expr = getParserContext().evaluate(e,
					() -> new ExpressionVisitor(getEnvironment(), getScope()));
			parameters.add(expr);
			contentType = getArrayInitializerType(node);
			if (contentType == null) {
				contentType = expr.getStaticType();
			} else {
				contentType = contentType.asArrayType().getBaseType();
			}

		}

		expression = new JavaNewArrayWithInitializer(getScope().getCFG(),
				getSourceCodeLocation(node),
				parameters.toArray(new Expression[0]),
				new JavaReferenceType(JavaArrayType.lookup(contentType, 1)));
		return false;
	}

	@Override
	public boolean visit(
			ArrayCreation node) {
		Type type = getParserContext().evaluate(node.getType(),
				() -> new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope()));

		// currently we handle just single-dim and bi-dim arrays
		if (node.dimensions().size() > 2)
			throw new ParsingException("multi-dim array", ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Multi-dimensional arrays are not supported are not supported.",
					getSourceCodeLocation(node));

		// single-dimension arrays
		if (node.dimensions().size() == 1) {
			Expression length = getParserContext().evaluate((ASTNode) node.dimensions().get(0),
					() -> new ExpressionVisitor(getEnvironment(), getScope()));
			expression = new JavaNewArray(getScope().getCFG(), getSourceCodeLocation(node), length,
					new JavaReferenceType(type));
		}
		// bi-dimension arrays
		else if (node.dimensions().size() == 2) {
			int fstDim = Long.decode(((NumberLiteral) node.dimensions().get(0)).getToken()).intValue();
			Expression sndDimExpr = getParserContext().evaluate((ASTNode) node.dimensions().get(1),
					() -> new ExpressionVisitor(getEnvironment(), getScope()));
			List<Expression> parameters = new ArrayList<>();
			for (int i = 0; i < fstDim; i++) {
				Expression expr = new JavaNewArray(getScope().getCFG(),
						getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation(),
						sndDimExpr, (JavaReferenceType) type.asArrayType().getInnerType());
				parameters.add(expr);
			}

			expression = new JavaNewArrayWithInitializer(getScope().getCFG(), getSourceCodeLocation(node),
					parameters.toArray(new Expression[0]), new JavaReferenceType(type));
		} else {
			ArrayInitializer initializer = node.getInitializer();

			List<Expression> parameters = new ArrayList<>();

			for (Object args : initializer.expressions()) {
				ASTNode e = (ASTNode) args;
				parameters.add(getParserContext().evaluate(e,
						() -> new ExpressionVisitor(getEnvironment(), getScope())));
			}

			expression = new JavaNewArrayWithInitializer(getScope().getCFG(), getSourceCodeLocation(node),
					parameters.toArray(new Expression[0]), new JavaReferenceType(type));
		}

		return false;
	}

	public Type getArrayInitializerType(
			ArrayInitializer node) {
		ASTNode decl = node;
		while (decl.getParent() != null && !(decl.getParent() instanceof FieldDeclaration)) {
			decl = decl.getParent();
		}
		if (decl.getParent() == null) {
			return null;
		}
		return getParserContext().evaluate(((FieldDeclaration) decl.getParent()).getType(),
				() -> new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope()));
	}

	@Override
	public boolean visit(
			Assignment node) {
		Assignment.Operator operator = node.getOperator();
		Expression left = getParserContext().evaluate(node.getLeftHandSide(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		Expression right = getParserContext().evaluate(node.getRightHandSide(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		SourceCodeLocationManager locationManager = getSourceCodeLocationManager(node.getLeftHandSide(), true);

		switch (operator.toString()) {
		case "=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.nextColumn(), left, right);
			break;
		case "+=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new Addition(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		case "-=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new JavaSubtraction(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		case "*=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new Multiplication(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		case "/=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new JavaDivision(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		case "%=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new Modulo(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		case "&=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new JavaBitwiseAnd(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		case "|=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new JavaBitwiseOr(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		case "^=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new JavaBitwiseExclusiveOr(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		case "<<=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new JavaShiftLeft(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		case ">>=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new JavaShiftRight(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		case ">>>=":
			expression = new JavaAssignment(getScope().getCFG(), locationManager.getCurrentLocation(), left,
					new JavaUnsignedShiftRight(getScope().getCFG(), locationManager.nextColumn(), left, right));
			break;
		default:
			throw new RuntimeException(new UnsupportedStatementException("Unknown assignment operator: " + operator));
		}
		return false;
	}

	@Override
	public boolean visit(
			BooleanLiteral node) {
		if (node.booleanValue()) {
			expression = new TrueLiteral(this.getScope().getCFG(), getSourceCodeLocation(node));
		} else {
			expression = new FalseLiteral(this.getScope().getCFG(), getSourceCodeLocation(node));
		}
		return false;
	}

	@Override
	public boolean visit(
			CaseDefaultExpression node) {
		throw new ParsingException("case-default",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Case Default Expressions are not supported.",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			CastExpression node) {
		Type right = getParserContext().evaluate(node.getType(),
				() -> new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope()));
		Expression left = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		expression = new JavaCastExpression(getScope().getCFG(), getSourceCodeLocation(node), left, right);
		return false;
	}

	@Override
	public boolean visit(
			CharacterLiteral node) {
		expression = new CharLiteral(this.getScope().getCFG(), getSourceCodeLocation(node), node.charValue());
		return false;
	}

	@Override
	public boolean visit(
			ClassInstanceCreation node) {
		if (node.getAnonymousClassDeclaration() != null) {
			throw new ParsingException("anonymous-class",
					ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Anonymous classes are not supported.",
					getSourceCodeLocation(node));
		}
		Type type = getParserContext().evaluate(node.getType(),
				() -> new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope()));

		if (!(type instanceof JavaClassType))
			throw new ParsingException("arguments-constructor",
					ParsingException.Type.UNSUPPORTED_STATEMENT,
					"A ClassInstanceCreation Type should be a JavaClassType; got: " + type.getClass().getName(),
					getSourceCodeLocation(node));

		List<Expression> parameters = new LinkedList<>();

		if (node.getExpression() != null) {
			// nested class creation, just pass the expression as first param
			parameters.add(getParserContext().evaluate(node.getExpression(),
					() -> new ExpressionVisitor(getEnvironment(), getScope())));
		}

		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e = (ASTNode) args;
				parameters.add(getParserContext().evaluate(e,
						() -> new ExpressionVisitor(getEnvironment(), getScope())));
			}
		}

		expression = new JavaNewObj(
				getScope().getCFG(),
				node.getExpression() != null ? getSourceCodeLocationManager(node.getExpression()).nextColumn()
						: getSourceCodeLocation(node),
				new JavaReferenceType(type),
				parameters.toArray(new Expression[0]));
		return false;
	}

	@Override
	public boolean visit(
			ConditionalExpression node) {

		Expression conditionExpr = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		if (conditionExpr == null) {
			throw new ParsingException("conditional-expression",
					ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
					"The condition is missing.",
					getSourceCodeLocation(node));
		}

		Expression thenExpr = getParserContext().evaluate(node.getThenExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		if (thenExpr == null)
			throw new ParsingException("conditional-expression",
					ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
					"The then expression is missing.",
					getSourceCodeLocation(node));

		Expression elseExpr = getParserContext().evaluate(node.getElseExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		if (elseExpr == null)
			throw new ParsingException("conditional-expression",
					ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
					"The else expression is missing.",
					getSourceCodeLocation(node));

		expression = new JavaConditionalExpression(getScope().getCFG(),
				getSourceCodeLocationManager(node.getExpression(), true).getCurrentLocation(), conditionExpr, thenExpr,
				elseExpr);

		return false;
	}

	@Override
	public boolean visit(
			CreationReference node) {
		throw new ParsingException("creation-reference",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Creation References are not supported.",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			ExpressionMethodReference node) {
		throw new ParsingException("expression-method-reference",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Expression Method References are not supported.",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			FieldAccess node) {
		Expression expr = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		expression = new JavaAccessInstanceGlobal(getScope().getCFG(),
				getSourceCodeLocationManager(node.getExpression(), true).nextColumn(), expr,
				node.getName().getIdentifier());
		return false;
	}

	@Override
	public boolean visit(
			InfixExpression node) {
		InfixExpression.Operator operator = node.getOperator();
		Expression left = getParserContext().evaluate(node.getLeftOperand(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		Expression right = getParserContext().evaluate(node.getRightOperand(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));

		List<Expression> operands = new ArrayList<>();
		List<ASTNode> jdtOperands = new ArrayList<>();
		operands.add(left);
		jdtOperands.add(node.getLeftOperand());
		operands.add(right);
		jdtOperands.add(node.getRightOperand());
		for (Object n : node.extendedOperands()) {
			Expression ext = getParserContext().evaluate((ASTNode) n,
					() -> new ExpressionVisitor(getEnvironment(), getScope()));
			if (ext != null) {
				operands.add(ext);
				jdtOperands.add((ASTNode) n);
			}
		}

		BinaryFactory factory = INFIX_OPS.get(operator);
		if (factory == null)
			throw new RuntimeException(new UnsupportedStatementException("Unknown infix operator: " + operator));
		expression = buildExpression(operands, jdtOperands,
				(
						first,
						second,
						location) -> factory.build(getScope().getCFG(), location, first, second));
		return false;
	}

	private Expression buildExpression(
			List<Expression> operands,
			List<ASTNode> jdtOperands,
			TriFunction<Expression, Expression, SourceCodeLocation, Expression> opBuilder) {

		if (operands.isEmpty())
			throw new IllegalArgumentException("No operands for expression");

		Expression result = operands.getFirst();
		for (int i = 1; i < operands.size(); i++) {
			result = opBuilder.apply(result, operands.get(i),
					getSourceCodeLocationManager(jdtOperands.get(i - 1), true).getCurrentLocation());
		}
		return result;
	}

	@Override
	public boolean visit(
			InstanceofExpression node) {
		Expression left = getParserContext().evaluate(node.getLeftOperand(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		Type right = getParserContext().evaluate(node.getRightOperand(),
				() -> new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope()));

		expression = new InstanceOf(getScope().getCFG(), getSourceCodeLocationManager(node, true).nextColumn(), left,
				right);

		return false;
	}

	@Override
	public boolean visit(
			LambdaExpression node) {
		throw new ParsingException("lambda-expression",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Lambda expressions are not supported.",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			MethodInvocation node) {
		expression = getParserContext().evaluate(node,
				() -> new MethodInvocationASTVisitor(getEnvironment(), getScope()));
		return false;
	}

	@Override
	public boolean visit(
			QualifiedName node) {
		expression = getParserContext().evaluate(node,
				() -> new NameResolverASTVisitor(getEnvironment(), getScope()));
		return false;
	}

	@Override
	public boolean visit(
			SimpleName node) {
		expression = getParserContext().evaluate(node,
				() -> new NameResolverASTVisitor(getEnvironment(), getScope()));
		return false;
	}

	@Override
	public boolean visit(
			NumberLiteral node) {
		String token = node.getToken();
		if ((token.endsWith("f") || token.endsWith("F")) && !token.startsWith("0x")) {
			// FlOAT
			expression = new FloatLiteral(this.getScope().getCFG(), getSourceCodeLocation(node),
					Float.parseFloat(token));
			return false;
		}
		if (token.contains(".")
				|| ((token.contains("e") || token.contains("E") || token.endsWith("d") || token.endsWith("D"))
						&& !token.startsWith("0x"))) {
			// DOUBLE
			expression = new DoubleLiteral(this.getScope().getCFG(), getSourceCodeLocation(node),
					Double.parseDouble(token));
			return false;
		}
		if (token.endsWith("l") || token.endsWith("L")) {
			// drop 'l' or 'L'
			String value = token.substring(0, token.length() - 1);
			if (value.startsWith("0x") || value.startsWith("0X")) {
				long parsed = Long.parseUnsignedLong(value.substring(2), 16);
				expression = new LongLiteral(this.getScope().getCFG(), getSourceCodeLocation(node), parsed);
			} else {
				long parsed = Long.decode(value);
				expression = new LongLiteral(this.getScope().getCFG(), getSourceCodeLocation(node), parsed);
			}
			return false;
		}
		try {
			long value = Long.decode(token); // handles 0x, 0b, octal, decimal
			if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
				expression = new IntLiteral(this.getScope().getCFG(), getSourceCodeLocation(node), (int) value);
			} else {
				expression = new LongLiteral(this.getScope().getCFG(), getSourceCodeLocation(node), value);
			}
		} catch (NumberFormatException e) {
			throw new RuntimeException("Could not parse " + token + ": not a valid Number Literal", e);
		}
		return false;
	}

	@Override
	public boolean visit(
			NullLiteral node) {
		expression = new JavaNullLiteral(getScope().getCFG(), getSourceCodeLocation(node));
		return false;
	}

	@Override
	public boolean visit(
			ParenthesizedExpression node) {
		expression = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		return false;
	}

	@Override
	public boolean visit(
			PostfixExpression node) {
		Expression expr = getParserContext().evaluate(node.getOperand(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		if (expr == null) {
			return false;
		}
		if (node.getOperator() == PostfixExpression.Operator.INCREMENT) {
			expression = new PostfixAddition(getScope().getCFG(),
					getSourceCodeLocationManager(node.getOperand(), true).nextColumn(),
					expr);
		}
		if (node.getOperator() == PostfixExpression.Operator.DECREMENT) {
			expression = new PostfixSubtraction(getScope().getCFG(),
					getSourceCodeLocationManager(node.getOperand(), true).nextColumn(),
					expr);
		}
		return false;
	}

	@Override
	public boolean visit(
			PrefixExpression node) {
		Expression expr = getParserContext().evaluate(node.getOperand(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		if (expr == null) {
			return false;
		}

		if (node.getOperator() == PrefixExpression.Operator.INCREMENT) {
			expression = new PrefixAddition(getScope().getCFG(), getSourceCodeLocation(node), expr);
		}
		if (node.getOperator() == PrefixExpression.Operator.DECREMENT) {
			expression = new PrefixSubtraction(getScope().getCFG(), getSourceCodeLocation(node), expr);
		}
		if (node.getOperator() == PrefixExpression.Operator.MINUS) {
			expression = new Negation(getScope().getCFG(), getSourceCodeLocation(node), expr);
		}

		if (node.getOperator() == PrefixExpression.Operator.PLUS) {
			expression = new PrefixPlus(getScope().getCFG(), getSourceCodeLocation(node), expr);
		}
		if (node.getOperator() == PrefixExpression.Operator.NOT) {
			expression = new Not(getScope().getCFG(), getSourceCodeLocation(node), expr);
		}
		if (node.getOperator() == PrefixExpression.Operator.COMPLEMENT) {
			expression = new BitwiseNot(getScope().getCFG(), getSourceCodeLocation(node), expr);
		}
		return false;
	}

	@Override
	public boolean visit(
			StringLiteral node) {
		String literal = node.getLiteralValue();
		expression = new JavaStringLiteral(this.getScope().getCFG(), getSourceCodeLocation(node), literal);
		return false;
	}

	@Override
	public boolean visit(
			SuperFieldAccess node) {
		throw new ParsingException("super-field-access",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Super Field Access expressions are not supported.",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			SuperMethodInvocation node) {
		expression = getParserContext().evaluate(node,
				() -> new MethodInvocationASTVisitor(getEnvironment(), getScope()));
		return false;
	}

	@Override
	public boolean visit(
			SuperMethodReference node) {
		throw new ParsingException("super-method-reference",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Super Method Reference expressions are not supported.",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			SwitchExpression node) {
		throw new ParsingException("switch-expression",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Switch Expressions are not supported.",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			ThisExpression node) {
		if (node.getQualifier() != null) {
			if (getScope().getParentScope() == null)
				throw new ParsingException("this-expression",
						ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Qualified this expressions can only be used inside non-static inner classes.",
						getSourceCodeLocation(node));

			it.unive.lisa.type.Type enclosing = getParserContext().evaluate(node.getQualifier(),
					() -> new TypeASTVisitor(getEnvironment(), getScope().getParentScope().unitScope()));
			getScope().getParentScope().getEnclosingClass();
			ClassScope cursor = getScope().getParentScope();
			SyntheticCodeLocationManager synth = getParserContext().getCurrentSyntheticCodeLocationManager(getSource());
			JavaReferenceType type = null;
			if (getScope().getCFG().getUnit() instanceof ClassUnit)
				type = JavaClassType.lookup(getScope().getCFG().getUnit().getName()).getReference();
			else
				type = JavaInterfaceType.lookup(getScope().getCFG().getUnit().getName()).getReference();
			// we accumulate this.$enclosing.$enclosing... until we find the
			// right type or we raise an exception
			expression = new VariableRef(getScope().getCFG(), getSourceCodeLocation(node), "this", type);
			while (cursor != null && cursor.getEnclosingClass() != null) {
				JavaClassType encl = cursor.getEnclosingClass();
				expression = new JavaAccessInstanceGlobal(getScope().getCFG(), synth.nextLocation(), expression,
						"$enclosing");
				if (encl.equals(enclosing))
					return false;
				cursor = cursor.getParentScope();
			}

			throw new ParsingException("this-expression",
					ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Qualified this expression could not find matching enclosing instance.",
					getSourceCodeLocation(node));
		}

		expression = new VariableRef(getScope().getCFG(), getSourceCodeLocation(node), "this", new JavaReferenceType(
				JavaClassType.lookup(((ClassUnit) getScope().getCFG().getUnit()).getName())));
		return false;
	}

	@Override
	public boolean visit(
			TypeLiteral node) {
		// FIXME: we erase the type parameter
		JavaClassType classType = JavaClassType.lookup("java.lang.Class");
		expression = new JavaNewObj(
				getScope().getCFG(),
				getSourceCodeLocation(node),
				new JavaReferenceType(classType),
				new Expression[0]);
		return false;
	}

	@Override
	public boolean visit(
			TypeMethodReference node) {
		throw new ParsingException("type-method-reference",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Type Method References are not supported.",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			SingleVariableDeclaration node) {
		it.unive.lisa.type.Type varType = getParserContext().evaluate(node.getType(),
				() -> new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope()));
		varType = varType.isInMemoryType() ? new JavaReferenceType(varType) : varType;

		String variableName = node.getName().getIdentifier();
		VariableRef ref = new VariableRef(getScope().getCFG(),
				getSourceCodeLocation(node.getName()),
				variableName, varType);

		if (getScope().getTracker() != null && getScope().getTracker().hasVariable(variableName))
			throw new ParsingException("variable-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
					"Variable " + variableName + " already exists in the cfg", getSourceCodeLocation(node));

		if (getScope().getTracker() != null)
			getScope().getTracker().addVariable(variableName, ref, ref.getAnnotations());
		getParserContext().addVariableType(getScope().getCFG(),
				new VariableInfo(variableName,
						getScope().getTracker() != null ? getScope().getTracker().getLocalVariable(variableName)
								: null),
				varType);

		expression = ref;
		return false;
	}

	@Override
	public boolean visit(
			VariableDeclarationExpression node) {
		TypeASTVisitor visitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope());
		node.getType().accept(visitor);
		it.unive.lisa.type.Type varType = visitor.getType();
		varType = varType.isInMemoryType() ? new JavaReferenceType(varType) : varType;

		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String variableName = fragment.getName().getIdentifier();
			varType = visitor.liftToArray(varType, fragment);

			VariableRef ref = new VariableRef(getScope().getCFG(),
					getSourceCodeLocation(fragment),
					variableName, varType);

			if (getScope().getTracker() != null && getScope().getTracker().hasVariable(variableName))
				throw new ParsingException("variable-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
						"Variable " + variableName + " already exists in the cfg", getSourceCodeLocation(node));

			if (getScope().getTracker() != null)
				getScope().getTracker().addVariable(variableName, ref, ref.getAnnotations());
			getParserContext().addVariableType(getScope().getCFG(),
					new VariableInfo(variableName,
							getScope().getTracker() != null ? getScope().getTracker().getLocalVariable(variableName)
									: null),
					varType);

			org.eclipse.jdt.core.dom.Expression expr = fragment.getInitializer();
			Expression initializer = getParserContext().evaluate(expr,
					() -> new ExpressionVisitor(getEnvironment(), getScope()));
			expression = new JavaAssignment(getScope().getCFG(),
					getSourceCodeLocationManager(fragment.getName(), true).getCurrentLocation(), ref, initializer);
		}
		return false;
	}

	public Expression getExpression() {
		return expression;
	}

	@Override
	public Expression getResult() {
		return expression;
	}

}

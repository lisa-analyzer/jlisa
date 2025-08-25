package it.unive.jlisa.frontend.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import it.unive.jlisa.program.cfg.expression.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CaseDefaultExpression;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.program.cfg.statement.JavaAddition;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.jlisa.program.cfg.statement.literal.CharLiteral;
import it.unive.jlisa.program.cfg.statement.literal.DoubleLiteral;
import it.unive.jlisa.program.cfg.statement.literal.FloatLiteral;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.jlisa.program.cfg.statement.literal.JavaStringLiteral;
import it.unive.jlisa.program.cfg.statement.literal.LongLiteral;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.comparison.GreaterThan;
import it.unive.lisa.program.cfg.statement.comparison.LessOrEqual;
import it.unive.lisa.program.cfg.statement.comparison.LessThan;
import it.unive.lisa.program.cfg.statement.comparison.NotEqual;
import it.unive.lisa.program.cfg.statement.literal.FalseLiteral;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.program.cfg.statement.logic.And;
import it.unive.lisa.program.cfg.statement.logic.Not;
import it.unive.lisa.program.cfg.statement.logic.Or;
import it.unive.lisa.program.cfg.statement.numeric.Addition;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.program.cfg.statement.numeric.Modulo;
import it.unive.lisa.program.cfg.statement.numeric.Multiplication;
import it.unive.lisa.program.cfg.statement.numeric.Negation;
import it.unive.lisa.program.cfg.statement.numeric.Subtraction;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;

public class ExpressionVisitor extends JavaASTVisitor {
	private CFG cfg;
	private Expression expression;

	public ExpressionVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg) {
		super(parserContext, source, compilationUnit);
		this.cfg = cfg;
	}


	@Override
	public boolean visit(ArrayAccess node) {
		ExpressionVisitor leftVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		ExpressionVisitor rightVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		node.getArray().accept(leftVisitor);
		node.getIndex().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Expression right = rightVisitor.getExpression();

		expression = new JavaArrayAccess(cfg, getSourceCodeLocation(node), left, right);
		return false;
	}

	@Override
	public boolean visit(ArrayCreation node) {
		// TODO: currently initializer are not supported
		TypeASTVisitor typeVisitor = new TypeASTVisitor(this.parserContext, source, compilationUnit);
		ExpressionVisitor lengthVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);


		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();

		// TODO: currently we handle single-dim arrays
		if(node.dimensions().size() != 0) {
			((ASTNode) node.dimensions().get(0)).accept(lengthVisitor);
			Expression length = lengthVisitor.getExpression();
			expression = new JavaNewArray(cfg, getSourceCodeLocation(node), length, new ReferenceType(type));
		} else {
			ArrayInitializer initializer = node.getInitializer();

			//initializer.expressions();
			List<Expression> parameters = new ArrayList<>();

			for (Object args : initializer.expressions()) {
				ASTNode e  = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				if (expr != null) {
					// This parsing error should be logged in ExpressionVisitor.
					parameters.add(expr);
				}

			}

			expression = new JavaNewArrayWithInitializer(cfg, getSourceCodeLocation(node), parameters.toArray(new Expression[0]), new ReferenceType(type));

		}

		return false;		
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		throw new RuntimeException(new UnsupportedStatementException("Array Initializer expressions are not supported"));
	}


	@Override
	public boolean visit(Assignment node) {
		Assignment.Operator operator = node.getOperator();
		ExpressionVisitor leftVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		ExpressionVisitor rightVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		node.getLeftHandSide().accept(leftVisitor);
		node.getRightHandSide().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Expression right = rightVisitor.getExpression();
		if (left == null || right == null) {
			// SKIP. There is an error.
			return false;
		}
		switch (operator.toString()) {
		case "=":
			expression = new JavaAssignment(cfg, getSourceCodeLocation(node), left, right);
			break;
		case "+=":
			expression = new JavaAssignment(cfg, getSourceCodeLocation(node), left,
					new Addition(cfg, getSourceCodeLocation(node), left, right));
			break;
		case "-=":
			expression = new JavaAssignment(cfg, getSourceCodeLocation(node), left,
					new Subtraction(cfg, getSourceCodeLocation(node), left, right));
			break;
		case "*=":
			expression = new JavaAssignment(cfg, getSourceCodeLocation(node), left,
					new Multiplication(cfg, getSourceCodeLocation(node), left, right));
			break;
		case "/=":
			expression = new JavaAssignment(cfg, getSourceCodeLocation(node), left,
					new Division(cfg, getSourceCodeLocation(node), left, right));
			break;
		case "%=":
			expression = new JavaAssignment(cfg, getSourceCodeLocation(node), left,
					new Modulo(cfg, getSourceCodeLocation(node), left, right));
		case "&=":
			expression = new JavaAssignment(cfg, getSourceCodeLocation(node), left,
					new JavaBitwiseAndOperator(cfg, getSourceCodeLocation(node), left, right));
			break;
		case "|=":
		case "^=":
		case "<<=":
		case ">>=":
		case ">>>=":
			parserContext.addException(
					new ParsingException("operators", ParsingException.Type.UNSUPPORTED_STATEMENT,
							operator + " operator are not supported.",
							getSourceCodeLocation(node))
					);
			break;
		default:
			throw new RuntimeException(new UnsupportedStatementException("Unknown assignment operator: " + operator));
		}
		return false;
	}

	@Override
	public boolean visit(BooleanLiteral node) {
		if (node.booleanValue()) {
			expression = new TrueLiteral(this.cfg, getSourceCodeLocation(node));
		} else {
			expression = new FalseLiteral(this.cfg, getSourceCodeLocation(node));
		}
		return false;
	}

	@Override
	public boolean visit(CaseDefaultExpression node) {
		parserContext.addException(
				new ParsingException("case-default", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Case Default Expressions are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(CastExpression node) {
		ExpressionVisitor rightVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		TypeASTVisitor leftVisitor = new TypeASTVisitor(this.parserContext, source, compilationUnit);
		node.getType().accept(leftVisitor);
		node.getExpression().accept(rightVisitor);
		Expression left = rightVisitor.getExpression();
		Type right = leftVisitor.getType();
		expression = new JavaCastExpression(cfg, getSourceCodeLocation(node), left, right);
		return false;
	}

	@Override
	public boolean visit(CharacterLiteral node) {
		expression = new CharLiteral(this.cfg, getSourceCodeLocation(node), node.charValue());
		return false;
	}


	@Override
	public boolean visit(ClassInstanceCreation node) {
		TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit);
		node.getType().accept(typeVisitor);
		it.unive.lisa.type.Type type = typeVisitor.getType();
		if (type == null) {
			// an error occurred.
			return false;
		}
		if (!(type instanceof JavaClassType)) {
			parserContext.addException(
					new ParsingException("arguments-constructor", ParsingException.Type.UNSUPPORTED_STATEMENT,
							"A ClassInstanceCreation Type should be a JavaClassType; got: " + type.getClass().getName(),
							getSourceCodeLocation(node))
					);
		}
		List<Expression> parameters = new ArrayList<>();
		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e  = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				if (expr != null) {
					// This parsing error should be logged in ExpressionVisitor.
					parameters.add(expr);
				}

			}
		}
		expression = new JavaNewObj(
				cfg,
				getSourceCodeLocation(node),
				((JavaClassType) type).getUnit().getName(),
				new ReferenceType(type),
				parameters.toArray(new Expression[0]));
		return false;
	}

	@Override
	public boolean visit(ConditionalExpression node) {

		ExpressionVisitor conditionVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		node.getExpression().accept(conditionVisitor);
		Expression conditionExpr = conditionVisitor.getExpression();
		if (conditionExpr == null) {
			parserContext.addException(
					new ParsingException("conditional-expression", ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
							"The condition is missing.",
							getSourceCodeLocation(node)));
		}

		ExpressionVisitor thenExprVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		node.getThenExpression().accept(thenExprVisitor);
		Expression thenExpr = thenExprVisitor.getExpression();
		if (thenExpr == null) {
			parserContext.addException(
					new ParsingException("conditional-expression", ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
							"The then expression is missing.",
							getSourceCodeLocation(node)));
		}

		ExpressionVisitor elseExprVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		node.getElseExpression().accept(elseExprVisitor);
		Expression elseExpr = elseExprVisitor.getExpression();
		if (elseExpr == null) {
			parserContext.addException(
					new ParsingException("conditional-expression", ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
							"The else expression is missing.",
							getSourceCodeLocation(node)));
		}

		expression = new JavaConditionalExpression(cfg,getSourceCodeLocation(node), conditionExpr, thenExpr, elseExpr);

		return false;
	}

	@Override
	public boolean visit(CreationReference node) {
		parserContext.addException(
				new ParsingException("creation-reference", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Creation References are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(ExpressionMethodReference node) {
		parserContext.addException(
				new ParsingException("expression-method-reference", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Expression Method References are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(FieldAccess node) {
		ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg);
		node.getExpression().accept(visitor);
		Expression expr = visitor.getExpression();
		if (expr != null) {
			expression = new JavaAccessGlobal(cfg, getSourceCodeLocation(node), expr, node.getName().getIdentifier());
		}
		return false;
	}

	@Override
	public boolean visit(InfixExpression node) {
		InfixExpression.Operator operator = node.getOperator();
		ExpressionVisitor leftVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		ExpressionVisitor rightVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		node.getLeftOperand().accept(leftVisitor);
		node.getRightOperand().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Expression right = rightVisitor.getExpression();
		if (left == null || right == null) {
			// SKIP. There is an error.
			return false;
		}
		List<Expression> operands = new ArrayList<>();
		operands.add(left);
		operands.add(right);
		for (Object n : node.extendedOperands()) {
			ExpressionVisitor extendedOperandVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
			((ASTNode) n).accept(extendedOperandVisitor);
			if (extendedOperandVisitor.getExpression() != null) {
				operands.add(extendedOperandVisitor.getExpression());
			}
		}
		switch (operator.toString()) {
		case "*":
			expression = buildExpression(operands, (first, second) ->
			new Multiplication(cfg, getSourceCodeLocation(node), first, second));
			break;
		case "/":
			expression = buildExpression(operands, (first, second) ->
			new Division(cfg, getSourceCodeLocation(node), first, second));
			break;
		case "%":
			expression = buildExpression(operands, (first, second) ->
			new Modulo(cfg, getSourceCodeLocation(node), first, second));
			break;
		case "+":
			expression = buildExpression(operands, (first, second) ->
			new JavaAddition(cfg, getOperatorLocation(node), first, second));
			break;
		case "-":
			expression = buildExpression(operands, (first, second) ->
			new Subtraction(cfg, getSourceCodeLocation(node), first, second));
			break;
		case ">>":
			expression = buildExpression(operands, (first, second) ->
			new JavaShiftOperator(cfg, getSourceCodeLocation(node), first, second));
			break;
		case "<<":
		case ">>>":
			parserContext.addException(
					new ParsingException("infix-operator", ParsingException.Type.UNSUPPORTED_STATEMENT,
							"The '" + operator + "' infix operator is not supported.",
							getSourceCodeLocation(node))
					);
			break;
		case "<":
			expression = new LessThan(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
			break;
		case ">":
			expression = new GreaterThan(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
			break;
		case "<=":
			expression = new LessOrEqual(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
			break;
		case ">=":
			expression = new GreaterThan(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
			break;
		case "==":
			expression = buildExpression(operands, (first, second) ->
			new Equal(cfg, getSourceCodeLocation(node), first, second));
			break;
		case "!=":
			expression = buildExpression(operands, (first, second) ->
			new NotEqual(cfg, getSourceCodeLocation(node), first, second));
			break;
		case "&":
			expression = buildExpression(operands, (first, second) ->
			new JavaBitwiseAndOperator(cfg, getSourceCodeLocation(node), first, second));
			break;
		case "^":
		case "|":
			parserContext.addException(
					new ParsingException("infix-operator", ParsingException.Type.UNSUPPORTED_STATEMENT,
							"The '" + operator + "' infix operator is not supported.",
							getSourceCodeLocation(node))
					);
			break;
		case "&&":
			expression = buildExpression(operands, (first, second) ->
			new And(cfg, getSourceCodeLocation(node), first, second));
			break;
		case "||":
			expression = buildExpression(operands, (first, second) ->
			new Or(cfg, getSourceCodeLocation(node), first, second));
			break;
		default:
			throw new RuntimeException(new UnsupportedStatementException("Unknown infix operator: " + operator));
		}
		return false;
	}

	private Expression buildExpression(
			List<Expression> operands,
			BiFunction<Expression, Expression, Expression> opBuilder) {

		if (operands.isEmpty())
			throw new IllegalArgumentException("No operands for expression");

		Expression result = operands.getFirst();
		for (int i = 1; i < operands.size(); i++) {
			result = opBuilder.apply(result, operands.get(i));
		}
		return result;
	}

	@Override
	public boolean visit(InstanceofExpression node) {
		ExpressionVisitor leftVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		TypeASTVisitor rightVisitor = new TypeASTVisitor(this.parserContext, source, compilationUnit);
		node.getLeftOperand().accept(leftVisitor);
		node.getRightOperand().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Type right = rightVisitor.getType();

		expression = new InstanceOf(cfg, getSourceCodeLocation(node), left, right);

		return false;
	}

	@Override
	public boolean visit(LambdaExpression node) {
		parserContext.addException(
				new ParsingException("lambda-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Lambda expressions are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		ExpressionVisitor receiver = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		List<Expression> parameters = new ArrayList<>();
		boolean instance = false;
		if (node.getExpression() != null) {
			node.getExpression().accept(receiver);
			if (receiver.getExpression() != null) {
				parameters.add(receiver.getExpression());
				instance = true;
			}
		}
		if (!node.typeArguments().isEmpty()) {
			parserContext.addException(
					new ParsingException("method-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
							"Method Invocation expressions with type arguments are not supported.",
							getSourceCodeLocation(node))
					);
		}
		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e  = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				if (expr != null) {
					// This parsing error should be logged in ExpressionVisitor.
					parameters.add(expr);
				}
			}
		}
		// TODO: REASON ABOUT INSTANCE / STATIC B.m() -> static, b.m() -> NOT STATIC, m() -> both satic and non-static
		// TODO: instead of Call.CallType.UNKNOWN, we can provide better information of the call type
		expression = new UnresolvedCall(cfg, getSourceCodeLocation(node), Call.CallType.UNKNOWN, null,node.getName().toString(), parameters.toArray(new Expression[0]));
		return false;
	}

	@Override
	public boolean visit(QualifiedName node) {
		String target = node.getName().getIdentifier(); // you may still want this
		ExpressionVisitor receiverVisitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg);
		if (node.getQualifier() instanceof QualifiedName) {
			parserContext.addException(
					new ParsingException("qualified-name", ParsingException.Type.UNSUPPORTED_STATEMENT,
							"Qualified Name expressions as qualifier of Qualifier Name expressions are not supported.",
							getSourceCodeLocation(node))
					);
		}
		node.getQualifier().accept(receiverVisitor);
		Expression receiver = receiverVisitor.getExpression();
		if (receiver == null) {
			return false;
		}
		expression = new JavaAccessGlobal(cfg, getSourceCodeLocation(node),receiver, target);
		return false;
	}

	@Override
	public boolean visit(SimpleName node) {
		String identifier = node.getIdentifier();
		expression = new VariableRef(cfg, getSourceCodeLocation(node), identifier, parserContext.getVariableStaticType(cfg, identifier));
		return false;
	}

	@Override
	public boolean visit(NumberLiteral node) {
		String token = node.getToken();
		if ((token.endsWith("f") || token.endsWith("F")) && !token.startsWith("0x")) {
			// FlOAT
			expression = new FloatLiteral(this.cfg, getSourceCodeLocation(node), Float.parseFloat(token));
			return false;
		}
		if (token.contains(".") || ((token.contains("e") || token.contains("E") || token.endsWith("d") || token.endsWith("D")) && !token.startsWith("0x"))) {
			// DOUBLE
			expression = new DoubleLiteral(this.cfg, getSourceCodeLocation(node), Double.parseDouble(token));
			return false;
		}
		if (token.endsWith("l") || token.endsWith("L")) {
			// drop 'l' or 'L'
			String value = token.substring(0, token.length() - 1);
			if (value.startsWith("0x") || value.startsWith("0X")) {
			    long parsed = Long.parseUnsignedLong(value.substring(2), 16);
			    expression = new LongLiteral(this.cfg, getSourceCodeLocation(node), parsed);
			} else {
			    long parsed = Long.decode(value);
			    expression = new LongLiteral(this.cfg, getSourceCodeLocation(node), parsed);
			}
			return false;
		}
		try {
			long value = Long.decode(token); // handles 0x, 0b, octal, decimal
			if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
				expression = new IntLiteral(this.cfg, getSourceCodeLocation(node), (int) value);
			} else {
				expression = new LongLiteral(this.cfg, getSourceCodeLocation(node), value);
			}
		} catch (NumberFormatException e) {
			throw new RuntimeException("Could not parse " + token + ": not a valid Number Literal", e );
		}
		return false;
	}

	@Override
	public boolean visit(NullLiteral node) {
		expression = new it.unive.lisa.program.cfg.statement.literal.NullLiteral(cfg, getSourceCodeLocation(node));
		return false;
	}

	@Override
	public boolean visit(ParenthesizedExpression node) {
		ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg);
		node.getExpression().accept(visitor);
		expression = visitor.getExpression();
		return false;
	}

	@Override
	public boolean visit(PostfixExpression node) {
		ExpressionVisitor sev = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		node.getOperand().accept(sev);
		Expression expr = sev.getExpression();
		if (expr == null) {
			return false;
		}
		if (node.getOperator() == PostfixExpression.Operator.INCREMENT) {
			expression = new PostfixAddition(cfg, getSourceCodeLocation(node), expr);
		}
		if (node.getOperator() == PostfixExpression.Operator.DECREMENT) {
			expression = new PostfixSubtraction(cfg, getSourceCodeLocation(node), expr);
		}
		return false;
	}

	@Override
	public boolean visit(PrefixExpression node) {
		ExpressionVisitor sev = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		node.getOperand().accept(sev);
		Expression expr = sev.getExpression();
		if (expr == null) {
			return false;
		}

		if (node.getOperator() == PrefixExpression.Operator.INCREMENT) {
			expression = new PrefixAddition(cfg, getSourceCodeLocation(node), expr);
		}
		if (node.getOperator() == PrefixExpression.Operator.DECREMENT) {
			expression = new PrefixSubtraction(cfg, getSourceCodeLocation(node), expr);
		}
		if (node.getOperator() == PrefixExpression.Operator.MINUS) {
			expression = new Negation(cfg, getSourceCodeLocation(node), expr);
		}

		if (node.getOperator() == PrefixExpression.Operator.PLUS) {
			expression = new PrefixPlus(cfg, getSourceCodeLocation(node), expr);
		}
		if (node.getOperator() == PrefixExpression.Operator.NOT) {
			expression = new Not(cfg, getSourceCodeLocation(node), expr);
		}
		if (node.getOperator() == PrefixExpression.Operator.COMPLEMENT) {
			expression = new BitwiseNot(cfg, getSourceCodeLocation(node), expr);
		}
		return false;
	}

	@Override
	public boolean visit(StringLiteral node) {
		String literal = node.getLiteralValue();
		expression = new JavaStringLiteral(this.cfg, getSourceCodeLocation(node), literal);
		return false;
	}

	@Override
	public boolean visit(SuperFieldAccess node) {

		parserContext.addException(
				new ParsingException("super-field-access", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Super Field Access expressions are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {

		parserContext.addException(
				new ParsingException("super-method-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Super Method Invocation expressions are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(SuperMethodReference node) {
		parserContext.addException(
				new ParsingException("super-method-reference", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Super Method Reference expressions are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(SwitchExpression node) {
		parserContext.addException(
				new ParsingException("switch-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Switch Expressions are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(ThisExpression node) {
		if (node.getQualifier() != null) {
			parserContext.addException(
					new ParsingException("this-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
							"Qualified This Expressions are not supported.",
							getSourceCodeLocation(node))
					);
		}
		expression = new VariableRef(cfg, getSourceCodeLocation(node), "this");
		return false;
	}

	@Override
	public boolean visit(TypeLiteral node) {
		parserContext.addException(
				new ParsingException("type-literal", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Type Literals are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(TypeMethodReference node) {
		parserContext.addException(
				new ParsingException("type-method-reference", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Type Method References are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationExpression node) {
		TypeASTVisitor visitor = new TypeASTVisitor(this.parserContext, source, compilationUnit);
		node.getType().accept(visitor);
		it.unive.lisa.type.Type variableType = visitor.getType();
		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String variableName = fragment.getName().getIdentifier();
			SourceCodeLocation loc = getSourceCodeLocation(fragment);
			VariableRef ref = new VariableRef(cfg,
					getSourceCodeLocation(fragment),
					variableName, variableType);
			parserContext.addVariableType(cfg,variableName, variableType);

			org.eclipse.jdt.core.dom.Expression expr = fragment.getInitializer();
			ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg);
			expr.accept(exprVisitor);
			Expression initializer = exprVisitor.getExpression();
			expression= new JavaAssignment(cfg, loc, ref, initializer);
		}
		return false;
	}


	public Expression getExpression() {
		return expression;
	}


}
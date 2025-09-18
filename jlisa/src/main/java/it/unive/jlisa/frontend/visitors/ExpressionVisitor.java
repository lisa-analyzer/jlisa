package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.frontend.util.JavaLocalVariableTracker;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.BitwiseNot;
import it.unive.jlisa.program.cfg.expression.InstanceOf;
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
import it.unive.jlisa.program.cfg.expression.JavaShiftLeft;
import it.unive.jlisa.program.cfg.expression.JavaShiftRight;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedCall;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedStaticCall;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedSuperCall;
import it.unive.jlisa.program.cfg.expression.JavaUnsignedShiftRight;
import it.unive.jlisa.program.cfg.expression.PostfixAddition;
import it.unive.jlisa.program.cfg.expression.PostfixSubtraction;
import it.unive.jlisa.program.cfg.expression.PrefixAddition;
import it.unive.jlisa.program.cfg.expression.PrefixPlus;
import it.unive.jlisa.program.cfg.expression.PrefixSubtraction;
import it.unive.jlisa.program.cfg.statement.JavaAddition;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.JavaSubtraction;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
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
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.comparison.GreaterOrEqual;
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
import it.unive.lisa.program.cfg.statement.numeric.Modulo;
import it.unive.lisa.program.cfg.statement.numeric.Multiplication;
import it.unive.lisa.program.cfg.statement.numeric.Negation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.logging.log4j.Logger;
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
import org.eclipse.jdt.core.dom.Name;
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

public class ExpressionVisitor extends BaseCodeElementASTVisitor {
	private CFG cfg;
	private Expression expression;

	private JavaLocalVariableTracker tracker;

	private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(ExpressionVisitor.class);

	public ExpressionVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit,
			CFG cfg,
			JavaLocalVariableTracker tracker,
			BaseUnitASTVisitor container) {
		super(parserContext, source, compilationUnit, container);
		this.cfg = cfg;
		this.tracker = tracker;
	}

	@Override
	public boolean visit(
			ArrayAccess node) {
		ExpressionVisitor leftVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		ExpressionVisitor rightVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		node.getArray().accept(leftVisitor);
		node.getIndex().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Expression right = rightVisitor.getExpression();
		expression = new JavaArrayAccess(cfg, getSourceCodeLocationManager(node.getArray(), true).getCurrentLocation(),
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
			ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg,
					tracker, container);
			e.accept(argumentsVisitor);
			Expression expr = argumentsVisitor.getExpression();
			parameters.add(expr);
			contentType = expr.getStaticType();
		}

		expression = new JavaNewArrayWithInitializer(cfg,
				getSourceCodeLocation(node),
				parameters.toArray(new Expression[0]),
				new JavaReferenceType(JavaArrayType.lookup(contentType, 1)));
		return false;
	}

	@Override
	public boolean visit(
			ArrayCreation node) {
		TypeASTVisitor typeVisitor = new TypeASTVisitor(this.parserContext, source, compilationUnit, container);
		ExpressionVisitor lengthVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);

		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();

		if (node.dimensions().size() > 1)
			throw new ParsingException("multi-dim array", ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Multi-dimensional arrays are not supported are not supported.",
					getSourceCodeLocation(node));

		// TODO: currently we handle single-dim arrays
		if (node.dimensions().size() != 0) {
			((ASTNode) node.dimensions().get(0)).accept(lengthVisitor);
			Expression length = lengthVisitor.getExpression();
			expression = new JavaNewArray(cfg, getSourceCodeLocation(node), length, new JavaReferenceType(type));
		} else {
			ArrayInitializer initializer = node.getInitializer();

			// initializer.expressions();
			List<Expression> parameters = new ArrayList<>();

			for (Object args : initializer.expressions()) {
				ASTNode e = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg,
						tracker, container);
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				parameters.add(expr);
			}

			expression = new JavaNewArrayWithInitializer(cfg, getSourceCodeLocation(node),
					parameters.toArray(new Expression[0]), new JavaReferenceType(type));
		}

		return false;
	}

	@Override
	public boolean visit(
			Assignment node) {
		Assignment.Operator operator = node.getOperator();
		ExpressionVisitor leftVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		ExpressionVisitor rightVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		node.getLeftHandSide().accept(leftVisitor);
		node.getRightHandSide().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Expression right = rightVisitor.getExpression();
		SourceCodeLocationManager locationManager = getSourceCodeLocationManager(node.getLeftHandSide(), true);

		switch (operator.toString()) {
		case "=":
			expression = new JavaAssignment(cfg, locationManager.nextColumn(), left, right);
			break;
		case "+=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new Addition(cfg, locationManager.nextColumn(), left, right));
			break;
		case "-=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new JavaSubtraction(cfg, locationManager.nextColumn(), left, right));
			break;
		case "*=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new Multiplication(cfg, locationManager.nextColumn(), left, right));
			break;
		case "/=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new JavaDivision(cfg, locationManager.nextColumn(), left, right));
			break;
		case "%=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new Modulo(cfg, locationManager.nextColumn(), left, right));
			break;
		case "&=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new JavaBitwiseAnd(cfg, locationManager.nextColumn(), left, right));
			break;
		case "|=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new JavaBitwiseOr(cfg, locationManager.nextColumn(), left, right));
			break;
		case "^=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new JavaBitwiseExclusiveOr(cfg, locationManager.nextColumn(), left, right));
			break;
		case "<<=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new JavaShiftLeft(cfg, locationManager.nextColumn(), left, right));
			break;
		case ">>=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new JavaShiftRight(cfg, locationManager.nextColumn(), left, right));
			break;
		case ">>>=":
			expression = new JavaAssignment(cfg, locationManager.getCurrentLocation(), left,
					new JavaUnsignedShiftRight(cfg, locationManager.nextColumn(), left, right));
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
			expression = new TrueLiteral(this.cfg, getSourceCodeLocation(node));
		} else {
			expression = new FalseLiteral(this.cfg, getSourceCodeLocation(node));
		}
		return false;
	}

	@Override
	public boolean visit(
			CaseDefaultExpression node) {
		parserContext.addException(
				new ParsingException("case-default", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Case Default Expressions are not supported.",
						getSourceCodeLocation(node)));
		return false;
	}

	@Override
	public boolean visit(
			CastExpression node) {
		ExpressionVisitor rightVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		TypeASTVisitor leftVisitor = new TypeASTVisitor(this.parserContext, source, compilationUnit, container);
		node.getType().accept(leftVisitor);
		node.getExpression().accept(rightVisitor);
		Expression left = rightVisitor.getExpression();
		Type right = leftVisitor.getType();
		expression = new JavaCastExpression(cfg, getSourceCodeLocation(node), left, right);
		return false;
	}

	@Override
	public boolean visit(
			CharacterLiteral node) {
		expression = new CharLiteral(this.cfg, getSourceCodeLocation(node), node.charValue());
		return false;
	}

	@Override
	public boolean visit(
			ClassInstanceCreation node) {
		TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit, container);
		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();

		if (!(type instanceof JavaClassType)) {
			parserContext.addException(
					new ParsingException("arguments-constructor", ParsingException.Type.UNSUPPORTED_STATEMENT,
							"A ClassInstanceCreation Type should be a JavaClassType; got: " + type.getClass().getName(),
							getSourceCodeLocation(node)));
		}
		List<Expression> parameters = new ArrayList<>();
		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg,
						tracker, container);
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				parameters.add(expr);
			}
		}

		expression = new JavaNewObj(
				cfg,
				getSourceCodeLocation(node),
				((JavaClassType) type).getUnit().getName(),
				new JavaReferenceType(type),
				parameters.toArray(new Expression[0]));
		return false;
	}

	@Override
	public boolean visit(
			ConditionalExpression node) {

		ExpressionVisitor conditionVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg,
				tracker, container);
		node.getExpression().accept(conditionVisitor);
		Expression conditionExpr = conditionVisitor.getExpression();
		if (conditionExpr == null) {
			parserContext.addException(
					new ParsingException("conditional-expression", ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
							"The condition is missing.",
							getSourceCodeLocation(node)));
		}

		ExpressionVisitor thenExprVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		node.getThenExpression().accept(thenExprVisitor);
		Expression thenExpr = thenExprVisitor.getExpression();
		if (thenExpr == null) {
			parserContext.addException(
					new ParsingException("conditional-expression", ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
							"The then expression is missing.",
							getSourceCodeLocation(node)));
		}

		ExpressionVisitor elseExprVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		node.getElseExpression().accept(elseExprVisitor);
		Expression elseExpr = elseExprVisitor.getExpression();
		if (elseExpr == null) {
			parserContext.addException(
					new ParsingException("conditional-expression", ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
							"The else expression is missing.",
							getSourceCodeLocation(node)));
		}

		expression = new JavaConditionalExpression(cfg,
				getSourceCodeLocationManager(node.getExpression(), true).getCurrentLocation(), conditionExpr, thenExpr,
				elseExpr);

		return false;
	}

	@Override
	public boolean visit(
			CreationReference node) {
		parserContext.addException(
				new ParsingException("creation-reference", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Creation References are not supported.",
						getSourceCodeLocation(node)));
		return false;
	}

	@Override
	public boolean visit(
			ExpressionMethodReference node) {
		parserContext.addException(
				new ParsingException("expression-method-reference", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Expression Method References are not supported.",
						getSourceCodeLocation(node)));
		return false;
	}

	@Override
	public boolean visit(
			FieldAccess node) {
		ExpressionVisitor visitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		node.getExpression().accept(visitor);
		Expression expr = visitor.getExpression();
		expression = new JavaAccessInstanceGlobal(cfg,
				getSourceCodeLocationManager(node.getExpression(), true).nextColumn(), expr,
				node.getName().getIdentifier());
		return false;
	}

	@Override
	public boolean visit(
			InfixExpression node) {
		InfixExpression.Operator operator = node.getOperator();
		ExpressionVisitor leftVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		ExpressionVisitor rightVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		node.getLeftOperand().accept(leftVisitor);
		node.getRightOperand().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Expression right = rightVisitor.getExpression();

		List<Expression> operands = new ArrayList<>();
		List<ASTNode> jdtOperands = new ArrayList<>();
		operands.add(left);
		jdtOperands.add(node.getLeftOperand());
		operands.add(right);
		jdtOperands.add(node.getRightOperand());
		for (Object n : node.extendedOperands()) {
			ExpressionVisitor extendedOperandVisitor = new ExpressionVisitor(parserContext, source, compilationUnit,
					cfg, tracker, container);
			((ASTNode) n).accept(extendedOperandVisitor);
			if (extendedOperandVisitor.getExpression() != null) {
				operands.add(extendedOperandVisitor.getExpression());
				jdtOperands.add((ASTNode) n);
			}
		}

		switch (operator.toString()) {
		case "*":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new Multiplication(cfg, location, first, second));
			break;
		case "/":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaDivision(cfg, location, first, second));
			break;
		case "%":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new Modulo(cfg, location, first, second));
			break;
		case "+":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaAddition(cfg, location, first, second));
			break;
		case "-":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaSubtraction(cfg, location, first, second));
			break;
		case ">>":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaShiftRight(cfg, location, first, second));
			break;
		case "<<":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaShiftLeft(cfg, location, first, second));
			break;
		case ">>>":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaUnsignedShiftRight(cfg, location, first, second));
			break;
		case "<":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new LessThan(cfg, location, first, second));
			break;
		case ">":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new GreaterThan(cfg, location, first, second));
			break;
		case "<=":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new LessOrEqual(cfg, location, first, second));
			break;
		case ">=":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new GreaterOrEqual(cfg, location, first, second));
			break;
		case "==":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new Equal(cfg, location, first, second));
			break;
		case "!=":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new NotEqual(cfg, location, first, second));
			break;
		case "&":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaBitwiseAnd(cfg, location, first, second));
			break;
		case "^":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaBitwiseExclusiveOr(cfg, location, first, second));
			break;
		case "|":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaBitwiseOr(cfg, location, first, second));
			break;
		case "&&":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new And(cfg, location, first, second));
			break;
		case "||":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new Or(cfg, location, first, second));
			break;
		default:
			throw new RuntimeException(new UnsupportedStatementException("Unknown infix operator: " + operator));
		}
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
		ExpressionVisitor leftVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		TypeASTVisitor rightVisitor = new TypeASTVisitor(this.parserContext, source, compilationUnit, container);
		node.getLeftOperand().accept(leftVisitor);
		node.getRightOperand().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Type right = rightVisitor.getType();

		expression = new InstanceOf(cfg, getSourceCodeLocationManager(node, true).nextColumn(), left, right);

		return false;
	}

	@Override
	public boolean visit(
			LambdaExpression node) {
		parserContext.addException(
				new ParsingException("lambda-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Lambda expressions are not supported.",
						getSourceCodeLocation(node)));
		return false;
	}

	@Override
	public boolean visit(
			MethodInvocation node) {
		ExpressionVisitor receiver = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		List<Expression> parameters = new ArrayList<>();
		String methodName = node.getName().toString();
		ClassUnit classUnit = (ClassUnit) this.cfg.getUnit();

		boolean isInstance;
		// we do not have a receiver
		if (node.getExpression() == null) {
			isInstance = !classUnit.getInstanceCodeMembersByName(methodName, true).isEmpty();

			// if instance, we add this as parameter
			if (isInstance)
				parameters.add(new VariableRef(cfg, getSourceCodeLocation(node), "this",
						new JavaReferenceType(JavaClassType.lookup(classUnit.getName(), null))));
		} else {
			node.getExpression().accept(receiver);
			if (JavaClassType.hasType(node.getExpression().toString()))
				isInstance = false;
			else {
				parameters.add(receiver.getExpression());
				isInstance = true;
			}
		}

		if (!node.typeArguments().isEmpty()) {
			parserContext.addException(
					new ParsingException("method-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
							"Method Invocation expressions with type arguments are not supported.",
							getSourceCodeLocation(node)));
		}
		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg,
						tracker, container);
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				parameters.add(expr);
			}
		}

		if (isInstance)
			expression = new JavaUnresolvedCall(cfg, getSourceCodeLocationManager(node.getName()).nextColumn(),
					Call.CallType.INSTANCE, null, node.getName().toString(), parameters.toArray(new Expression[0]));
		else
			expression = new JavaUnresolvedStaticCall(cfg, getSourceCodeLocationManager(node.getName()).nextColumn(),
					node.getExpression() == null ? classUnit.getName() : node.getExpression().toString(),
					node.getName().toString(), parameters.toArray(new Expression[0]));

		return false;
	}

	@Override
	public boolean visit(
			QualifiedName node) {
		String targetName = node.getName().getIdentifier();

		// need to resolve recursively
		if (node.getQualifier() instanceof QualifiedName) {
			ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg,
					tracker, container);
			node.getQualifier().accept(visitor);
			Expression expr = visitor.getExpression();
			expression = new JavaAccessInstanceGlobal(cfg,
					getSourceCodeLocationManager(node.getQualifier(), true).nextColumn(), expr,
					node.getName().getIdentifier());
			return false;
		}

		// FIXME: we are currently taking just the last name (the true name of
		// the unit)
		String unitName;
		Name lastName = node.getQualifier();

		if (node.getQualifier() instanceof SimpleName)
			unitName = lastName.toString();
		else {
			while (lastName instanceof QualifiedName)
				lastName = ((QualifiedName) lastName).getQualifier();
			unitName = lastName.toString();
		}

		Unit unit = getProgram().getUnit(unitName);
		if (unit == null) {
			// FIXME: WORKAROUND FOR SEARCHING FOR MISSING LIBRARIES
			if (Character.isUpperCase(unitName.charAt(0)))
				parserContext.addException(
						new ParsingException("missing-type", ParsingException.Type.UNSUPPORTED_STATEMENT,
								"Missing unit " + unitName,
								getSourceCodeLocation(node)));
			else {
				// it is a field access
				ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg,
						tracker, container);
				lastName.accept(visitor);
				Expression expr = visitor.getExpression();
				expression = new JavaAccessInstanceGlobal(cfg,
						getSourceCodeLocationManager(node.getQualifier(), true).nextColumn(), expr,
						node.getName().getIdentifier());
				return false;
			}
		}

		Global global = parserContext.getGlobal(unit, targetName);
		if (global == null) {
			LOG.warn("Global " + targetName + " not found in unit " + unit.getName() + ".");
			/*
			 * parserContext.addException(new
			 * ParsingException("missing_globals",
			 * ParsingException.Type.MALFORMED_SOURCE, "Global " + targetName +
			 * " not found in unit " + unit.getName() + ".",
			 * getSourceCodeLocation(node)));
			 */
			global = new Global(getSourceCodeLocation(node), unit, targetName, false);
		}
		expression = new JavaAccessGlobal(cfg,
				getSourceCodeLocationManager(node.getQualifier(), true).getCurrentLocation(), unit, global);
		return false;
	}

	@Override
	public boolean visit(
			SimpleName node) {
		String identifier = node.getIdentifier();
		expression = new VariableRef(cfg, getSourceCodeLocation(node), identifier, parserContext.getVariableStaticType(
				cfg, new VariableInfo(identifier, tracker != null ? tracker.getLocalVariable(identifier) : null)));
		return false;
	}

	@Override
	public boolean visit(
			NumberLiteral node) {
		String token = node.getToken();
		if ((token.endsWith("f") || token.endsWith("F")) && !token.startsWith("0x")) {
			// FlOAT
			expression = new FloatLiteral(this.cfg, getSourceCodeLocation(node), Float.parseFloat(token));
			return false;
		}
		if (token.contains(".")
				|| ((token.contains("e") || token.contains("E") || token.endsWith("d") || token.endsWith("D"))
						&& !token.startsWith("0x"))) {
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
			throw new RuntimeException("Could not parse " + token + ": not a valid Number Literal", e);
		}
		return false;
	}

	@Override
	public boolean visit(
			NullLiteral node) {
		expression = new JavaNullLiteral(cfg, getSourceCodeLocation(node));
		return false;
	}

	@Override
	public boolean visit(
			ParenthesizedExpression node) {
		ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg, tracker, container);
		node.getExpression().accept(visitor);
		expression = visitor.getExpression();
		return false;
	}

	@Override
	public boolean visit(
			PostfixExpression node) {
		ExpressionVisitor sev = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
		node.getOperand().accept(sev);
		Expression expr = sev.getExpression();
		if (expr == null) {
			return false;
		}
		if (node.getOperator() == PostfixExpression.Operator.INCREMENT) {
			expression = new PostfixAddition(cfg, getSourceCodeLocationManager(node.getOperand(), true).nextColumn(),
					expr);
		}
		if (node.getOperator() == PostfixExpression.Operator.DECREMENT) {
			expression = new PostfixSubtraction(cfg, getSourceCodeLocationManager(node.getOperand(), true).nextColumn(),
					expr);
		}
		return false;
	}

	@Override
	public boolean visit(
			PrefixExpression node) {
		ExpressionVisitor sev = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
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
	public boolean visit(
			StringLiteral node) {
		String literal = node.getLiteralValue();
		expression = new JavaStringLiteral(this.cfg, getSourceCodeLocation(node), literal);
		return false;
	}

	@Override
	public boolean visit(
			SuperFieldAccess node) {

		parserContext.addException(
				new ParsingException("super-field-access", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Super Field Access expressions are not supported.",
						getSourceCodeLocation(node)));
		return false;
	}

	@Override
	public boolean visit(
			SuperMethodInvocation node) {
		ClassUnit superClass = (ClassUnit) this.cfg.getUnit();
		JavaClassType superType = JavaClassType.lookup(superClass.getName(), null);

		// craft the call to superclass
		List<Expression> parameters = new ArrayList<>();
		parameters.add(new VariableRef(cfg, getSourceCodeLocation(node), "this", new JavaReferenceType(superType)));

		for (Object args : node.arguments()) {
			ASTNode e = (ASTNode) args;
			ExpressionVisitor argVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker, container);
			e.accept(argVisitor);
			Expression expr = argVisitor.getExpression();
			parameters.add(expr);
		}

		expression = new JavaUnresolvedSuperCall(cfg, getSourceCodeLocationManager(node.getName()).nextColumn(),
				Call.CallType.INSTANCE, superClass.getName(), node.getName().toString(),
				parameters.toArray(new Expression[0]));
		return false;
	}

	@Override
	public boolean visit(
			SuperMethodReference node) {
		parserContext.addException(
				new ParsingException("super-method-reference", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Super Method Reference expressions are not supported.",
						getSourceCodeLocation(node)));
		return false;
	}

	@Override
	public boolean visit(
			SwitchExpression node) {
		parserContext.addException(
				new ParsingException("switch-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Switch Expressions are not supported.",
						getSourceCodeLocation(node)));
		return false;
	}

	@Override
	public boolean visit(
			ThisExpression node) {
		if (node.getQualifier() != null) {
			parserContext.addException(
					new ParsingException("this-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
							"Qualified This Expressions are not supported.",
							getSourceCodeLocation(node)));
		}
		expression = new VariableRef(cfg, getSourceCodeLocation(node), "this");
		return false;
	}

	@Override
	public boolean visit(
			TypeLiteral node) {
		TypeASTVisitor visitor = new TypeASTVisitor(this.parserContext, source, compilationUnit, container);
		node.getType().accept(visitor);

		// FIXME: we erase the type parameter
		JavaClassType classType = JavaClassType.lookup("Class", null);
		expression = new JavaNewObj(
				cfg,
				getSourceCodeLocation(node),
				classType.getUnit().getName(),
				new JavaReferenceType(classType),
				new Expression[0]);
		return false;
	}

	@Override
	public boolean visit(
			TypeMethodReference node) {
		parserContext.addException(
				new ParsingException("type-method-reference", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Type Method References are not supported.",
						getSourceCodeLocation(node)));
		return false;
	}

	@Override
	public boolean visit(
			VariableDeclarationExpression node) {
		TypeASTVisitor visitor = new TypeASTVisitor(this.parserContext, source, compilationUnit, container);
		node.getType().accept(visitor);
		it.unive.lisa.type.Type varType = visitor.getType();
		varType = varType.isInMemoryType() ? new JavaReferenceType(varType) : varType;

		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String variableName = fragment.getName().getIdentifier();
			VariableRef ref = new VariableRef(cfg,
					getSourceCodeLocation(fragment),
					variableName, varType);

			if (tracker != null && tracker.hasVariable(variableName))
				throw new ParsingException("variable-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
						"Variable " + variableName + " already exists in the cfg", getSourceCodeLocation(node));

			if (tracker != null)
				tracker.addVariable(variableName, ref, ref.getAnnotations());
			parserContext.addVariableType(cfg,
					new VariableInfo(variableName, tracker != null ? tracker.getLocalVariable(variableName) : null),
					varType);

			org.eclipse.jdt.core.dom.Expression expr = fragment.getInitializer();
			ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg,
					tracker, container);
			expr.accept(exprVisitor);
			Expression initializer = exprVisitor.getExpression();
			expression = new JavaAssignment(cfg,
					getSourceCodeLocationManager(fragment.getName(), true).getCurrentLocation(), ref, initializer);
		}
		return false;
	}

	public Expression getExpression() {
		return expression;
	}

}
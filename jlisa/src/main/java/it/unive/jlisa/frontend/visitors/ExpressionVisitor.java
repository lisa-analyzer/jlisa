package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.frontend.util.VariableInfo;
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
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.*;
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
import it.unive.lisa.program.cfg.statement.logic.Not;
import it.unive.lisa.program.cfg.statement.numeric.Addition;
import it.unive.lisa.program.cfg.statement.numeric.Modulo;
import it.unive.lisa.program.cfg.statement.numeric.Multiplication;
import it.unive.lisa.program.cfg.statement.numeric.Negation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.workset.LIFOWorkingSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.tuple.Triple;
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
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
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
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
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

public class ExpressionVisitor extends ScopedVisitor<MethodScope> {
	private Expression expression;

	public ExpressionVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			ArrayAccess node) {
		ExpressionVisitor leftVisitor = new ExpressionVisitor(getEnvironment(), getScope());
		ExpressionVisitor rightVisitor = new ExpressionVisitor(getEnvironment(), getScope());
		node.getArray().accept(leftVisitor);
		node.getIndex().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Expression right = rightVisitor.getExpression();
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
			ExpressionVisitor argumentsVisitor = new ExpressionVisitor(getEnvironment(), getScope());
			e.accept(argumentsVisitor);
			Expression expr = argumentsVisitor.getExpression();
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
		TypeASTVisitor typeVisitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope());
		ExpressionVisitor lengthVisitor = new ExpressionVisitor(getEnvironment(), getScope());

		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();

		// currently we handle just single-dim and bi-dim arrays
		if (node.dimensions().size() > 2)
			throw new ParsingException("multi-dim array", ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Multi-dimensional arrays are not supported are not supported.",
					getSourceCodeLocation(node));

		// single-dimension arrays
		if (node.dimensions().size() == 1) {
			((ASTNode) node.dimensions().get(0)).accept(lengthVisitor);
			Expression length = lengthVisitor.getExpression();
			expression = new JavaNewArray(getScope().getCFG(), getSourceCodeLocation(node), length,
					new JavaReferenceType(type));
		}
		// bi-dimension arrays
		else if (node.dimensions().size() == 2) {
			((ASTNode) node.dimensions().get(0)).accept(lengthVisitor);
			int fstDim = Long.decode(((NumberLiteral) node.dimensions().get(0)).getToken()).intValue();
			ExpressionVisitor sndDimVisitor = new ExpressionVisitor(getEnvironment(), getScope());
			((ASTNode) node.dimensions().get(1)).accept(sndDimVisitor);
			List<Expression> parameters = new ArrayList<>();
			for (int i = 0; i < fstDim; i++) {
				Expression expr = new JavaNewArray(getScope().getCFG(),
						getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation(),
						sndDimVisitor.getExpression(), (JavaReferenceType) type.asArrayType().getInnerType());
				parameters.add(expr);
			}

			expression = new JavaNewArrayWithInitializer(getScope().getCFG(), getSourceCodeLocation(node),
					parameters.toArray(new Expression[0]), new JavaReferenceType(type));
		} else {
			ArrayInitializer initializer = node.getInitializer();

			// initializer.expressions();
			List<Expression> parameters = new ArrayList<>();

			for (Object args : initializer.expressions()) {
				ASTNode e = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(getEnvironment(), getScope());
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				parameters.add(expr);
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
		TypeASTVisitor visitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope());
		((FieldDeclaration) decl.getParent()).getType().accept(visitor);
		return visitor.getType();
	}

	@Override
	public boolean visit(
			Assignment node) {
		Assignment.Operator operator = node.getOperator();
		ExpressionVisitor leftVisitor = new ExpressionVisitor(getEnvironment(), getScope());
		ExpressionVisitor rightVisitor = new ExpressionVisitor(getEnvironment(), getScope());
		node.getLeftHandSide().accept(leftVisitor);
		node.getRightHandSide().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Expression right = rightVisitor.getExpression();
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
		ExpressionVisitor rightVisitor = new ExpressionVisitor(getEnvironment(), getScope());
		TypeASTVisitor leftVisitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope());
		node.getType().accept(leftVisitor);
		node.getExpression().accept(rightVisitor);
		Expression left = rightVisitor.getExpression();
		Type right = leftVisitor.getType();
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
		TypeASTVisitor typeVisitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope());
		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();

		if (!(type instanceof JavaClassType))
			throw new ParsingException("arguments-constructor",
					ParsingException.Type.UNSUPPORTED_STATEMENT,
					"A ClassInstanceCreation Type should be a JavaClassType; got: " + type.getClass().getName(),
					getSourceCodeLocation(node));

		List<Expression> parameters = new LinkedList<>();

		if (node.getExpression() != null) {
			// nested class creation, just pass the expression as first param
			ExpressionVisitor argumentsVisitor = new ExpressionVisitor(getEnvironment(), getScope());
			node.getExpression().accept(argumentsVisitor);
			Expression expr = argumentsVisitor.getExpression();
			parameters.add(expr);
		}

		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(getEnvironment(), getScope());
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				parameters.add(expr);
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

		ExpressionVisitor conditionVisitor = new ExpressionVisitor(getEnvironment(), getScope());
		node.getExpression().accept(conditionVisitor);
		Expression conditionExpr = conditionVisitor.getExpression();
		if (conditionExpr == null) {
			throw new ParsingException("conditional-expression",
					ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
					"The condition is missing.",
					getSourceCodeLocation(node));
		}

		ExpressionVisitor thenExprVisitor = new ExpressionVisitor(getEnvironment(), getScope());
		node.getThenExpression().accept(thenExprVisitor);
		Expression thenExpr = thenExprVisitor.getExpression();
		if (thenExpr == null)
			throw new ParsingException("conditional-expression",
					ParsingException.Type.MISSING_EXPECTED_EXPRESSION,
					"The then expression is missing.",
					getSourceCodeLocation(node));

		ExpressionVisitor elseExprVisitor = new ExpressionVisitor(getEnvironment(), getScope());
		node.getElseExpression().accept(elseExprVisitor);
		Expression elseExpr = elseExprVisitor.getExpression();
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
		ExpressionVisitor visitor = new ExpressionVisitor(getEnvironment(), getScope());
		node.getExpression().accept(visitor);
		Expression expr = visitor.getExpression();
		expression = new JavaAccessInstanceGlobal(getScope().getCFG(),
				getSourceCodeLocationManager(node.getExpression(), true).nextColumn(), expr,
				node.getName().getIdentifier());
		return false;
	}

	@Override
	public boolean visit(
			InfixExpression node) {
		InfixExpression.Operator operator = node.getOperator();
		ExpressionVisitor leftVisitor = new ExpressionVisitor(getEnvironment(), getScope());
		ExpressionVisitor rightVisitor = new ExpressionVisitor(getEnvironment(), getScope());
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
			ExpressionVisitor extendedOperandVisitor = new ExpressionVisitor(getEnvironment(), getScope());
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
					location) -> new Multiplication(getScope().getCFG(), location, first, second));
			break;
		case "/":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaDivision(getScope().getCFG(), location, first, second));
			break;
		case "%":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new Modulo(getScope().getCFG(), location, first, second));
			break;
		case "+":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaAddition(getScope().getCFG(), location, first, second));
			break;
		case "-":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaSubtraction(getScope().getCFG(), location, first, second));
			break;
		case ">>":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaShiftRight(getScope().getCFG(), location, first, second));
			break;
		case "<<":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaShiftLeft(getScope().getCFG(), location, first, second));
			break;
		case ">>>":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaUnsignedShiftRight(getScope().getCFG(), location, first, second));
			break;
		case "<":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new LessThan(getScope().getCFG(), location, first, second));
			break;
		case ">":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new GreaterThan(getScope().getCFG(), location, first, second));
			break;
		case "<=":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new LessOrEqual(getScope().getCFG(), location, first, second));
			break;
		case ">=":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new GreaterOrEqual(getScope().getCFG(), location, first, second));
			break;
		case "==":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new Equal(getScope().getCFG(), location, first, second));
			break;
		case "!=":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new NotEqual(getScope().getCFG(), location, first, second));
			break;
		case "&":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaBitwiseAnd(getScope().getCFG(), location, first, second));
			break;
		case "^":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaBitwiseExclusiveOr(getScope().getCFG(), location, first, second));
			break;
		case "|":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaBitwiseOr(getScope().getCFG(), location, first, second));
			break;
		case "&&":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaAnd(getScope().getCFG(), location, first, second));
			break;
		case "||":
			expression = buildExpression(operands, jdtOperands, (
					first,
					second,
					location) -> new JavaOr(getScope().getCFG(), location, first, second));
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
		ExpressionVisitor leftVisitor = new ExpressionVisitor(getEnvironment(), getScope());
		TypeASTVisitor rightVisitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope());
		node.getLeftOperand().accept(leftVisitor);
		node.getRightOperand().accept(rightVisitor);
		Expression left = leftVisitor.getExpression();
		Type right = rightVisitor.getType();

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
		List<Expression> parameters = new ArrayList<>();
		String methodName = node.getName().toString();
		ClassUnit classUnit = (ClassUnit) getScope().getCFG().getUnit();

		boolean isInstance = false;
		String name = null;
		// we do not have a receiver
		if (node.getExpression() == null) {
			isInstance = !classUnit.getInstanceCodeMembersByName(methodName, true).isEmpty();

			// if instance, we add this as parameter
			if (isInstance)
				parameters.add(new VariableRef(getScope().getCFG(), getSourceCodeLocation(node), "this",
						new JavaReferenceType(JavaClassType.lookup(classUnit.getName()))));
		} else {
			// this might be a fqn instead
			name = getScope().getParentScope().getUnitScope().getExplicitImports().get(node.getExpression().toString());
			if (name == null) {
				name = node.getExpression().toString();
				Unit resolved = TypeASTVisitor.getUnit(name, getProgram(), getScope().getParentScope().getUnitScope());
				if (resolved != null)
					name = resolved.getName();
			}
			if (LibrarySpecificationProvider.isLibraryAvailable(name))
				LibrarySpecificationProvider.importClass(getProgram(), name);

			Expression rec = null;
			try {
				ExpressionVisitor receiver = new ExpressionVisitor(getEnvironment(), getScope());
				node.getExpression().accept(receiver);
				rec = receiver.getExpression();
			} catch (ParsingException e) {
				if (!e.getName().equals("missing-variable"))
					throw e;
				// missing var is fine: we will use the fqn
			}

			if (rec != null) {
				// if rec is a VariableRef, we need to check if the code member
				// of the compilation unit of the variable is instance or not.
				if (rec instanceof VariableRef) {
					if (rec.getStaticType() instanceof JavaReferenceType refType) {
						if (refType.getInnerType() instanceof UnitType unitType) {
							if (!unitType.getUnit().getInstanceCodeMembersByName(methodName, true).isEmpty()) {
								parameters.add(rec);
								isInstance = true;
							} else {
								name = unitType.toString();
							}
						}
					}
				} else if (rec instanceof JavaAccessGlobal accessGlobal) {
					if (accessGlobal.getTarget().getStaticType() instanceof JavaReferenceType refType) {
						if (refType.getInnerType() instanceof JavaClassType classType) {
							if (!classType.getUnit().getInstanceCodeMembersByName(methodName, true).isEmpty()) {
								parameters.add(rec);
								isInstance = true;
							} else {
								name = classType.toString();
							}
						}

					}
				} else {
					// if the receiver is not a variable ref, nor an
					// accessGlobal, we assume that the call is an instance.
					// However, if the receiver is something like
					// foo().foo2().foo3(), since we don't know the return type
					// of foo2()
					// we should try to resolve this call as both instance and
					// static.
					parameters.add(rec);
					isInstance = true;
				}
			}
		}

		if (!node.typeArguments().isEmpty())
			throw new ParsingException("method-invocation",
					ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Method Invocation expressions with type arguments are not supported.",
					getSourceCodeLocation(node));

		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(getEnvironment(), getScope());
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				parameters.add(expr);
			}
		}

		if (isInstance)
			expression = new JavaUnresolvedCall(
					getScope().getCFG(),
					getSourceCodeLocationManager(node.getName()).nextColumn(),
					Call.CallType.INSTANCE,
					null,
					node.getName().toString(),
					parameters.toArray(new Expression[0]));
		else
			expression = new JavaUnresolvedStaticCall(
					getScope().getCFG(),
					getSourceCodeLocationManager(node.getName()).nextColumn(),
					name == null ? classUnit.getName() : name,
					node.getName().toString(),
					parameters.toArray(new Expression[0]));

		return false;
	}

	@Override
	public boolean visit(
			QualifiedName node) {
		/*
		 * From the javadoc of FieldAccess: An expression like "foo.this" can
		 * only be represented as a this expression (ThisExpression) containing
		 * a simple name. "this" is a keyword, and therefore invalid as an
		 * identifier. An expression like "this.foo" can only be represented as
		 * a field access expression (FieldAccess) containing a this expression
		 * and a simple name. Again, this is because "this" is a keyword, and
		 * therefore invalid as an identifier. An expression with "super" can
		 * only be represented as a super field access expression
		 * (SuperFieldAccess). "super" is a also keyword, and therefore invalid
		 * as an identifier. An expression like "foo.bar" can be represented
		 * either as a qualified name (QualifiedName) or as a field access
		 * expression (FieldAccess) containing simple names. Either is
		 * acceptable, and there is no way to choose between them without
		 * information about what the names resolve to (ASTParser may return
		 * either). Other expressions ending in an identifier, such as
		 * "foo().bar" can only be represented as field access expressions
		 * (FieldAccess).
		 */

		// based on tests, field accesses have precedence over fqns
		// the resolution also searches in enclosing instances
		try {
			Expression fa = solveAsFieldAccess(node);
			if (fa != null) {
				expression = fa;
				return false;
			}
		} catch (ParsingException e) {
			if (!e.getName().equals("missing-global"))
				throw e;
		}

		// we were not able to solve it as a field access,
		// so we might have:
		// - some.qualified.or.not.class.name.StaticField
		// - some.qualified.or.not.class.name.StaticField.SomeOtherField
		// - some.qualified.or.not.class.name (?)

		// we try to find a unit by starting at the first token
		// ("some") and going right ("some.qualified", "some.qualified.or", ...)
		// until we find a unit or we exhaust all the possibilities
		List<SimpleName> fields = new LinkedList<>();
		SimpleName firstField = null;
		Name current = node;
		// each element in names holds:
		// - the fqn to try
		// - the static field to access in the fqn
		// - the remaining fields to access from the static field
		LIFOWorkingSet<Triple<String, SimpleName, List<SimpleName>>> names = new LIFOWorkingSet<>();
		while (current != null) {
			names.push(Triple.of(current.toString(), firstField, new LinkedList<>(fields)));
			if (current instanceof QualifiedName) {
				if (firstField != null)
					fields.addFirst(firstField);
				firstField = ((QualifiedName) current).getName();
				current = ((QualifiedName) current).getQualifier();
			} else
				current = null;
		}

		Unit candidate = null;
		while (!names.isEmpty()) {
			Triple<String, SimpleName, List<SimpleName>> tentative = names.pop();
			candidate = TypeASTVisitor.getUnit(tentative.getLeft(), getProgram(),
					getScope().getParentScope().unitScope());
			if (candidate == null)
				continue;

			// unit found, search for the global
			if (tentative.getMiddle() == null)
				// if we got a unit with no field to access, we do nothing:
				// the caller has to handle the fqn as a type (eg, in a static
				// call)
				return false;
			Global global = getParserContext().getGlobal(candidate, tentative.getMiddle().getIdentifier(), false);
			if (global == null)
				// we got the unit, but we have to access the global before
				// returning
				// if we cannot find it, we try the next candidate
				continue;

			Expression access = new JavaAccessGlobal(
					getScope().getCFG(),
					getSourceCodeLocationManager(node.getQualifier(), true).getCurrentLocation(),
					candidate,
					global);

			if (tentative.getRight().isEmpty()) {
				// no more fields to access, we are done
				expression = access;
				return false;
			}

			// we have more fields to access
			for (SimpleName f : tentative.getRight()) {
				try {
					access = new JavaAccessInstanceGlobal(getScope().getCFG(),
							getSourceCodeLocationManager(f).nextColumn(),
							access,
							f.getIdentifier());
				} catch (ParsingException e) {
					if (!e.getName().equals("missing-global"))
						throw e;
					// no global found, we stop here and we try the next
					// candidate
					break;
				}
			}
		}

		// we did not find a field access or a fqn matching the node
		throw new ParsingException("missing-type",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Missing unit " + node,
				getSourceCodeLocation(node));
	}

	private Expression solveAsFieldAccess(
			QualifiedName node) {
		// we try to resolve node as a field access (y.x or y.x.z.w)
		String targetName = node.getName().getIdentifier();
		Name qualifier = node.getQualifier();

		Expression receiver = null;
		if (qualifier instanceof SimpleName) {
			ExpressionVisitor visitor = new ExpressionVisitor(getEnvironment(), getScope());
			try {
				// this searches also in enclosing instances
				((SimpleName) qualifier).accept(visitor);
				receiver = visitor.getExpression();
			} catch (ParsingException e) {
				if (!e.getName().equals("missing-variable"))
					throw e;
				receiver = null;
			}
		} else if (qualifier instanceof QualifiedName)
			receiver = solveAsFieldAccess((QualifiedName) qualifier);

		if (receiver == null)
			return null;

		if (receiver.getStaticType().isReferenceType()
				&& receiver.getStaticType().asReferenceType().getInnerType().isUnitType()) {
			// this might be a static field access
			// starting from an object
			UnitType unitType = receiver.getStaticType().asReferenceType().getInnerType().asUnitType();
			Global global = getParserContext().getGlobal(unitType.getUnit(), targetName, true);
			if (global != null && !global.isInstance())
				return new JavaAccessGlobal(
						getScope().getCFG(),
						getSourceCodeLocationManager(node.getQualifier(), true).getCurrentLocation(),
						unitType.getUnit(),
						global);
		}

		return new JavaAccessInstanceGlobal(getScope().getCFG(),
				getSourceCodeLocationManager(node.getQualifier(), true).nextColumn(),
				receiver,
				targetName);
	}

	@Override
	public boolean visit(
			SimpleName node) {
		String identifier = node.getIdentifier();
		if (getScope().getTracker() != null && getScope().getTracker().hasVariable(identifier)) {
			expression = new VariableRef(
					getScope().getCFG(),
					getSourceCodeLocation(node),
					identifier,
					getParserContext().getVariableStaticType(getScope().getCFG(), new VariableInfo(identifier,
							getScope().getTracker() != null ? getScope().getTracker().getLocalVariable(identifier)
									: null)));
			return false;
		}

		// if the tracker does not have information about the actual
		// variable, this might be a global
		Global global = getParserContext().getGlobal(getScope().getCFG().getDescriptor().getUnit(), identifier, true);
		SyntheticCodeLocationManager synth = getParserContext().getCurrentSyntheticCodeLocationManager(getSource());
		if (global != null) {
			if (global.isInstance()) {
				JavaReferenceType type = null;
				if (getScope().getCFG().getUnit() instanceof ClassUnit)
					type = JavaClassType.lookup(getScope().getCFG().getUnit().getName()).getReference();
				else
					type = JavaInterfaceType.lookup(getScope().getCFG().getUnit().getName()).getReference();

				expression = new JavaAccessInstanceGlobal(getScope().getCFG(),
						getSourceCodeLocationManager(node).getCurrentLocation(),
						new VariableRef(getScope().getCFG(), synth.nextLocation(), "this", type),
						identifier);
			} else
				expression = new JavaAccessGlobal(
						getScope().getCFG(),
						getSourceCodeLocationManager(node).getCurrentLocation(),
						getScope().getCFG().getUnit(),
						global);

			return false;
		}

		// it might also be an instance global from an enclosing instance
		ClassScope cursor = getScope().getParentScope().getParentScope();
		// ClassASTVisitor cursor = container instanceof ClassASTVisitor ?
		// (ClassASTVisitor) container : null;
		JavaReferenceType type = null;
		if (getScope().getCFG().getUnit() instanceof ClassUnit)
			type = JavaClassType.lookup(getScope().getCFG().getUnit().getName()).getReference();
		else
			type = JavaInterfaceType.lookup(getScope().getCFG().getUnit().getName()).getReference();
		// we accumulate this.$enclosing.$enclosing... until we find the global
		// or we raise an exception
		expression = new VariableRef(getScope().getCFG(), synth.nextLocation(), "this", type);
		while (cursor != null) {
			CompilationUnit encl = cursor.getLisaClassUnit();
			// JavaClassType encl = cursor.enclosingType;
			expression = new JavaAccessInstanceGlobal(getScope().getCFG(), synth.nextLocation(), expression,
					"$enclosing");
			global = getParserContext().getGlobal(encl, identifier, true);
			if (global != null) {
				if (global.isInstance()) {
					expression = new JavaAccessInstanceGlobal(
							getScope().getCFG(),
							getSourceCodeLocation(node),
							expression,
							identifier);
				} else
					expression = new JavaAccessGlobal(
							getScope().getCFG(),
							getSourceCodeLocation(node),
							encl,
							global);

				return false;
			} else
				cursor = cursor.getParentScope();
		}

		throw new ParsingException("missing-variable",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Variable " + identifier + " not defined before its use",
				getSourceCodeLocation(node));
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
		ExpressionVisitor visitor = new ExpressionVisitor(getEnvironment(), getScope());
		node.getExpression().accept(visitor);
		expression = visitor.getExpression();
		return false;
	}

	@Override
	public boolean visit(
			PostfixExpression node) {
		ExpressionVisitor sev = new ExpressionVisitor(getEnvironment(), getScope());
		node.getOperand().accept(sev);
		Expression expr = sev.getExpression();
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
		ExpressionVisitor sev = new ExpressionVisitor(getEnvironment(), getScope());
		node.getOperand().accept(sev);
		Expression expr = sev.getExpression();
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
		ClassUnit superClass = (ClassUnit) getScope().getCFG().getUnit();
		JavaClassType superType = JavaClassType.lookup(superClass.getName());

		// craft the call to superclass
		List<Expression> parameters = new ArrayList<>();
		parameters.add(new VariableRef(getScope().getCFG(), getSourceCodeLocation(node), "this",
				new JavaReferenceType(superType)));

		for (Object args : node.arguments()) {
			ASTNode e = (ASTNode) args;
			ExpressionVisitor argVisitor = new ExpressionVisitor(getEnvironment(), getScope());
			e.accept(argVisitor);
			Expression expr = argVisitor.getExpression();
			parameters.add(expr);
		}

		expression = new JavaUnresolvedSuperCall(getScope().getCFG(),
				getSourceCodeLocationManager(node.getName()).nextColumn(),
				Call.CallType.INSTANCE, superClass.getName(), node.getName().toString(),
				parameters.toArray(new Expression[0]));
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

			TypeASTVisitor visitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().unitScope());
			node.getQualifier().accept(visitor);
			it.unive.lisa.type.Type enclosing = visitor.getType();
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
		TypeASTVisitor visitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope());
		node.getType().accept(visitor);

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
		TypeASTVisitor visitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope());
		node.getType().accept(visitor);
		it.unive.lisa.type.Type varType = visitor.getType();
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
			ExpressionVisitor exprVisitor = new ExpressionVisitor(getEnvironment(), getScope());
			expr.accept(exprVisitor);
			Expression initializer = exprVisitor.getExpression();
			expression = new JavaAssignment(getScope().getCFG(),
					getSourceCodeLocationManager(fragment.getName(), true).getCurrentLocation(), ref, initializer);
		}
		return false;
	}

	public Expression getExpression() {
		return expression;
	}

}
package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.program.cfg.expression.*;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.comparison.*;
import it.unive.lisa.program.cfg.statement.global.AccessInstanceGlobal;
import it.unive.lisa.program.cfg.statement.literal.FalseLiteral;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.program.cfg.statement.logic.And;
import it.unive.lisa.program.cfg.statement.logic.Not;
import it.unive.lisa.program.cfg.statement.logic.Or;
import it.unive.lisa.program.cfg.statement.numeric.*;
import it.unive.lisa.symbolic.value.operator.BitwiseOperator;
import it.unive.lisa.symbolic.value.operator.LogicalOperator;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseAnd;
import org.eclipse.jdt.core.dom.*;

import javax.lang.model.type.TypeVisitor;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jdt.core.dom.Assignment.Operator.*;

public class ExpressionVisitor extends JavaASTVisitor {
    private CFG cfg;
    private Expression expression;

    public ExpressionVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg) {
        super(parserContext, source, compilationUnit);
        this.cfg = cfg;
    }

    /*@Override
    public boolean visit(Annotation node) {
        throw new RuntimeException(new UnsupportedStatementException("Annotation expression not supported"));
        //return false;
    }*/

    @Override
    public boolean visit(ArrayAccess node) {
        parserContext.addException(new ParsingException("array-access", ParsingException.Type.UNSUPPORTED_STATEMENT, "Array Accesses are not supported.", getSourceCodeLocation(node)));
        return false;
    }

    @Override
    public boolean visit(ArrayCreation node) {
        throw new RuntimeException(new UnsupportedStatementException("Array Creation expressions are not supported"));
        //return false;
    }

    @Override
    public boolean visit(ArrayInitializer node) {
        throw new RuntimeException(new UnsupportedStatementException("Array Initializer expressions are not supported"));
        //return false;
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
                expression = new it.unive.lisa.program.cfg.statement.Assignment(cfg, getSourceCodeLocation(node), left, right);
                break;
            case "+=":
            case "-=":
            case "*=":
            case "/=":
            case "&=":
            case "|=":
            case "^=":
            case "%=":
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
        parserContext.addException(
                new ParsingException("cast-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Cast Expressions are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        parserContext.addException(
                new ParsingException("character-literal", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Character Literals are not supported.",
                        getSourceCodeLocation(node))
        );
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
                type,
                parameters.toArray(new Expression[0]));
        /*parserContext.addException(
                new ParsingException("class-instance-creation", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Classes Instance Creation are not supported.",
                        getSourceCodeLocation(node))
        );*/
        return false;
    }

    @Override
    public boolean visit(ConditionalExpression node) {
        parserContext.addException(
                new ParsingException("conditional-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Conditional Expressions are not supported.",
                        getSourceCodeLocation(node))
        );
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
            expression = new AccessInstanceGlobal(cfg, getSourceCodeLocation(node), expr, node.getName().getIdentifier());
        }
        /*parserContext.addException(
                new ParsingException("field-access", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Field Access expressions are not supported.",
                        getSourceCodeLocation(node))
        );*/
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
        switch (operator.toString()) {
            case "*":
                expression = new Multiplication(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
                break;
            case "/":
                expression = new Division(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
                break;
            case "%":
                expression = new Modulo(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
                break;
            case "+":
                expression = new Addition(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
                break;
            case "-":
                expression = new Subtraction(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
                break;
            case "<<":
            case ">>":
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
                expression = new Equal(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
                break;
            case "!=":
                expression = new NotEqual(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
                break;
            case "^":
            case "&":
            case "|":
                parserContext.addException(
                        new ParsingException("infix-operator", ParsingException.Type.UNSUPPORTED_STATEMENT,
                                "The '" + operator + "' infix operator is not supported.",
                                getSourceCodeLocation(node))
                );
                break;
            case "&&":
                expression = new And(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
                break;
            case "||":
                expression = new Or(cfg, getSourceCodeLocation(node), leftVisitor.getExpression(), rightVisitor.getExpression());
                break;
            default:
                throw new RuntimeException(new UnsupportedStatementException("Unknown infix operator: " + operator));
        }
        return false;
    }

    @Override
    public boolean visit(InstanceofExpression node) {
        parserContext.addException(
                new ParsingException("instanceof-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Instanceof expressions are not supported.",
                        getSourceCodeLocation(node))
        );
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
        //TODO: REASON ABOUT INSTANCE / STATIC B.m() -> static, b.m() -> NOT STATIC, m() -> both satic and non-static
        if (instance) {
            expression = new UnresolvedCall(cfg, getSourceCodeLocation(node), Call.CallType.UNKNOWN, null,node.getName().toString(), parameters.toArray(new Expression[0]));
        }
        return false;
    }

    /*@Override
    public boolean visit(MethodReference node) {
        throw new RuntimeException(new UnsupportedStatementException("Method Reference expression not supported"));
        //return false;
    }*/

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
        /*parserContext.addException(
                new ParsingException("qualified-name", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Qualified Name expressions are not supported.",
                        getSourceCodeLocation(node))
        );*/
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        String identifier = node.getIdentifier();
        expression = new VariableRef(cfg, getSourceCodeLocation(node), identifier, parserContext.getVariableStaticType(cfg, identifier));
        return false;
    }

    @Override
    public boolean visit(NullLiteral node) {
        expression = new it.unive.lisa.program.cfg.statement.literal.NullLiteral(cfg, getSourceCodeLocation(node));
        return false;
    }

    /*@Override
    public boolean visit(Pattern node) {
        throw new RuntimeException(new UnsupportedStatementException("Pattern expression not supported"));
        //return false;
    }*/

    @Override
    public boolean visit(ParenthesizedExpression node) {
        parserContext.addException(
                new ParsingException("parenthesized-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Parenthesized Expressions are not supported.",
                        getSourceCodeLocation(node))
        );
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
        String literal = node.getEscapedValue();
        expression = new it.unive.lisa.program.cfg.statement.literal.StringLiteral(this.cfg, getSourceCodeLocation(node.getParent()), literal);
        return false;
    }
    @Override
    public boolean visit(NumberLiteral node) {
        String token = node.getToken();
        if ((token.endsWith("f") || token.endsWith("F")) && !token.startsWith("0x")) {
            // FlOAT
            expression = new it.unive.lisa.program.cfg.statement.literal.Float32Literal(this.cfg, getSourceCodeLocation(node), Float.parseFloat(token));
            return false;
        }
        if (token.contains(".") || ((token.contains("e") || token.contains("E") || token.endsWith("d") || token.endsWith("D")) && !token.startsWith("0x"))) {
            // DOUBLE
            expression = new it.unive.lisa.program.cfg.statement.literal.Float64Literal(this.cfg, getSourceCodeLocation(node), Double.parseDouble(token));
            return false;
        }
        if (token.endsWith("l") || token.endsWith("L")) {
            expression = new it.unive.lisa.program.cfg.statement.literal.Int64Literal(this.cfg, getSourceCodeLocation(node), Long.parseLong(token));
            return false;
        }
        try {
            long value = Long.decode(token); // handles 0x, 0b, octal, decimal
            if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                expression = new it.unive.lisa.program.cfg.statement.literal.Int32Literal(this.cfg, getSourceCodeLocation(node),(int)value);
            } else {
                expression = new it.unive.lisa.program.cfg.statement.literal.Int64Literal(this.cfg, getSourceCodeLocation(node), value);
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not parse " + token + ": not a valid Number Literal", e );
        }
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
        parserContext.addException(
                new ParsingException("variable-declaration-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Variable Declaration Expressions are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }


    public Expression getExpression() {
        return expression;
    }


}

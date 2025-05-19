package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class StatementASTVisitor extends JavaASTVisitor {
    private CFG cfg;
    private it.unive.lisa.program.cfg.statement.Statement first;
    private it.unive.lisa.program.cfg.statement.Statement last;
    private NodeList<CFG, it.unive.lisa.program.cfg.statement.Statement, Edge> block = new NodeList<>(new SequentialEdge());

    public StatementASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg) {
        super(parserContext, source, compilationUnit);
        this.cfg = cfg;
    }

    public Statement getFirst() {
        return first;
    }

    public Statement getLast() {
        return last;
    }

    public NodeList<CFG, Statement, Edge> getBlock() {
        return block;
    }

    @Override
    public boolean visit(AssertStatement node) {
        parserContext.addException(
                new ParsingException("assert-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Assert statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(Block node) {
        block = new NodeList<>(new SequentialEdge());

        for (Object o : node.statements()) {
            StatementASTVisitor statementASTVisitor = new StatementASTVisitor(parserContext, source, compilationUnit, cfg);
            ((org.eclipse.jdt.core.dom.Statement) o).accept(statementASTVisitor);
            if (statementASTVisitor.getBlock().getNodes().isEmpty()) {
                // A PARSING ERROR HAPPENS. IGNORE THAT.
                return false;
            }
            block.mergeWith(statementASTVisitor.getBlock());
            if (first == null) {
                first = statementASTVisitor.getFirst();
            }
            if (last != null) {
                block.addEdge(new SequentialEdge(last, statementASTVisitor.getLast()));
            }
            last = statementASTVisitor.getLast();
        }
        return false;
    }

    @Override
    public boolean visit(BreakStatement node) {
        parserContext.addException(
                new ParsingException("break-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Break statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        if (!node.typeArguments().isEmpty()) {
            parserContext.addException(
                    new ParsingException("constructor-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
                            "Constructor invocation statements with type arguments are not supported.",
                            getSourceCodeLocation(node))
            );
        }


        // get the type from the descriptor
        Expression thisExpression = new VariableRef(cfg, getSourceCodeLocation(node), "this");
        List<Expression> parameters = new ArrayList<>();
        parameters.add(thisExpression);

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
        first = new UnresolvedCall(cfg, getSourceCodeLocation(node), Call.CallType.INSTANCE, null,this.cfg.getDescriptor().getName(), parameters.toArray(new Expression[0]));
        last = first;
        block.addNode(first);

        return false;
    }

    @Override
    public boolean visit(ContinueStatement node) {
        parserContext.addException(
                new ParsingException("continue-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Continue statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(DoStatement node) {
        parserContext.addException(
                new ParsingException("do-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Do-while statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(EmptyStatement node) {
        parserContext.addException(
                new ParsingException("empty-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Empty statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        parserContext.addException(
                new ParsingException("enhanced-for-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Enhanced for-each loops are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        block = new NodeList<>(new SequentialEdge());
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(parserContext, this.source, this.compilationUnit, this.cfg);
        node.getExpression().accept(expressionVisitor);
        first = expressionVisitor.getExpression();
        if (first == null) {
            // PARSING ERROR. IGNORE
            return false;
        }
        first = expressionVisitor.getExpression();
        last = first;
        block.addNode(first);
        return false;
    }

    @Override
    public boolean visit(ForStatement node) {
        parserContext.addException(
                new ParsingException("for-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "For loops are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(IfStatement node) {
        block = new NodeList<>(new SequentialEdge());
        ExpressionVisitor conditionVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        node.getExpression().accept(conditionVisitor);

        StatementASTVisitor trueVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        node.getThenStatement().accept(trueVisitor);

        StatementASTVisitor falseVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        if (node.getElseStatement() != null) {
            node.getElseStatement().accept(falseVisitor);
        }
        if (trueVisitor.getBlock().getNodes().isEmpty() || conditionVisitor.getExpression() == null || (node.getElseStatement() != null && falseVisitor.getBlock().getNodes().isEmpty())) {
            return false;
        }
        first = conditionVisitor.getExpression();

        block.addNode(conditionVisitor.getExpression());

        NodeList<CFG, Statement, Edge> trueBlock = trueVisitor.getBlock();
        block.mergeWith(trueBlock);
        block.addEdge(new TrueEdge(first, trueVisitor.getFirst()));
        if (node.getElseStatement() != null) {
            NodeList<CFG, Statement, Edge> falseBlock = falseVisitor.getBlock();
            block.mergeWith(falseBlock);
            block.addEdge(new FalseEdge(first, falseVisitor.getFirst()));
        }
        Statement noop = new NoOp(cfg, conditionVisitor.getExpression().getLocation());
        block.addNode(noop);
        block.addEdge(new SequentialEdge(trueVisitor.getLast(), noop));
        if (node.getElseStatement() != null) {
            block.addEdge(new SequentialEdge(falseVisitor.getLast(), noop));
        }
        last = noop;
        return false;
    }

    @Override
    public boolean visit(LabeledStatement node) {
        parserContext.addException(
                new ParsingException("labeled-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Labeled statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(ReturnStatement node) {
        ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        if (node.getExpression() != null) {
            node.getExpression().accept(visitor);
        }
        Expression e = visitor.getExpression();
        Statement ret;
        if (e == null) {
            ret = new Ret(cfg, getSourceCodeLocation(node));
        } else {
            ret = new Return(cfg, getSourceCodeLocation(node), e);
        }
        first = ret;
        last = ret;
        block.addNode(ret);
        /*parserContext.addException(
                new ParsingException("return-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Return statements are not supported.",
                        getSourceCodeLocation(node))
        );*/
        return false;
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        parserContext.addException(
                new ParsingException("super-constructor-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Super constructor invocations are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(SwitchCase node) {
        parserContext.addException(
                new ParsingException("switch-case", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Switch cases are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(SwitchStatement node) {
        parserContext.addException(
                new ParsingException("switch-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Switch statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(SynchronizedStatement node) {
        parserContext.addException(
                new ParsingException("synchronized-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Synchronized statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(ThrowStatement node) {
        parserContext.addException(
                new ParsingException("throw-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Throw statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(TryStatement node) {
        parserContext.addException(
                new ParsingException("try-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Try-catch-finally blocks are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(TypeDeclarationStatement node) {
        parserContext.addException(
                new ParsingException("type-declaration-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Type declaration statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        block = new NodeList<>(new SequentialEdge());
        TypeASTVisitor visitor = new TypeASTVisitor(this.parserContext, source, compilationUnit);
        node.getType().accept(visitor);
        Type variableType = visitor.getType();
        for (Object f : node.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
            String variableName = fragment.getName().getIdentifier();
            SourceCodeLocation loc = getSourceCodeLocation(fragment);
            VariableRef ref = new VariableRef(cfg,
                    getSourceCodeLocation(fragment),
                    variableName, variableType);
            Expression initializer;
            if(fragment.getInitializer() == null) {
                if (variableType == Int32Type.INSTANCE) {
                    initializer = new Int32Literal(cfg, loc, (int)JavaTypeSystem.getDefaultValue(variableType));
                } else {
                    initializer = new NullLiteral(cfg, loc);
                }
            } else {
                org.eclipse.jdt.core.dom.Expression expr = fragment.getInitializer();
                ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg);
                expr.accept(exprVisitor);
                initializer = exprVisitor.getExpression();
                if (initializer == null) {
                    initializer = new NullLiteral(cfg, loc);
                }
            }
            Assignment assignment = new Assignment(cfg, loc, ref, initializer);
            block.addNode(assignment);
            if (first == null) {
                first = assignment;
            } else {
                block.addEdge(new SequentialEdge(first, assignment));
            }
            //cfg.getNodeList().mergeWith(block);
            //cfg.addNode(ref);
            last = assignment;
            //fragment.getInitializer()
        }
        return false;
    }

    @Override
    public boolean visit(WhileStatement node) {
        NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

        ExpressionVisitor condition = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        node.getExpression().accept(condition);
        Expression expression = condition.getExpression();

        this.first = expression;
        block.addNode(expression);

        StatementASTVisitor loopBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        node.getBody().accept(loopBody);

        if (loopBody.first == null || loopBody.last == null) {
            // Parsing error. Skipping...
            return false;
        }

        block.mergeWith(loopBody.getBlock());
        block.addEdge(new TrueEdge(expression, loopBody.getFirst()));
        block.addEdge(new SequentialEdge(loopBody.getLast(), expression));

        Statement noop = new NoOp(this.cfg, expression.getLocation());
        block.addNode(noop);
        block.addEdge(new FalseEdge(expression, noop));

        this.last = noop;
        this.block = block;
        return false;
    }
}

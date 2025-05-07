package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import org.eclipse.jdt.core.dom.*;

import static org.eclipse.jdt.core.dom.Assignment.Operator.ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.PLUS_ASSIGN;

public class StatementExpressionVisitor extends JavaASTVisitor {
    private CFG cfg;
    private it.unive.lisa.program.cfg.statement.Expression expression;

    public StatementExpressionVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg) {
        super(parserContext, source, compilationUnit);
        this.cfg = cfg;
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        String className = node.getName().toString();
        parserContext.addException(
                new ParsingException("class-instance-creation", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Class instance creation expressions are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        parserContext.addException(
                new ParsingException("method-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Method invocation expressions are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(PostfixExpression node) {
        parserContext.addException(
                new ParsingException("postfix-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Postfix expressions are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(PrefixExpression node) {
        parserContext.addException(
                new ParsingException("prefix-expression", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Prefix expressions are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        parserContext.addException(
                new ParsingException("super-method-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Super method invocation expressions are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    public it.unive.lisa.program.cfg.statement.Expression getExpression() {
        return expression;
    }
}

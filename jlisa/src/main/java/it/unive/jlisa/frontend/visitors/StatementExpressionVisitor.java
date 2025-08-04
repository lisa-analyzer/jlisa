package it.unive.jlisa.frontend.visitors;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.lisa.program.cfg.CFG;

public class StatementExpressionVisitor extends JavaASTVisitor {
    private it.unive.lisa.program.cfg.statement.Expression expression;

    public StatementExpressionVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg) {
        super(parserContext, source, compilationUnit);
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
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

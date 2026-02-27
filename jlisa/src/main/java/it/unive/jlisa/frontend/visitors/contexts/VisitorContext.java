package it.unive.jlisa.frontend.visitors.contexts;

import it.unive.jlisa.frontend.ParserContext;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class VisitorContext {
    protected final ParserContext parserContext;
    protected final String source;
    protected final CompilationUnit compilationUnit;

    public VisitorContext(ParserContext parserContext, String source, CompilationUnit compilationUnit) {
        this.parserContext = parserContext;
        this.source = source;
        this.compilationUnit = compilationUnit;
    }

    public ParserContext getParserContext() {
        return parserContext;
    }

    public String getSource() {
        return source;
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }
}
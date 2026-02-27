package it.unive.jlisa.frontend.visitors.contexts;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.visitors.BaseUnitASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.Map;

public class UnitVisitorContext extends VisitorContext {
    protected final BaseUnitASTVisitor container;
    protected final Map<String, String> imports;
    protected final String pkg;

    public UnitVisitorContext(ParserContext parserContext, String source, CompilationUnit compilationUnit,
                              BaseUnitASTVisitor container, String pkg, Map<String, String> imports) {
        super(parserContext, source, compilationUnit);
        this.container = container;
        this.pkg = pkg;
        this.imports = imports;
    }
}
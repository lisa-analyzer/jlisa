package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public abstract class JavaASTVisitor extends ASTVisitor {
    protected String source;
    protected CompilationUnit compilationUnit;
    protected ParserContext parserContext;
    public JavaASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit) {
        this.parserContext = parserContext;
        this.source = source;
        this.compilationUnit = compilationUnit;
    }

    public SourceCodeLocation getSourceCodeLocation(ASTNode node) {
        int startPos = node.getStartPosition();
        return parserContext.getLocationManager(this.source, compilationUnit.getLineNumber(startPos), compilationUnit.getColumnNumber(startPos)).getCurrentLocation();
    }

    public SourceCodeLocationManager getSourceCodeLocationManager(ASTNode node) {
        int startPos = node.getStartPosition();
        return parserContext.getLocationManager(this.source, compilationUnit.getLineNumber(startPos), compilationUnit.getColumnNumber(startPos));
    }

    public SourceCodeLocationManager getSourceCodeLocationManager(ASTNode node, boolean end) {
        int startPos = node.getStartPosition();
        if (end) {
            return parserContext.getLocationManager(this.source, compilationUnit.getLineNumber(startPos), compilationUnit.getColumnNumber(startPos) + node.getLength());
        }
        return parserContext.getLocationManager(this.source, compilationUnit.getLineNumber(startPos), compilationUnit.getColumnNumber(startPos));
    }

    public Program getProgram() {
        return parserContext.getProgram();
    }

    public int getApiLevel() {
        return parserContext.getApiLevel();
    }

    public ParserContext getParserContext() {
        return parserContext;
    }
}

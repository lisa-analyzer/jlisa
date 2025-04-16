package it.unive.jlisa.frontend.visitors;

import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public abstract class JavaASTVisitor extends ASTVisitor {
    protected Program program;
    protected String source;
    protected int apiLevel;
    protected CompilationUnit compilationUnit;

    public JavaASTVisitor(Program program, String source, int apiLevel, CompilationUnit compilationUnit) {
        this.program = program;
        this.source = source;
        this.apiLevel = apiLevel;
        this.compilationUnit = compilationUnit;
    }

    public SourceCodeLocation getSourceCodeLocation(ASTNode node) {
        int startPos = node.getStartPosition();
        return new SourceCodeLocation(this.source, compilationUnit.getLineNumber(startPos), compilationUnit.getColumnNumber(startPos));
    }
}

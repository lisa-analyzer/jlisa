package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import org.eclipse.jdt.core.dom.*;

public class ClassASTVisitor extends JavaASTVisitor{

    public ClassASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit) {
        super(parserContext, source, compilationUnit);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (node.getSuperclassType() != null) {
            throw new RuntimeException(new UnsupportedStatementException("extends is not supported yet."));
        }
        if (!node.permittedTypes().isEmpty()) {
            throw new RuntimeException(new UnsupportedStatementException("permits is not supported yet."));
        }
        ClassUnit cUnit = (ClassUnit) getProgram().getUnit(node.getName().toString());
        for (FieldDeclaration fd : node.getFields()) {
            FieldDeclarationVisitor visitor = new FieldDeclarationVisitor(parserContext, source, cUnit, compilationUnit);
            fd.accept(visitor);
        }
        for (MethodDeclaration md : node.getMethods()) {
            if (md.isConstructor()) {}
            MethodASTVisitor visitor = new MethodASTVisitor(parserContext, source, cUnit, compilationUnit);
            md.accept(visitor);
        }

        return false;
    }
}
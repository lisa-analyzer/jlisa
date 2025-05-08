package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.lisa.program.ClassUnit;
import org.eclipse.jdt.core.dom.*;

public class ClassASTVisitor extends JavaASTVisitor{

    public ClassASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit) {
        super(parserContext, source, compilationUnit);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        ClassUnit cUnit = (ClassUnit) getProgram().getUnit(node.getName().toString());

        if (node.getSuperclassType() != null) {
            TypeASTVisitor visitor = new TypeASTVisitor(parserContext, source, compilationUnit);
            node.getSuperclassType().accept(visitor);
            it.unive.lisa.type.Type superType = visitor.getType();
            if (superType != null) {
                it.unive.lisa.program.Unit superUnit = getProgram().getUnit(superType.toString());
                if (superUnit instanceof it.unive.lisa.program.CompilationUnit) {
                    cUnit.addAncestor((it.unive.lisa.program.CompilationUnit)superUnit);
                }
            }
            node.getSuperclassType().accept(this);
            //throw new RuntimeException(new UnsupportedStatementException("extends is not supported yet."));
        }
        if (!node.permittedTypes().isEmpty()) {
            throw new RuntimeException(new UnsupportedStatementException("permits is not supported yet."));
        }

        for (FieldDeclaration fd : node.getFields()) {
            FieldDeclarationVisitor visitor = new FieldDeclarationVisitor(parserContext, source, cUnit, compilationUnit);
            fd.accept(visitor);
        }
        for (MethodDeclaration md : node.getMethods()) {
            MethodASTVisitor visitor = new MethodASTVisitor(parserContext, source, cUnit, compilationUnit);
            md.accept(visitor);
        }

        return false;
    }
}
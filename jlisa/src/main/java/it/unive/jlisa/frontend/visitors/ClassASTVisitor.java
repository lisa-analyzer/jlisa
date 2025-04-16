package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ClassASTVisitor extends JavaASTVisitor{

    public ClassASTVisitor(Program program, String source, int apiLevel, CompilationUnit compilationUnit) {
        super(program, source, apiLevel, compilationUnit);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (node.getSuperclassType() != null) {
            throw new RuntimeException(new UnsupportedStatementException("extends is not supported yet."));
        }
        if (!node.permittedTypes().isEmpty()) {
            throw new RuntimeException(new UnsupportedStatementException("permits is not supported yet."));
        }
        ClassUnit cUnit = (ClassUnit) program.getUnit(node.getName().toString());
        for (MethodDeclaration md : node.getMethods()) {
            if (md.isConstructor()) {}
            MethodASTVisitor visitor = new MethodASTVisitor(program, source, apiLevel, cUnit, compilationUnit);
            md.accept(visitor);
        }

        return false;
    }
}
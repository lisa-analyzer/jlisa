package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class InterfaceASTVisitor extends JavaASTVisitor {

    public InterfaceASTVisitor(Program program, String source, int apiLevel, CompilationUnit compilationUnit) {
        super(program, source, apiLevel, compilationUnit);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        InterfaceUnit iUnit = (InterfaceUnit) program.getUnit(node.getName().toString());
        //TODO manage superinterfaces
        if (node.getSuperclassType() != null) {
            throw new RuntimeException(new UnsupportedStatementException("extends is not supported yet."));
        }
        if (!node.superInterfaceTypes().isEmpty()) {
            throw new RuntimeException(new UnsupportedStatementException("implements is not supported yet."));
        }
        if (!node.permittedTypes().isEmpty()) {
            throw new RuntimeException(new UnsupportedStatementException("permits is not supported yet."));
        }
        return false;
    }


}

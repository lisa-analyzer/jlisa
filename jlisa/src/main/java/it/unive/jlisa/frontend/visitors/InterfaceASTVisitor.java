package it.unive.jlisa.frontend.visitors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;

public class InterfaceASTVisitor extends JavaASTVisitor {

    public InterfaceASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit) {
        super(parserContext, source, compilationUnit);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        // InterfaceUnit iUnit = (InterfaceUnit) getProgram().getUnit(node.getName().toString());
        //TODO manage superinterfaces
        if (node.getSuperclassType() != null) {
            parserContext.addException(
                    new ParsingException("extends-clause", ParsingException.Type.UNSUPPORTED_STATEMENT,
                            "The 'extends' clause is not supported yet.",
                            getSourceCodeLocation(node.getSuperclassType()))
            );
        }

        if (!node.superInterfaceTypes().isEmpty()) {
            parserContext.addException(
                    new ParsingException("implements-clause", ParsingException.Type.UNSUPPORTED_STATEMENT,
                            "The 'implements' clause is not supported yet.",
                            getSourceCodeLocation((ASTNode) node.superInterfaceTypes().get(0))) // using first interface for location
            );
        }

        if (!node.permittedTypes().isEmpty()) {
            parserContext.addException(
                    new ParsingException("permits-clause", ParsingException.Type.UNSUPPORTED_STATEMENT,
                            "The 'permits' clause is not supported yet.",
                            getSourceCodeLocation((ASTNode) node.permittedTypes().get(0))) // using first permitted type for location
            );
        }
        return false;
    }


}

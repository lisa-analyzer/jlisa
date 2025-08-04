package it.unive.jlisa.frontend.visitors;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.type.ArrayType;
import it.unive.lisa.type.Type;

public class VariableDeclarationASTVisitor extends JavaASTVisitor{
    Parameter parameter;
    public VariableDeclarationASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit) {
        super(parserContext, source, compilationUnit);
    }

    public boolean visit(SingleVariableDeclaration node) {
        TypeASTVisitor visitor = new TypeASTVisitor(parserContext, source, compilationUnit);
        node.getType().accept(visitor);
        Type type = visitor.getType();
        if (type == null) {
            throw new RuntimeException(new UnsupportedStatementException("variable should have a type"));
        }

        if (node.getExtraDimensions() != 0) {
            if (type instanceof ArrayType) {
                ArrayType arrayType = (ArrayType) type;
                int dim = arrayType.getDimensions();
                type = JavaArrayType.lookup(arrayType.getBaseType(), dim + node.getExtraDimensions());
            } else {
                type = JavaArrayType.lookup(type, node.getExtraDimensions());
            }
        }
        String identifier = node.getName().getIdentifier();
        //TODO annotations
        Annotations annotations = new Annotations();
        this.parameter = new Parameter(getSourceCodeLocation(node), identifier, type, null, annotations);
        return false;
    }

    public Parameter getParameter() {
        return this.parameter;
    }
}

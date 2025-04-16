package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.types.JavaArrayType;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.type.*;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.eclipse.jdt.core.dom.*;

public class TypeASTVisitor extends JavaASTVisitor{
    private Type type;
    public TypeASTVisitor(Program program, String source, int apiLevel, CompilationUnit compilationUnit) {
        super(program, source, apiLevel, compilationUnit);
    }


    @Override
    public boolean visit(PrimitiveType node) {
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.VOID) {
            type = VoidType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.INT) {
            type = Int32Type.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.LONG) {
            type = Int64Type.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.SHORT) {
            type = Int16Type.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.FLOAT) {
            type = Float32Type.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.DOUBLE) {
            type = Float64Type.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.BOOLEAN) {
            type = BoolType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.CHAR) {
            throw new RuntimeException(new UnsupportedStatementException("char type not supported"));
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.BYTE) {
            throw new RuntimeException(new UnsupportedStatementException("byte type not supported"));
        }
        return false;
    }

    @Override
    public boolean visit(ArrayType node) {
        TypeASTVisitor visitor = new TypeASTVisitor(program, source, apiLevel, compilationUnit);
        node.accept(visitor);
        Type type = visitor.getType();
        if (type == null) {
            throw new RuntimeException(new UnsupportedStatementException("array should have a type"));
        }
        if (node.getDimensions() == 0) {
            throw new RuntimeException(new UnsupportedStatementException("array should have at least one dimension"));
        }
        JavaArrayType javaArrayType = JavaArrayType.lookup(type, node.getDimensions());
        return false;

    }

    public boolean visit(WildcardType node) {
        throw new RuntimeException(new UnsupportedStatementException("wildcard type not supported"));
    }

    public boolean visit(UnionType node) {
        throw new RuntimeException(new UnsupportedStatementException("union type not supported"));
    }

    public boolean visit(IntersectionType node) {
        throw new RuntimeException(new UnsupportedStatementException("intersection type not supported"));
    }

    public boolean visit(QualifiedType node) {
        throw new RuntimeException(new UnsupportedStatementException("qualified type not supported"));
    }

    public boolean visit(NameQualifiedType node) {
        throw new RuntimeException(new UnsupportedStatementException("qualified type not supported"));
    }

    public boolean visit(SimpleName node) {
        type = Untyped.INSTANCE;
        return false;
    }
    public Type getType() {
        return type;
    }

}

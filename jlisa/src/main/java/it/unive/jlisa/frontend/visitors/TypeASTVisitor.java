package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.types.JavaArrayType;
import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.type.*;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.eclipse.jdt.core.dom.*;

public class TypeASTVisitor extends JavaASTVisitor{
    private Type type;
    public TypeASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit) {
        super(parserContext, source, compilationUnit);
    }


    @Override
    public boolean visit(PrimitiveType node) {
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.VOID) {
            type = VoidType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.INT) {
            type = IntType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.LONG) {
            type = LongType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.SHORT) {
            type = ShortType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.FLOAT) {
            type = FloatType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.DOUBLE) {
            type = DoubleType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.BOOLEAN) {
            type = BoolType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.CHAR) {
            parserContext.addException(new ParsingException(
                    "primitive-type", ParsingException.Type.UNSUPPORTED_STATEMENT,
                    "char type not supported",
                    getSourceCodeLocation(node)));
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.BYTE) {
            type = ByteType.INSTANCE;
        }
        return false;
    }

    @Override
    public boolean visit(ArrayType node) {
        TypeASTVisitor visitor = new TypeASTVisitor(parserContext, source, compilationUnit);
        node.getElementType().accept(visitor);
        Type _type = visitor.getType();
        if (_type == null) {
            throw new RuntimeException(new UnsupportedStatementException("array should have a type"));
        }
        if (node.getDimensions() == 0) {
            throw new RuntimeException(new UnsupportedStatementException("array should have at least one dimension"));
        }
        _type = JavaArrayType.lookup(_type, node.getDimensions());
        type = _type;
        return false;

    }

    @Override
    public boolean visit(UnionType node) {
        parserContext.addException(new ParsingException(
                "union-type", ParsingException.Type.UNSUPPORTED_STATEMENT,
                "Union type not supported",
                getSourceCodeLocation(node)));
        return false;
    }

    @Override
    public boolean visit(IntersectionType node) {
        parserContext.addException(new ParsingException(
                "intersection-type", ParsingException.Type.UNSUPPORTED_STATEMENT,
                "Intersection type not supported",
                getSourceCodeLocation(node)));
        return false;
    }

    @Override
    public boolean visit(QualifiedType node) {
        parserContext.addException(new ParsingException(
                "qualified-type", ParsingException.Type.UNSUPPORTED_STATEMENT,
                "Qualified type not supported",
                getSourceCodeLocation(node)));
        return false;
    }

    @Override
    public boolean visit(NameQualifiedType node) {
        parserContext.addException(new ParsingException(
                "qualified-type", ParsingException.Type.UNSUPPORTED_STATEMENT,
                "Name-qualified type not supported",
                getSourceCodeLocation(node)));
        return false;
    }

    public boolean visit(QualifiedName node) {
        parserContext.addException(new ParsingException(
                "qualified-name", ParsingException.Type.UNSUPPORTED_STATEMENT,
                "Qualified-type-name type not supported",
                getSourceCodeLocation(node)));
        return false;
    }
    public boolean visit(SimpleName node) {
        Unit u = getProgram().getUnit(node.getFullyQualifiedName());
        if (u == null) {
            throw new UnsupportedStatementException(node.getFullyQualifiedName() + " not exists in program, location: ." + getSourceCodeLocation(node));
        }
        if (!(u instanceof ClassUnit)) {
            throw new UnsupportedStatementException(node.getFullyQualifiedName() + " is not a class unit.");
        }
        JavaClassType javaClassType = JavaClassType.lookup(node.getFullyQualifiedName(), (ClassUnit)u);
        if (javaClassType == null) {
            type = Untyped.INSTANCE;
        } else {
            type = javaClassType;
        }
        return false;
    }
    public Type getType() {
        return type;
    }

}

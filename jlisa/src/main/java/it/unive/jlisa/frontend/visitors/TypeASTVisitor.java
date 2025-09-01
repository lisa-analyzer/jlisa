package it.unive.jlisa.frontend.visitors;

import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.UnionType;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;

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
            type = JavaIntType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.LONG) {
            type = JavaLongType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.SHORT) {
            type = JavaShortType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.FLOAT) {
            type = JavaFloatType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.DOUBLE) {
            type = JavaDoubleType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.BOOLEAN) {
            type = BoolType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.CHAR) {
            type = JavaCharType.INSTANCE;
        }
        if (node.getPrimitiveTypeCode() ==  PrimitiveType.BYTE) {
            type = JavaByteType.INSTANCE;
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
        _type = JavaArrayType.lookup(_type.isInMemoryType() ? new ReferenceType(_type) : _type, node.getDimensions());
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
    	// get the qualifier
        String qName = node.getName().toString();

        // look up the unit in the program (e.g., Map.Entry, we lookup Entry)
        Unit u = getProgram().getUnit(qName);
        if (u == null) 
            throw new UnsupportedStatementException(qName + " not exists in program, location: " + getSourceCodeLocation(node));
        
        if (!(u instanceof ClassUnit))
            throw new UnsupportedStatementException(qName + " is not a class unit.");
        
        JavaClassType javaClassType = JavaClassType.lookup(qName, (ClassUnit) u);
        if (javaClassType == null) {
            type = Untyped.INSTANCE;
        } else {
            type = javaClassType;
        }

        return false;
    }
    
    @Override
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
    
    
    @Override
    public boolean visit(ParameterizedType node) {
    	TypeASTVisitor visitor = new TypeASTVisitor(parserContext, source, compilationUnit);
        node.getType().accept(visitor);
        Type rawType = visitor.getType();

        if (rawType == null) 
            throw new UnsupportedStatementException("Parameterized type has no valid raw type: " + node);

        // we only keep the raw type (e.g., List from List<String>)
        type = rawType;
        return false;
    }
    
    public Type getType() {
        return type;
    }
}

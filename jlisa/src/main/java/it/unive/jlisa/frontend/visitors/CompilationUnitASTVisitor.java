package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.types.JavaClassType;
import it.unive.jlisa.types.JavaInterfaceType;
import it.unive.lisa.program.*;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;

public class CompilationUnitASTVisitor extends JavaASTVisitor {
    public CompilationUnitASTVisitor(Program program, String source, int apiLevel, CompilationUnit unit) {
        super(program, source, apiLevel, unit);
    }


    @Override
    public boolean visit(CompilationUnit node) {
        addUnits(node);
        visitUnits(node);

        return false;
    }

    private void addUnits(CompilationUnit unit) {
        List types = unit.types();
        for (Object type : types) {
            if (type instanceof TypeDeclaration) {
                TypeDeclaration typeDecl = (TypeDeclaration) type;
                if ((typeDecl.isInterface())) {
                    InterfaceUnit iUnit = buildInterfaceUnit(source, unit, program, typeDecl);
                    JavaInterfaceType.lookup(iUnit.getName(), iUnit);
                } else {
                    ClassUnit cUnit = buildClassUnit(source, unit, program, typeDecl);
                    JavaClassType.lookup(cUnit.getName(), cUnit);
                }
            }
            if (type instanceof EnumDeclaration) {
                throw new UnsupportedStatementException("EnumDeclaration not supported yet");
            }
            if (type instanceof AnnotationTypeDeclaration) {
                throw new UnsupportedStatementException("AnnotationTypeDeclaration not supported yet");
            }
        }
    }

    private void visitUnits(CompilationUnit unit) {
        List types = unit.types();
        for (Object type : types) {
            if (type instanceof TypeDeclaration) {
                TypeDeclaration typeDecl = (TypeDeclaration) type;
                if ((typeDecl.isInterface())) {
                    InterfaceASTVisitor interfaceVisitor = new InterfaceASTVisitor(program, source, apiLevel, unit);
                    typeDecl.accept(interfaceVisitor);
                } else {
                    ClassASTVisitor classVisitor = new ClassASTVisitor(program, source, apiLevel, unit);
                    typeDecl.accept(classVisitor);
                }
            }
            if (type instanceof EnumDeclaration) {
                throw new UnsupportedStatementException("EnumDeclaration not supported yet");
            }
            if (type instanceof AnnotationTypeDeclaration) {
                throw new UnsupportedStatementException("AnnotationTypeDeclaration not supported yet");
            }
        }

    }

    public InterfaceUnit buildInterfaceUnit(String source, CompilationUnit unit, Program program, TypeDeclaration typeDecl) {
        SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

        int modifiers = typeDecl.getModifiers();
        if (Modifier.isFinal(modifiers)) {
            throw new RuntimeException(new ProgramValidationException("Illegal combination of modifiers: interface and final"));
        }

        InterfaceUnit iUnit = new InterfaceUnit(loc, program, typeDecl.getName().toString(), false);
        program.addUnit(iUnit);
        return iUnit;
    }

    public ClassUnit buildClassUnit(String source, CompilationUnit unit, Program program, TypeDeclaration typeDecl) {
        SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

        int modifiers = typeDecl.getModifiers();
        if (Modifier.isPrivate(modifiers) && !(typeDecl.getParent() instanceof CompilationUnit)) {
            throw new RuntimeException(new ProgramValidationException("Modifier private not allowed in a top-level class"));
        }
        ClassUnit cUnit;
        if (Modifier.isAbstract(modifiers)) {
            if (Modifier.isFinal(modifiers)) {
                throw new RuntimeException(new ProgramValidationException("illegal combination of modifiers: abstract and final"));
            }
            cUnit = new AbstractClassUnit(loc, program, typeDecl.getName().toString(), Modifier.isFinal(modifiers));
        } else {
            cUnit = new ClassUnit(loc, program, typeDecl.getName().toString(), true);
        }
        program.addUnit(cUnit);
        return cUnit;
    }
}

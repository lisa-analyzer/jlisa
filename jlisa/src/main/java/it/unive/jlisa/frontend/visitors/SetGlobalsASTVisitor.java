package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.type.Type;
import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SetGlobalsASTVisitor extends ScopedVisitor<UnitScope> {

    public SetGlobalsASTVisitor(ParsingEnvironment env, UnitScope scope) {
        super(env, scope);
    }

    public boolean visit(
            CompilationUnit node) {
        List<?> types = node.types();
        Set<String> processed = new TreeSet<>();
        for (Object type : types)
            if (type instanceof TypeDeclaration)
                addGlobalsInDeclaration((TypeDeclaration) type, null, processed);
            else if (type instanceof EnumDeclaration)
                addEnumConstants((EnumDeclaration) type, null, processed);
        return false;
    }

    private void addGlobals(
            CompilationUnit unit,
            Set<String> processed) {
        List<?> types = unit.types();
        for (Object type : types)
            if (type instanceof TypeDeclaration)
                addGlobalsInDeclaration((TypeDeclaration) type, null, processed);
            else if (type instanceof EnumDeclaration)
                addEnumConstants((EnumDeclaration) type, null, processed);
    }

    private void addGlobalsInDeclaration(
            TypeDeclaration typeDecl,
            String outer,
            Set<String> processed) {
        String name = (getScope().getPackage().isEmpty() ? "" : scope.getPackage() + ".") + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
        if (!processed.add(name))
            return;
        if ((typeDecl.isInterface())) {
            JavaInterfaceType interfaceType = JavaInterfaceType.lookup(name);
            addFields(interfaceType.getUnit(), typeDecl, false, outer);
        } else {
            JavaClassType classType = JavaClassType.lookup(name);
            addFields(classType.getUnit(), typeDecl, true, outer);
        }

        // nested types (e.g., nested inner classes)
        String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
        TypeDeclaration[] nested = typeDecl.getTypes();
        for (TypeDeclaration n : nested)
            addGlobalsInDeclaration(n, newOuter, processed);
        for (Object decl : typeDecl.bodyDeclarations())
            if (decl instanceof TypeDeclaration)
                addGlobalsInDeclaration((TypeDeclaration) decl, newOuter, processed);
            else if (decl instanceof EnumDeclaration)
                addEnumConstants((EnumDeclaration) decl, newOuter, processed);
    }

    private void addFields(
            it.unive.lisa.program.CompilationUnit unit,
            TypeDeclaration typeDecl,
            boolean isClass,
            String outer) {
        JavaClassType enclosingType = null;
        if (isClass && outer != null) {
            // adding the synthetic field for the enclosing instance
            enclosingType = JavaClassType.lookup((getScope().getPackage().isEmpty() ? "" : getScope().getPackage() + ".") + outer);
            Global enclosingField = new Global(
                    getSourceCodeLocation(typeDecl.getName()),
                    unit,
                    "$enclosing",
                    true,
                    enclosingType.getReference());
            ((ClassUnit) unit).addInstanceGlobal(enclosingField);
        }

        // iterates over inner declarations
        for (Object decl : typeDecl.bodyDeclarations()) {
            Set<String> visitedFieldNames = new HashSet<>();
            if (decl instanceof FieldDeclaration fdecl) {
                FieldDeclarationVisitor visitor = new FieldDeclarationVisitor(getEnvironment(), getScope().toClassScope(enclosingType, (ClassUnit) unit),
                        visitedFieldNames);
                fdecl.accept(visitor);
            }
        }
    }

    private void addEnumConstants(
            EnumDeclaration node,
            String outer,
            Set<String> processed) {
        String name = (getScope().getPackage().isEmpty() ? "" : getScope().getPackage() + ".") + (outer == null ? "" : outer + ".") + node.getName().toString();
        if (!processed.add(name))
            return;
        EnumUnit enUnit = (EnumUnit) getProgram().getUnit(name);
        Type enumType = JavaClassType.lookup(enUnit.getName());

        // we add the name field
        Global nameField = new Global(
                getSourceCodeLocation(node.getName()),
                enUnit,
                "name",
                true,
                JavaClassType.getStringType().getReference());
        enUnit.addInstanceGlobal(nameField);

        // adding static fields corresponding to enum constants
        for (Object con : node.enumConstants()) {
            Global g = new Global(
                    getSourceCodeLocation((ASTNode) con),
                    enUnit,
                    con.toString(),
                    false,
                    new JavaReferenceType(enumType));
            enUnit.addGlobal(g);
        }

        // nested types (e.g., nested inner classes)
        String newOuter = outer == null ? node.getName().toString() : outer + "." + node.getName().toString();
        for (Object decl : node.bodyDeclarations())
            if (decl instanceof TypeDeclaration)
                addGlobalsInDeclaration((TypeDeclaration) decl, newOuter, processed);
            else if (decl instanceof EnumDeclaration)
                addEnumConstants((EnumDeclaration) decl, newOuter, processed);
    }
}

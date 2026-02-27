package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.lisa.type.UnitType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SetRelationshipsASTVisitor extends ScopedVisitor<UnitScope>{
    public SetRelationshipsASTVisitor(ParsingEnvironment env, UnitScope scope) {
        super(env, scope);
    }

    public boolean visit(
            CompilationUnit node) {
        List<?> types = node.types();
        Set<String> processed = new TreeSet<>();
        for (Object type : types)
            if (type instanceof TypeDeclaration)
                setRelationshipsInDeclaration(node, null, (TypeDeclaration) type, processed);
            else if (type instanceof EnumDeclaration)
                setEnumRelationships(node, null, (EnumDeclaration) type, processed);

        return false;
    }

    private void setRelationshipsInDeclaration(
            CompilationUnit unit,
            String outer,
            TypeDeclaration typeDecl,
            Set<String> processed) {
        it.unive.lisa.program.CompilationUnit lisaCU = null;
        String name = (getScope().getPackage().isEmpty() ? "" : scope.getPackage() + ".") + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
        if (!processed.add(name))
            return;
        if (typeDecl.isInterface())
            lisaCU = JavaInterfaceType.lookup(name).getUnit();
        else
            lisaCU = JavaClassType.lookup(name).getUnit();

        if (typeDecl.getSuperclassType() != null)
            setSupertype(unit, typeDecl.getSuperclassType(), lisaCU);
        else
            lisaCU.addAncestor(JavaClassType.getObjectType().getUnit());

        for (Object oInterfaceType : typeDecl.superInterfaceTypes())
            setSupertype(unit, (ASTNode) oInterfaceType, lisaCU);

        String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
        for (TypeDeclaration nested : typeDecl.getTypes())
            setRelationshipsInDeclaration(unit, newOuter, nested, processed);
        for (Object nested : typeDecl.bodyDeclarations())
            if (nested instanceof TypeDeclaration)
                setRelationshipsInDeclaration(unit, newOuter, (TypeDeclaration) nested, processed);
            else if (nested instanceof EnumDeclaration)
                setEnumRelationships(unit, newOuter, (EnumDeclaration) nested, processed);
    }

    private void setEnumRelationships(
            CompilationUnit unit,
            String outer,
            EnumDeclaration typeDecl,
            Set<String> processed) {
        String name = (getScope().getPackage().isEmpty() ? "" : scope.getPackage() + ".") + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
        if (!processed.add(name))
            return;
        it.unive.lisa.program.CompilationUnit lisaCU = JavaClassType.lookup(name).getUnit();

        lisaCU.addAncestor(JavaClassType.getObjectType().getUnit());

        for (Object oInterfaceType : typeDecl.superInterfaceTypes())
            setSupertype(unit, (ASTNode) oInterfaceType, lisaCU);

        String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
        for (Object nested : typeDecl.bodyDeclarations())
            if (nested instanceof TypeDeclaration)
                setRelationshipsInDeclaration(unit, newOuter, (TypeDeclaration) nested, processed);
            else if (nested instanceof EnumDeclaration)
                setEnumRelationships(unit, newOuter, (EnumDeclaration) nested, processed);
    }

    private void setSupertype(
            CompilationUnit unit,
            ASTNode typeDecl,
            it.unive.lisa.program.CompilationUnit lisaCU) {
        TypeASTVisitor typeVisitor = new TypeASTVisitor(getEnvironment(), getScope());
        typeDecl.accept(typeVisitor);
        it.unive.lisa.type.Type superClassType = typeVisitor.getType();
        if (superClassType != null) {
            UnitType unitType = superClassType.asUnitType();
            if (unitType != null)
                lisaCU.addAncestor(unitType.getUnit());
        }
    }
}

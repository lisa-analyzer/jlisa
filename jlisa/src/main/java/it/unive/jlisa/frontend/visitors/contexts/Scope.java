package it.unive.jlisa.frontend.visitors.contexts;

import it.unive.jlisa.frontend.visitors.BaseUnitASTVisitor;
import it.unive.jlisa.program.type.JavaClassType;

public final class Scope {

    private final it.unive.lisa.program.CompilationUnit lisaUnit;
    private final JavaClassType enclosingClass;
    private final BaseUnitASTVisitor container;

    public Scope(it.unive.lisa.program.CompilationUnit lisaUnit,
                 JavaClassType enclosingClass,
                 BaseUnitASTVisitor container) {
        this.lisaUnit = lisaUnit;
        this.enclosingClass = enclosingClass;
        this.container = container;
    }

    public Scope withEnclosingClass(JavaClassType type) {
        return new Scope(lisaUnit, type, container);
    }

    public it.unive.lisa.program.CompilationUnit lisaUnit() { return lisaUnit; }
    public JavaClassType enclosingClass() { return enclosingClass; }
    public BaseUnitASTVisitor container() { return container; }
}
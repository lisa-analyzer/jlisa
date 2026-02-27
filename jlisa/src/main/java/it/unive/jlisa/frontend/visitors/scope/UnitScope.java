package it.unive.jlisa.frontend.visitors.scope;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.*;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.*;

public final class UnitScope extends Scope {
    private final String pkg;
    private final Map<String, String> explicitImports;
    private final Set<String> onDemandPackages = new HashSet<>();
    private final Map<String, it.unive.lisa.program.CompilationUnit> localTypes = new HashMap<>();
    private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(UnitScope.class);


    public UnitScope(String pkg, Map<String, String> explicitImports) {
        this.pkg = pkg != null ? pkg : "";
        this.explicitImports = explicitImports;
    }

    public String getPackage() {
        return pkg;
    }

    public Map<String, String> getExplicitImports() { return explicitImports; }


    public ClassScope toClassScope(JavaClassType enclosingClass, ClassUnit lisaUnit) {
        return new ClassScope(this, null, enclosingClass, lisaUnit);
    }


    public static UnitScope init(ParsingEnvironment environment, CompilationUnit cu) {
        String pkg = cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName() : null;
        UnitScope scope = new UnitScope(pkg, new HashMap<>());

        // Step 1: add java.lang imports
        addJavaLangImports(scope);

        // Step 2: process explicit and on-demand imports
        processImports(scope, environment, cu.imports());


        for (Object type : cu.types()) {
            if (type instanceof TypeDeclaration td) {
                addType(scope, environment, td, null);
            } else if (type instanceof EnumDeclaration ed) {
                addEnum(scope, environment, ed, null);
            }
        }
        // Step 3: add local types (classes + enums) as explicit imports and register in scope
        addLocalTypes(scope, environment, cu);

        return scope;
    }

    /** Recursively add TypeDeclaration and nested types */
    private static void addType(UnitScope scope, ParsingEnvironment env, TypeDeclaration td, String outer) {
        String fqn = scope.getPackage() + "." + (outer == null ? "" : outer + ".") + td.getName().toString();
        scope.addExplicitImport(td.getName().toString(), fqn);

        it.unive.lisa.program.CompilationUnit unit = buildUnit(env, td, fqn);
        scope.addLocalType(td.getName().toString(), unit);

        String newOuter = outer == null ? td.getName().toString() : outer + "." + td.getName().toString();

        // Nested classes
        for (TypeDeclaration nested : td.getTypes()) {
            addType(scope, env, nested, newOuter);
        }

        // Nested enums
        for (Object decl : td.bodyDeclarations()) {
            if (decl instanceof EnumDeclaration ed) {
                addEnum(scope, env, ed, newOuter);
            }
        }
    }

    /** Add enum declarations recursively */
    private static void addEnum(UnitScope scope, ParsingEnvironment env, EnumDeclaration ed, String outer) {
        String fqn = scope.getPackage() + (outer == null ? "" : outer + ".") + ed.getName().toString();
        scope.addExplicitImport(ed.getName().toString(), fqn);
        scope.addLocalType(ed.getName().toString(), buildEnumUnit(env, ed, fqn));
    }

    private void addLocalType(String string, it.unive.lisa.program.CompilationUnit unit) {
        this.localTypes.put(string, unit);
    }

    public it.unive.lisa.program.CompilationUnit getLocalType(String name) {
        return localTypes.get(name);
    }

    public Set<String> getOnDemandPackages() {
        return onDemandPackages;
    }

    private static it.unive.lisa.program.CompilationUnit buildUnit(ParsingEnvironment env, TypeDeclaration td, String fqn) {
        int modifiers = td.getModifiers();

        if (!td.isInterface() && Modifier.isPrivate(modifiers))
            throw new RuntimeException(
                    new ProgramValidationException("Modifier private not allowed in a top-level class"));

        if (td.isInterface()) {
            // Interface unit
            return new InterfaceUnit(env.getSourceCodeLocation(td), env.parserContext().getProgram(), fqn, Modifier.isFinal(modifiers));
        } else if (Modifier.isAbstract(modifiers)) {
            if (Modifier.isFinal(modifiers))
                throw new RuntimeException(
                        new ProgramValidationException("illegal combination of modifiers: abstract and final"));
            return new AbstractClassUnit(env.getSourceCodeLocation(td), env.parserContext().getProgram(), fqn, Modifier.isFinal(modifiers));
        } else {
            return new ClassUnit(env.getSourceCodeLocation(td), env.parserContext().getProgram(), fqn, Modifier.isFinal(modifiers));
        }
    }

    /**
     * Adds all local types (classes and enums) declared in this compilation unit
     * to the scope as explicit imports, and registers nested types recursively.
     */
    private static void addLocalTypes(UnitScope scope, ParsingEnvironment env, CompilationUnit cu) {
        for (Object type : cu.types()) {
            if (type instanceof TypeDeclaration td) {
                addTypeDeclaration(scope, env, td, null);
            } else if (type instanceof EnumDeclaration ed) {
                String fqn = scope.getPackage() + ed.getName().toString();
                scope.addExplicitImport(ed.getName().toString(), fqn);
                scope.addLocalType(ed.getName().toString(), buildEnumUnit(env, ed, fqn));
            }
        }
    }

    private void addExplicitImport(String string, String fqn) {
        this.explicitImports.put(string, fqn);
    }

    /**
     * Recursively adds a TypeDeclaration (class/interface) and its nested types
     * to the scope.
     */
    private static void addTypeDeclaration(UnitScope scope, ParsingEnvironment env, TypeDeclaration td, String outer) {
        String name = scope.getPackage() + (outer == null ? "" : outer + ".") + td.getName().toString();

        // Register as explicit import
        scope.addExplicitImport(td.getName().toString(), name);

        // Create the Unit (ClassUnit or InterfaceUnit)
        it.unive.lisa.program.CompilationUnit unit = buildUnit(env, td, name);
        scope.addLocalType(td.getName().toString(), unit);

        // Handle nested types
        String newOuter = outer == null ? td.getName().toString() : outer + "." + td.getName().toString();
        for (TypeDeclaration nested : td.getTypes()) {
            addTypeDeclaration(scope, env, nested, newOuter);
        }

        for (Object decl : td.bodyDeclarations()) {
            if (decl instanceof EnumDeclaration ed) {
                String fqn = scope.getPackage() + newOuter + "." + ed.getName().toString();
                scope.addExplicitImport(ed.getName().toString(), fqn);
                scope.addLocalType(ed.getName().toString(), buildEnumUnit(env, ed, fqn));
            }
        }
    }

    private static EnumUnit buildEnumUnit(ParsingEnvironment env, EnumDeclaration ed, String fqn) {
        EnumUnit enUnit = new EnumUnit(env.getSourceCodeLocation(ed), env.parserContext().getProgram(), fqn, true);
        env.parserContext().getProgram().addUnit(enUnit);
        JavaClassType.register(enUnit.getName(), enUnit);
        return enUnit;
    }

    private static void addLocalImports(
            UnitScope scope,
            CompilationUnit unit
            ) {
        List<?> types = unit.types();
        for (Object type : types) {
            if (type instanceof TypeDeclaration)
                addLocalImportsInDeclaration(scope, (TypeDeclaration) type, null);
            else if (type instanceof EnumDeclaration) {
                EnumDeclaration typeDecl = (EnumDeclaration) type;
                String name = scope.getPackage() + typeDecl.getName().toString();
                scope.getExplicitImports().put(typeDecl.getName().toString(), name);
            }
        }
    }

    private static void addLocalImportsInDeclaration(
            UnitScope scope,
            TypeDeclaration typeDecl,
            String outer) {
        String name = scope.getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
        scope.getExplicitImports().put(typeDecl.getName().toString(), name);
        if (outer != null)
            scope.getExplicitImports().put((outer == null ? "" : outer + ".") + typeDecl.getName().toString(), name);

        // nested types (e.g., nested inner classes)
        String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
        for (TypeDeclaration nested : typeDecl.getTypes())
            addLocalImportsInDeclaration(scope, nested, newOuter);
        for (Object decl : typeDecl.bodyDeclarations()) {
            if (decl instanceof EnumDeclaration enumDecl) {
                name = scope.getPackage() + (newOuter == null ? "" : newOuter + ".") + enumDecl.getName().toString();
                scope.getExplicitImports().put(enumDecl.getName().toString(), name);
                scope.getExplicitImports().put(newOuter + "." + enumDecl.getName().toString(), name);
            }
        }
    }


    private static void processImports(
            UnitScope scope,
            ParsingEnvironment environment,
            List<ImportDeclaration> imports) {

        Set<String> seen = new HashSet<>();
        for (ImportDeclaration i : imports) {
            String fqn = i.getName().getFullyQualifiedName();
            if (!seen.add(fqn))
                LOG.error("Duplicated import " + fqn + " at " + environment.getSourceCodeLocation(i));
        }

        for (ImportDeclaration i : imports) {
            if (i.isStatic()) {
                throw new ParsingException(
                        "java-import",
                        ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Static imports are not supported.",
                        environment.getSourceCodeLocation(i));
            }

            String importName = i.getName().getFullyQualifiedName();

            if (i.isOnDemand()) {
                // just record the on-demand package in the scope
                scope.addOnDemandPackage(importName);
            } else {
                // explicit import: map short name -> fully qualified name
                String shortName;
                if (i.getName().isSimpleName())
                    shortName = importName;
                else
                    shortName = ((QualifiedName) i.getName()).getName().getFullyQualifiedName();

                scope.addExplicitImport(shortName, importName);
            }
        }
    }

    private void addOnDemandPackage(String importName) {
        onDemandPackages.add(importName);
    }

    private static boolean buildClassUnit(
            UnitScope scope,
            ParsingEnvironment env,
            Program program,
            String outer,
            TypeDeclaration typeDecl,
            Set<String> processed) {
        SourceCodeLocation loc = env.getSourceCodeLocation(typeDecl);

        int modifiers = typeDecl.getModifiers();
        if (Modifier.isPrivate(modifiers) && outer == null)
            throw new RuntimeException(
                    new ProgramValidationException("Modifier private not allowed in a top-level class"));

        ClassUnit cUnit;
        String name = scope.getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
        if (!processed.add(name))
            return false;
        if (Modifier.isAbstract(modifiers))
            if (Modifier.isFinal(modifiers))
                throw new RuntimeException(
                        new ProgramValidationException("illegal combination of modifiers: abstract and final"));
            else
                cUnit = new AbstractClassUnit(loc, program, name, Modifier.isFinal(modifiers));
        else
            cUnit = new ClassUnit(loc, program, name, Modifier.isFinal(modifiers));

        program.addUnit(cUnit);
        JavaClassType.register(cUnit.getName(), cUnit);
        return true;
    }






    private static void addJavaLangImports(UnitScope scope) {
        scope.getExplicitImports().put("Appendable", "java.lang.Appendable");
        scope.getExplicitImports().put("AutoCloseable", "java.lang.AutoCloseable");
        scope.getExplicitImports().put("CharSequence", "java.lang.CharSequence");
        scope.getExplicitImports().put("Cloneable", "java.lang.Cloneable");
        scope.getExplicitImports().put("Comparable", "java.lang.Comparable");
        scope.getExplicitImports().put("Iterable", "java.lang.Iterable");
        scope.getExplicitImports().put("Readable", "java.lang.Readable");
        scope.getExplicitImports().put("Runnable", "java.lang.Runnable");
        scope.getExplicitImports().put("Thread.UncaughtExceptionHandler", "java.lang.Thread.UncaughtExceptionHandler");
        scope.getExplicitImports().put("Boolean", "java.lang.Boolean");
        scope.getExplicitImports().put("Byte", "java.lang.Byte");
        scope.getExplicitImports().put("Character", "java.lang.Character");
        scope.getExplicitImports().put("Character.Subset", "java.lang.Character.Subset");
        scope.getExplicitImports().put("Character.UnicodeBlock", "java.lang.Character.UnicodeBlock");
        scope.getExplicitImports().put("Class", "java.lang.Class");
        scope.getExplicitImports().put("ClassLoader", "java.lang.ClassLoader");
        scope.getExplicitImports().put("ClassValue", "java.lang.ClassValue");
        scope.getExplicitImports().put("Compiler", "java.lang.Compiler");
        scope.getExplicitImports().put("Double", "java.lang.Double");
        scope.getExplicitImports().put("Enum", "java.lang.Enum");
        scope.getExplicitImports().put("Float", "java.lang.Float");
        scope.getExplicitImports().put("InheritableThreadLocal", "java.lang.InheritableThreadLocal");
        scope.getExplicitImports().put("Integer", "java.lang.Integer");
        scope.getExplicitImports().put("Long", "java.lang.Long");
        scope.getExplicitImports().put("Math", "java.lang.Math");
        scope.getExplicitImports().put("Number", "java.lang.Number");
        scope.getExplicitImports().put("Object", "java.lang.Object");
        scope.getExplicitImports().put("Package", "java.lang.Package");
        scope.getExplicitImports().put("Process", "java.lang.Process");
        scope.getExplicitImports().put("ProcessBuilder", "java.lang.ProcessBuilder");
        scope.getExplicitImports().put("ProcessBuilder.Redirect", "java.lang.ProcessBuilder.Redirect");
        scope.getExplicitImports().put("Runtime", "java.lang.Runtime");
        scope.getExplicitImports().put("RuntimePermission", "java.lang.RuntimePermission");
        scope.getExplicitImports().put("SecurityManager", "java.lang.SecurityManager");
        scope.getExplicitImports().put("Short", "java.lang.Short");
        scope.getExplicitImports().put("StackTraceElement", "java.lang.StackTraceElement");
        scope.getExplicitImports().put("StrictMath", "java.lang.StrictMath");
        scope.getExplicitImports().put("String", "java.lang.String");
        scope.getExplicitImports().put("StringBuffer", "java.lang.StringBuffer");
        scope.getExplicitImports().put("StringBuilder", "java.lang.StringBuilder");
        scope.getExplicitImports().put("System", "java.lang.System");
        scope.getExplicitImports().put("Thread", "java.lang.Thread");
        scope.getExplicitImports().put("ThreadGroup", "java.lang.ThreadGroup");
        scope.getExplicitImports().put("ThreadLocal", "java.lang.ThreadLocal");
        scope.getExplicitImports().put("Throwable", "java.lang.Throwable");
        scope.getExplicitImports().put("Void", "java.lang.Void");
        scope.getExplicitImports().put("Character.UnicodeScript", "java.lang.Character.UnicodeScript");
        scope.getExplicitImports().put("ProcessBuilder.Redirect.Type", "java.lang.ProcessBuilder.Redirect.Type");
        scope.getExplicitImports().put("Thread.State", "java.lang.Thread.State");
        scope.getExplicitImports().put("ArithmeticException", "java.lang.ArithmeticException");
        scope.getExplicitImports().put("ArrayIndexOutOfBoundsException", "java.lang.ArrayIndexOutOfBoundsException");
        scope.getExplicitImports().put("ArrayStoreException", "java.lang.ArrayStoreException");
        scope.getExplicitImports().put("ClassCastException", "java.lang.ClassCastException");
        scope.getExplicitImports().put("ClassNotFoundException", "java.lang.ClassNotFoundException");
        scope.getExplicitImports().put("CloneNotSupportedException", "java.lang.CloneNotSupportedException");
        scope.getExplicitImports().put("EnumConstantNotPresentException", "java.lang.EnumConstantNotPresentException");
        scope.getExplicitImports().put("Exception", "java.lang.Exception");
        scope.getExplicitImports().put("IllegalAccessException", "java.lang.IllegalAccessException");
        scope.getExplicitImports().put("IllegalArgumentException", "java.lang.IllegalArgumentException");
        scope.getExplicitImports().put("IllegalMonitorStateException", "java.lang.IllegalMonitorStateException");
        scope.getExplicitImports().put("IllegalStateException", "java.lang.IllegalStateException");
        scope.getExplicitImports().put("IllegalThreadStateException", "java.lang.IllegalThreadStateException");
        scope.getExplicitImports().put("IndexOutOfBoundsException", "java.lang.IndexOutOfBoundsException");
        scope.getExplicitImports().put("InstantiationException", "java.lang.InstantiationException");
        scope.getExplicitImports().put("InterruptedException", "java.lang.InterruptedException");
        scope.getExplicitImports().put("NegativeArraySizeException", "java.lang.NegativeArraySizeException");
        scope.getExplicitImports().put("NoSuchFieldException", "java.lang.NoSuchFieldException");
        scope.getExplicitImports().put("NoSuchMethodException", "java.lang.NoSuchMethodException");
        scope.getExplicitImports().put("NullPointerException", "java.lang.NullPointerException");
        scope.getExplicitImports().put("NumberFormatException", "java.lang.NumberFormatException");
        scope.getExplicitImports().put("ReflectiveOperationException", "java.lang.ReflectiveOperationException");
        scope.getExplicitImports().put("RuntimeException", "java.lang.RuntimeException");
        scope.getExplicitImports().put("SecurityException", "java.lang.SecurityException");
        scope.getExplicitImports().put("StringIndexOutOfBoundsException", "java.lang.StringIndexOutOfBoundsException");
        scope.getExplicitImports().put("TypeNotPresentException", "java.lang.TypeNotPresentException");
        scope.getExplicitImports().put("UnsupportedOperationException", "java.lang.UnsupportedOperationException");
        scope.getExplicitImports().put("AbstractMethodError", "java.lang.AbstractMethodError");
        scope.getExplicitImports().put("AssertionError", "java.lang.AssertionError");
        scope.getExplicitImports().put("BootstrapMethodError", "java.lang.BootstrapMethodError");
        scope.getExplicitImports().put("ClassCircularityError", "java.lang.ClassCircularityError");
        scope.getExplicitImports().put("ClassFormatError", "java.lang.ClassFormatError");
        scope.getExplicitImports().put("Error", "java.lang.Error");
        scope.getExplicitImports().put("ExceptionInInitializerError", "java.lang.ExceptionInInitializerError");
        scope.getExplicitImports().put("IllegalAccessError", "java.lang.IllegalAccessError");
        scope.getExplicitImports().put("IncompatibleClassChangeError", "java.lang.IncompatibleClassChangeError");
        scope.getExplicitImports().put("InstantiationError", "java.lang.InstantiationError");
        scope.getExplicitImports().put("InternalError", "java.lang.InternalError");
        scope.getExplicitImports().put("LinkageError", "java.lang.LinkageError");
        scope.getExplicitImports().put("NoClassDefFoundError", "java.lang.NoClassDefFoundError");
        scope.getExplicitImports().put("NoSuchFieldError", "java.lang.NoSuchFieldError");
        scope.getExplicitImports().put("NoSuchMethodError", "java.lang.NoSuchMethodError");
        scope.getExplicitImports().put("OutOfMemoryError", "java.lang.OutOfMemoryError");
        scope.getExplicitImports().put("StackOverflowError", "java.lang.StackOverflowError");
        scope.getExplicitImports().put("ThreadDeath", "java.lang.ThreadDeath");
        scope.getExplicitImports().put("UnknownError", "java.lang.UnknownError");
        scope.getExplicitImports().put("UnsatisfiedLinkError", "java.lang.UnsatisfiedLinkError");
        scope.getExplicitImports().put("UnsupportedClassVersionError", "java.lang.UnsupportedClassVersionError");
        scope.getExplicitImports().put("VerifyError", "java.lang.VerifyError");
        scope.getExplicitImports().put("VirtualMachineError", "java.lang.VirtualMachineError");
        scope.getExplicitImports().put("Deprecated", "java.lang.Deprecated");
        scope.getExplicitImports().put("FunctionalInterface", "java.lang.FunctionalInterface");
        scope.getExplicitImports().put("Override", "java.lang.Override");
        scope.getExplicitImports().put("SafeVarargs", "java.lang.SafeVarargs");
        scope.getExplicitImports().put("SuppressWarnings", "java.lang.SuppressWarnings");
    }

}
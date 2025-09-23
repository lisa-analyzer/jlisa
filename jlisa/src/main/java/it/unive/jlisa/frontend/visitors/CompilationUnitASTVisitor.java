package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.AbstractClassUnit;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class CompilationUnitASTVisitor extends BaseUnitASTVisitor {

	private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(CompilationUnitASTVisitor.class);

	public enum VisitorType {
		ADD_UNITS,
		VISIT_UNIT,
		ADD_GLOBALS,
		SET_RELATIONSHIPS
	}

	public VisitorType visitorType;

	public CompilationUnitASTVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit unit,
			VisitorType visitorType) {
		super(parserContext, source, null, new TreeMap<>(), unit);
		this.visitorType = visitorType;
	}

	@Override
	public boolean visit(
			PackageDeclaration node) {
		this.pkg = node != null ? node.getName().getFullyQualifiedName() : null;
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean visit(
			CompilationUnit node) {

		visit(node.getPackage());

		// java.lang is always imported
		addJavaLangImports();

		// java.lang imports can be overwritten
		visit(node.imports());

		// process imports from nested declarations
		addLocalImports(node);

		if (visitorType == VisitorType.ADD_UNITS) {
			// phase 1
			addUnits(node);
		} else if (visitorType == VisitorType.SET_RELATIONSHIPS) {
			// phase 2
			setRelationships(node);
		} else if (visitorType == VisitorType.ADD_GLOBALS) {
			// phase 3
			addGlobals(node);
		} else if (visitorType == VisitorType.VISIT_UNIT) {
			// phase 4
			visitUnits(node);
		}
		return false;
	}

	private void visit(
			List<ImportDeclaration> imports) {
		for (int l = 0; l < imports.size() - 1; l++) {
			for (int k = l + 1; k < imports.size(); k++) {
				if (imports.get(l).getName().getFullyQualifiedName()
						.equals(imports.get(k).getName().getFullyQualifiedName()))
					LOG.error("Duplicated import " + imports.get(k).getName().getFullyQualifiedName() + " at "
							+ getSourceCodeLocation(imports.get(l)) + " and "
							+ getSourceCodeLocation(imports.get(k)));
			}
		}

		for (ImportDeclaration i : imports)
			if (i.isStatic())
				throw new ParsingException("java-import", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Static imports are not supported.", getSourceCodeLocation(i));
			else if (i.isOnDemand()) {
				Collection<String> libs = LibrarySpecificationProvider
						.getLibrariesOfPackage(i.getName().getFullyQualifiedName());
				for (String lib : libs) {
					this.imports.put(lib.substring(lib.lastIndexOf(".") + 1), lib);
					LibrarySpecificationProvider.importClass(getProgram(), lib);
				}
			} else {
				String importName = i.getName().getFullyQualifiedName();
				String shortName;
				if (i.getName().isSimpleName())
					shortName = i.getName().getFullyQualifiedName();
				else
					shortName = ((QualifiedName) i.getName()).getName().getFullyQualifiedName();

				this.imports.put(shortName, importName);

				if (LibrarySpecificationProvider.isLibraryAvailable(importName)) {
					LibrarySpecificationProvider.importClass(getProgram(), importName);
					for (String lib : LibrarySpecificationProvider.getNestedUnits(importName)) {
						LibrarySpecificationProvider.importClass(getProgram(), lib);
						// eg we are importing "java.util.Map", we want to
						// include also "java.util.Map.Entry"
						// - "java.util.Map.Entry".replace("java.util.Map", "")
						// = ".Entry"
						// - ".Entry".substring(1) = "Entry"
						// so the short name is Map.Entry
						String libname = shortName + "." + lib.replace(importName, "").substring(1);
						this.imports.put(libname, lib);
					}
				}
			}
	}

	private void addLocalImports(
			CompilationUnit unit) {
		List<?> types = unit.types();
		for (Object type : types) {
			if (type instanceof TypeDeclaration)
				addLocalImportsInDeclaration((TypeDeclaration) type, null);
			else if (type instanceof EnumDeclaration) {
				EnumDeclaration typeDecl = (EnumDeclaration) type;
				String name = getPackage() + typeDecl.getName().toString();
				imports.put(typeDecl.getName().toString(), name);
			}
		}
	}

	private void addLocalImportsInDeclaration(
			TypeDeclaration typeDecl,
			String outer) {
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		imports.put(typeDecl.getName().toString(), name);
		if (outer != null)
			imports.put((outer == null ? "" : outer + ".") + typeDecl.getName().toString(), name);

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		for (TypeDeclaration nested : typeDecl.getTypes())
			addLocalImportsInDeclaration(nested, newOuter);
		for (Object decl : typeDecl.bodyDeclarations()) {
			if (decl instanceof EnumDeclaration) {
				EnumDeclaration enumDecl = (EnumDeclaration) decl;
				name = getPackage() + (newOuter == null ? "" : newOuter + ".") + enumDecl.getName().toString();
				imports.put(enumDecl.getName().toString(), name);
				imports.put(newOuter + "." + enumDecl.getName().toString(), name);
			}
		}
	}

	private void addUnits(
			CompilationUnit unit) {
		List<?> types = unit.types();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				addUnitsInDeclaration((TypeDeclaration) type, null);
			else if (type instanceof EnumDeclaration)
				buildEnumUnit(source, getProgram(), null, (EnumDeclaration) type);
	}

	private void addUnitsInDeclaration(
			TypeDeclaration typeDecl,
			String outer) {
		if ((typeDecl.isInterface()))
			buildInterfaceUnit(source, compilationUnit, getProgram(), outer, typeDecl);
		else
			buildClassUnit(source, compilationUnit, getProgram(), outer, typeDecl);

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		for (TypeDeclaration nested : typeDecl.getTypes())
			addUnitsInDeclaration(nested, newOuter);
		for (Object decl : typeDecl.bodyDeclarations()) {
			if (decl instanceof EnumDeclaration)
				buildEnumUnit(source, getProgram(), newOuter, (EnumDeclaration) decl);
		}
	}

	private void buildInterfaceUnit(
			String source,
			CompilationUnit unit,
			Program program,
			String outer,
			TypeDeclaration typeDecl) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

		int modifiers = typeDecl.getModifiers();
		if (Modifier.isFinal(modifiers)) {
			throw new RuntimeException(
					new ProgramValidationException("Illegal combination of modifiers: interface and final"));
		}

		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		InterfaceUnit iUnit = new InterfaceUnit(loc, program, name, false);
		program.addUnit(iUnit);
		JavaInterfaceType.register(iUnit.getName(), iUnit);
	}

	private void buildClassUnit(
			String source,
			CompilationUnit unit,
			Program program,
			String outer,
			TypeDeclaration typeDecl) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

		int modifiers = typeDecl.getModifiers();
		if (Modifier.isPrivate(modifiers) && !(typeDecl.getParent() instanceof CompilationUnit))
			throw new RuntimeException(
					new ProgramValidationException("Modifier private not allowed in a top-level class"));

		ClassUnit cUnit;
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
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
	}

	private void buildEnumUnit(
			String source,
			Program program,
			String outer,
			EnumDeclaration typeDecl) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		EnumUnit enUnit = new EnumUnit(loc, program, name, true);
		program.addUnit(enUnit);
		JavaClassType.register(enUnit.getName(), enUnit);
	}

	private void setRelationships(
			CompilationUnit unit) {
		List<?> types = unit.types();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				setRelationshipsInDeclaration(unit, null, (TypeDeclaration) type);
	}

	private void setRelationshipsInDeclaration(
			CompilationUnit unit,
			String outer,
			TypeDeclaration typeDecl) {
		it.unive.lisa.program.CompilationUnit lisaCU = null;
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		if (typeDecl.isInterface())
			lisaCU = JavaInterfaceType.lookup(name).getUnit();
		else
			lisaCU = JavaClassType.lookup(name).getUnit();

		if (typeDecl.getSuperclassType() != null)
			setSupertype(unit, typeDecl.getSuperclassType(), lisaCU);

		for (Object oInterfaceType : typeDecl.superInterfaceTypes())
			setSupertype(unit, (ASTNode) oInterfaceType, lisaCU);

		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		for (TypeDeclaration nested : typeDecl.getTypes())
			setRelationshipsInDeclaration(unit, newOuter, nested);
	}

	private void setSupertype(
			CompilationUnit unit,
			ASTNode typeDecl,
			it.unive.lisa.program.CompilationUnit lisaCU) {
		TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, unit, this);
		typeDecl.accept(typeVisitor);
		it.unive.lisa.type.Type superClassType = typeVisitor.getType();
		if (superClassType != null) {
			UnitType unitType = superClassType.asUnitType();
			if (unitType != null)
				lisaCU.addAncestor(unitType.getUnit());
		}
	}

	private void addGlobals(
			CompilationUnit unit) {
		List<?> types = unit.types();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				addGlobalsInDeclaration((TypeDeclaration) type, null);
			else if (type instanceof EnumDeclaration)
				addEnumConstants((EnumDeclaration) type, null);
	}

	private void addGlobalsInDeclaration(
			TypeDeclaration typeDecl,
			String outer) {
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		if ((typeDecl.isInterface())) {
			JavaInterfaceType interfaceType = JavaInterfaceType.lookup(name);
			addFields(interfaceType.getUnit(), typeDecl);
		} else {
			JavaClassType classType = JavaClassType.lookup(name);
			addFields(classType.getUnit(), typeDecl);
		}
		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		TypeDeclaration[] nested = typeDecl.getTypes();
		for (TypeDeclaration n : nested)
			addGlobalsInDeclaration(n, newOuter);
		for (Object decl : typeDecl.bodyDeclarations())
			if (decl instanceof EnumDeclaration)
				addEnumConstants((EnumDeclaration) decl, newOuter);
	}

	private void addFields(
			it.unive.lisa.program.CompilationUnit unit,
			TypeDeclaration typeDecl) {
		// iterates over inner declarations
		for (Object decl : typeDecl.bodyDeclarations()) {
			Set<String> visitedFieldNames = new HashSet<>();
			if (decl instanceof FieldDeclaration fdecl) {
				FieldDeclarationVisitor visitor = new FieldDeclarationVisitor(parserContext, source, unit,
						compilationUnit,
						visitedFieldNames,
						this);
				fdecl.accept(visitor);
			}
		}
	}

	private void addEnumConstants(
			EnumDeclaration node,
			String outer) {
		String name = getPackage() + (outer == null ? "" : outer + ".") + node.getName().toString();
		EnumUnit enUnit = (EnumUnit) getProgram().getUnit(name);
		Type enumType = JavaClassType.lookup(enUnit.getName());

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
	}

	private void visitUnits(
			CompilationUnit unit) {
		List<?> types = unit.types();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				visitUnitsInDeclaration(unit, (TypeDeclaration) type, null);
			else if (type instanceof EnumDeclaration) {
				EnumDeclaration enumDecl = (EnumDeclaration) type;
				ClassASTVisitor classVisitor = new ClassASTVisitor(parserContext, source, unit, pkg, imports,
						getPackage() + enumDecl.getName().toString());
				enumDecl.accept(classVisitor);
			}
	}

	private void visitUnitsInDeclaration(
			CompilationUnit unit,
			TypeDeclaration typeDecl,
			String outer) {
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		if ((typeDecl.isInterface())) {
			InterfaceASTVisitor interfaceVisitor = new InterfaceASTVisitor(
					parserContext,
					source,
					unit,
					pkg,
					imports,
					name);
			typeDecl.accept(interfaceVisitor);
		} else {
			ClassASTVisitor classVisitor = new ClassASTVisitor(
					parserContext,
					source,
					unit,
					pkg,
					imports,
					name);
			typeDecl.accept(classVisitor);
		}

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		for (TypeDeclaration nested : typeDecl.getTypes())
			visitUnitsInDeclaration(unit, nested, newOuter);
		for (Object decl : typeDecl.bodyDeclarations()) {
			if (decl instanceof EnumDeclaration) {
				name = getPackage() + newOuter + "." + ((EnumDeclaration) decl).getName().toString();
				ClassASTVisitor classVisitor = new ClassASTVisitor(parserContext, source, unit, pkg, imports, name);
				((EnumDeclaration) decl).accept(classVisitor);
			}
		}
	}

	private void addJavaLangImports() {
		imports.put("Appendable", "java.lang.Appendable");
		imports.put("AutoCloseable", "java.lang.AutoCloseable");
		imports.put("CharSequence", "java.lang.CharSequence");
		imports.put("Cloneable", "java.lang.Cloneable");
		imports.put("Comparable", "java.lang.Comparable");
		imports.put("Iterable", "java.lang.Iterable");
		imports.put("Readable", "java.lang.Readable");
		imports.put("Runnable", "java.lang.Runnable");
		imports.put("Thread.UncaughtExceptionHandler", "java.lang.Thread.UncaughtExceptionHandler");
		imports.put("Boolean", "java.lang.Boolean");
		imports.put("Byte", "java.lang.Byte");
		imports.put("Character", "java.lang.Character");
		imports.put("Character.Subset", "java.lang.Character.Subset");
		imports.put("Character.UnicodeBlock", "java.lang.Character.UnicodeBlock");
		imports.put("Class", "java.lang.Class");
		imports.put("ClassLoader", "java.lang.ClassLoader");
		imports.put("ClassValue", "java.lang.ClassValue");
		imports.put("Compiler", "java.lang.Compiler");
		imports.put("Double", "java.lang.Double");
		imports.put("Enum", "java.lang.Enum");
		imports.put("Float", "java.lang.Float");
		imports.put("InheritableThreadLocal", "java.lang.InheritableThreadLocal");
		imports.put("Integer", "java.lang.Integer");
		imports.put("Long", "java.lang.Long");
		imports.put("Math", "java.lang.Math");
		imports.put("Number", "java.lang.Number");
		imports.put("Object", "java.lang.Object");
		imports.put("Package", "java.lang.Package");
		imports.put("Process", "java.lang.Process");
		imports.put("ProcessBuilder", "java.lang.ProcessBuilder");
		imports.put("ProcessBuilder.Redirect", "java.lang.ProcessBuilder.Redirect");
		imports.put("Runtime", "java.lang.Runtime");
		imports.put("RuntimePermission", "java.lang.RuntimePermission");
		imports.put("SecurityManager", "java.lang.SecurityManager");
		imports.put("Short", "java.lang.Short");
		imports.put("StackTraceElement", "java.lang.StackTraceElement");
		imports.put("StrictMath", "java.lang.StrictMath");
		imports.put("String", "java.lang.String");
		imports.put("StringBuffer", "java.lang.StringBuffer");
		imports.put("StringBuilder", "java.lang.StringBuilder");
		imports.put("System", "java.lang.System");
		imports.put("Thread", "java.lang.Thread");
		imports.put("ThreadGroup", "java.lang.ThreadGroup");
		imports.put("ThreadLocal", "java.lang.ThreadLocal");
		imports.put("Throwable", "java.lang.Throwable");
		imports.put("Void", "java.lang.Void");
		imports.put("Character.UnicodeScript", "java.lang.Character.UnicodeScript");
		imports.put("ProcessBuilder.Redirect.Type", "java.lang.ProcessBuilder.Redirect.Type");
		imports.put("Thread.State", "java.lang.Thread.State");
		imports.put("ArithmeticException", "java.lang.ArithmeticException");
		imports.put("ArrayIndexOutOfBoundsException", "java.lang.ArrayIndexOutOfBoundsException");
		imports.put("ArrayStoreException", "java.lang.ArrayStoreException");
		imports.put("ClassCastException", "java.lang.ClassCastException");
		imports.put("ClassNotFoundException", "java.lang.ClassNotFoundException");
		imports.put("CloneNotSupportedException", "java.lang.CloneNotSupportedException");
		imports.put("EnumConstantNotPresentException", "java.lang.EnumConstantNotPresentException");
		imports.put("Exception", "java.lang.Exception");
		imports.put("IllegalAccessException", "java.lang.IllegalAccessException");
		imports.put("IllegalArgumentException", "java.lang.IllegalArgumentException");
		imports.put("IllegalMonitorStateException", "java.lang.IllegalMonitorStateException");
		imports.put("IllegalStateException", "java.lang.IllegalStateException");
		imports.put("IllegalThreadStateException", "java.lang.IllegalThreadStateException");
		imports.put("IndexOutOfBoundsException", "java.lang.IndexOutOfBoundsException");
		imports.put("InstantiationException", "java.lang.InstantiationException");
		imports.put("InterruptedException", "java.lang.InterruptedException");
		imports.put("NegativeArraySizeException", "java.lang.NegativeArraySizeException");
		imports.put("NoSuchFieldException", "java.lang.NoSuchFieldException");
		imports.put("NoSuchMethodException", "java.lang.NoSuchMethodException");
		imports.put("NullPointerException", "java.lang.NullPointerException");
		imports.put("NumberFormatException", "java.lang.NumberFormatException");
		imports.put("ReflectiveOperationException", "java.lang.ReflectiveOperationException");
		imports.put("RuntimeException", "java.lang.RuntimeException");
		imports.put("SecurityException", "java.lang.SecurityException");
		imports.put("StringIndexOutOfBoundsException", "java.lang.StringIndexOutOfBoundsException");
		imports.put("TypeNotPresentException", "java.lang.TypeNotPresentException");
		imports.put("UnsupportedOperationException", "java.lang.UnsupportedOperationException");
		imports.put("AbstractMethodError", "java.lang.AbstractMethodError");
		imports.put("AssertionError", "java.lang.AssertionError");
		imports.put("BootstrapMethodError", "java.lang.BootstrapMethodError");
		imports.put("ClassCircularityError", "java.lang.ClassCircularityError");
		imports.put("ClassFormatError", "java.lang.ClassFormatError");
		imports.put("Error", "java.lang.Error");
		imports.put("ExceptionInInitializerError", "java.lang.ExceptionInInitializerError");
		imports.put("IllegalAccessError", "java.lang.IllegalAccessError");
		imports.put("IncompatibleClassChangeError", "java.lang.IncompatibleClassChangeError");
		imports.put("InstantiationError", "java.lang.InstantiationError");
		imports.put("InternalError", "java.lang.InternalError");
		imports.put("LinkageError", "java.lang.LinkageError");
		imports.put("NoClassDefFoundError", "java.lang.NoClassDefFoundError");
		imports.put("NoSuchFieldError", "java.lang.NoSuchFieldError");
		imports.put("NoSuchMethodError", "java.lang.NoSuchMethodError");
		imports.put("OutOfMemoryError", "java.lang.OutOfMemoryError");
		imports.put("StackOverflowError", "java.lang.StackOverflowError");
		imports.put("ThreadDeath", "java.lang.ThreadDeath");
		imports.put("UnknownError", "java.lang.UnknownError");
		imports.put("UnsatisfiedLinkError", "java.lang.UnsatisfiedLinkError");
		imports.put("UnsupportedClassVersionError", "java.lang.UnsupportedClassVersionError");
		imports.put("VerifyError", "java.lang.VerifyError");
		imports.put("VirtualMachineError", "java.lang.VirtualMachineError");
		imports.put("Deprecated", "java.lang.Deprecated");
		imports.put("FunctionalInterface", "java.lang.FunctionalInterface");
		imports.put("Override", "java.lang.Override");
		imports.put("SafeVarargs", "java.lang.SafeVarargs");
		imports.put("SuppressWarnings", "java.lang.SuppressWarnings");
	}
}

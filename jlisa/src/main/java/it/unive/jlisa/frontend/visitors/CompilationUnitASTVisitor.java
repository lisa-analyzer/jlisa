package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
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
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.VoidType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class CompilationUnitASTVisitor extends BaseUnitASTVisitor {

	private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(CompilationUnitASTVisitor.class);

	public enum VisitorType {
		ADD_UNITS,
		VISIT_UNIT,
		ADD_GLOBALS,
		SET_RELATIONSHIPS,
		INIT_CODE_MEMBERS
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
			addUnits(node, new TreeSet<>());
		} else if (visitorType == VisitorType.SET_RELATIONSHIPS) {
			// phase 2
			setRelationships(node, new TreeSet<>());
		} else if (visitorType == VisitorType.ADD_GLOBALS) {
			// phase 3
			addGlobals(node, new TreeSet<>());
		} else if (visitorType == VisitorType.INIT_CODE_MEMBERS) {
			// phase 4
			initCodeMembers(node, new TreeSet<>());
		} else if (visitorType == VisitorType.VISIT_UNIT) {
			// phase 5
			visitUnits(node, new TreeSet<>());
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
				String importName = i.getName().getFullyQualifiedName();
				Collection<String> libs = LibrarySpecificationProvider
						.getLibrariesOfPackage(importName);
				for (String lib : libs) {
					this.imports.put(lib.substring(lib.lastIndexOf(".") + 1), lib);
					LibrarySpecificationProvider.importClass(getProgram(), lib);
				}

				if (visitorType != VisitorType.ADD_UNITS)
					// we do this only after the first phase,
					// as units get added in that phase
					for (Unit unit : getProgram().getUnits())
						if (getPackage(unit).equals(importName))
							this.imports.put(unit.getName().replace(getPackage(unit), "").substring(1), unit.getName());
			} else {
				String importName = i.getName().getFullyQualifiedName();
				String shortName;
				if (i.getName().isSimpleName())
					shortName = i.getName().getFullyQualifiedName();
				else
					shortName = ((QualifiedName) i.getName()).getName().getFullyQualifiedName();

				this.imports.put(shortName, importName);

				// if we are importing eg "java.util.Map", we want to
				// include also "java.util.Map.Entry"
				// - "java.util.Map.Entry".replace("java.util.Map", "")
				// = ".Entry"
				// - ".Entry".substring(1) = "Entry"
				// so the short name is Map.Entry
				if (visitorType != VisitorType.ADD_UNITS)
					// we do this only after the first phase,
					// as units get added in that phase
					for (Unit unit : getProgram().getUnits())
						if (unit.getName().startsWith(importName + ".")) {
							String libname = shortName + "." + unit.getName().replace(importName, "").substring(1);
							this.imports.put(libname, unit.getName());
						}
				if (LibrarySpecificationProvider.isLibraryAvailable(importName)) {
					LibrarySpecificationProvider.importClass(getProgram(), importName);
					for (String lib : LibrarySpecificationProvider.getNestedUnits(importName)) {
						LibrarySpecificationProvider.importClass(getProgram(), lib);
						String libname = shortName + "." + lib.replace(importName, "").substring(1);
						this.imports.put(libname, lib);
					}
				}
			}
	}

	private String getPackage(
			Unit unit) {
		String name = unit.getName();
		int idx = name.lastIndexOf('.');
		if (idx < 0)
			return "";
		String pkg = name.substring(0, idx);
		Unit outer = getProgram().getUnit(pkg);
		if (outer != null)
			// name points to an inner class, so
			// we have to return the package of the outer class
			return getPackage(outer);
		return pkg;
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
			CompilationUnit unit,
			Set<String> processed) {
		List<?> types = unit.types();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				addUnitsInDeclaration((TypeDeclaration) type, null, processed);
			else if (type instanceof EnumDeclaration)
				addEnumUnit(source, getProgram(), null, (EnumDeclaration) type, processed);
	}

	private void addUnitsInDeclaration(
			TypeDeclaration typeDecl,
			String outer,
			Set<String> processed) {
		if ((typeDecl.isInterface())) {
			if (!buildInterfaceUnit(source, compilationUnit, getProgram(), outer, typeDecl, processed))
				return;
		} else {
			if (!buildClassUnit(source, compilationUnit, getProgram(), outer, typeDecl, processed))
				return;
		}

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		for (TypeDeclaration nested : typeDecl.getTypes())
			addUnitsInDeclaration(nested, newOuter, processed);
		for (Object decl : typeDecl.bodyDeclarations()) {
			if (decl instanceof EnumDeclaration)
				addEnumUnit(source, getProgram(), newOuter, (EnumDeclaration) decl, processed);
			else if (decl instanceof TypeDeclaration)
				addUnitsInDeclaration((TypeDeclaration) decl, newOuter, processed);
		}
	}

	private boolean buildInterfaceUnit(
			String source,
			CompilationUnit unit,
			Program program,
			String outer,
			TypeDeclaration typeDecl,
			Set<String> processed) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

		int modifiers = typeDecl.getModifiers();
		if (Modifier.isFinal(modifiers)) {
			throw new RuntimeException(
					new ProgramValidationException("Illegal combination of modifiers: interface and final"));
		}

		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		if (!processed.add(name))
			return false;
		InterfaceUnit iUnit = new InterfaceUnit(loc, program, name, false);
		program.addUnit(iUnit);
		JavaInterfaceType.register(iUnit.getName(), iUnit);
		return true;
	}

	private boolean buildClassUnit(
			String source,
			CompilationUnit unit,
			Program program,
			String outer,
			TypeDeclaration typeDecl,
			Set<String> processed) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

		int modifiers = typeDecl.getModifiers();
		if (Modifier.isPrivate(modifiers) && outer == null)
			throw new RuntimeException(
					new ProgramValidationException("Modifier private not allowed in a top-level class"));

		ClassUnit cUnit;
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
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

	private void addEnumUnit(
			String source,
			Program program,
			String outer,
			EnumDeclaration typeDecl,
			Set<String> processed) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		if (!processed.add(name))
			return;
		EnumUnit enUnit = new EnumUnit(loc, program, name, true);
		program.addUnit(enUnit);
		JavaClassType.register(enUnit.getName(), enUnit);

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		for (Object decl : typeDecl.bodyDeclarations())
			if (decl instanceof EnumDeclaration)
				addEnumUnit(source, getProgram(), newOuter, (EnumDeclaration) decl, processed);
			else if (decl instanceof TypeDeclaration)
				addUnitsInDeclaration((TypeDeclaration) decl, newOuter, processed);
	}

	private void setRelationships(
			CompilationUnit unit,
			Set<String> processed) {
		List<?> types = unit.types();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				setRelationshipsInDeclaration(unit, null, (TypeDeclaration) type, processed);
			else if (type instanceof EnumDeclaration)
				setEnumRelationships(unit, null, (EnumDeclaration) type, processed);
	}

	private void setRelationshipsInDeclaration(
			CompilationUnit unit,
			String outer,
			TypeDeclaration typeDecl,
			Set<String> processed) {
		it.unive.lisa.program.CompilationUnit lisaCU = null;
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
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
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
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
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
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
		if (isClass && outer != null) {
			// adding the synthetic field for the enclosing instance
			JavaClassType enclosingType = JavaClassType.lookup(getPackage() + outer);
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
			String outer,
			Set<String> processed) {
		String name = getPackage() + (outer == null ? "" : outer + ".") + node.getName().toString();
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

	private void visitUnits(
			CompilationUnit unit,
			Set<String> processed) {
		List<?> types = unit.types();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				visitUnitsInDeclaration(unit, (TypeDeclaration) type, null, null, processed);
			else if (type instanceof EnumDeclaration)
				visitEnumUnit(unit, (EnumDeclaration) type, null, processed);
	}

	private void visitUnitsInDeclaration(
			CompilationUnit unit,
			TypeDeclaration typeDecl,
			String outer,
			ClassASTVisitor enclosing,
			Set<String> processed) {
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		if (!processed.add(name))
			return;
		ClassASTVisitor classVisitor = null;
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
			boolean isStatic = Modifier.isStatic(typeDecl.getModifiers());
			classVisitor = new ClassASTVisitor(
					parserContext,
					source,
					unit,
					pkg,
					imports,
					name,
					// static classes "reset" accessibility to outer instances
					outer == null || isStatic ? null : enclosing,
					outer == null || isStatic ? null : JavaClassType.lookup(getPackage() + outer));
			typeDecl.accept(classVisitor);
		}

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		for (TypeDeclaration nested : typeDecl.getTypes())
			visitUnitsInDeclaration(unit, nested, newOuter, classVisitor, processed);
		for (Object decl : typeDecl.bodyDeclarations())
			if (decl instanceof EnumDeclaration)
				visitEnumUnit(unit, (EnumDeclaration) decl, newOuter, processed);
			else if (decl instanceof TypeDeclaration)
				visitUnitsInDeclaration(unit, (TypeDeclaration) decl, newOuter, classVisitor, processed);
	}

	private void visitEnumUnit(
			CompilationUnit unit,
			EnumDeclaration enumDecl,
			String outer,
			Set<String> processed) {
		String name = getPackage() + (outer == null ? "" : outer + ".") + enumDecl.getName().toString();
		if (!processed.add(name))
			return;
		ClassASTVisitor classVisitor = new ClassASTVisitor(parserContext, source, unit, pkg, imports, name, null, null);
		enumDecl.accept(classVisitor);

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? enumDecl.getName().toString() : outer + "." + enumDecl.getName().toString();
		for (Object decl : enumDecl.bodyDeclarations())
			if (decl instanceof EnumDeclaration)
				visitEnumUnit(unit, (EnumDeclaration) decl, newOuter, processed);
			else if (decl instanceof TypeDeclaration)
				visitUnitsInDeclaration(unit, (TypeDeclaration) decl, newOuter, null, processed);
	}

	private void initCodeMembers(
			CompilationUnit unit,
			Set<String> processed) {
		List<?> types = unit.types();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				initCodeMembersInDeclaration(unit, (TypeDeclaration) type, null, processed);
			else if (type instanceof EnumDeclaration)
				initCodeMembersInEnum(unit, (EnumDeclaration) type, null, processed);
	}

	private void initCodeMembersInDeclaration(
			CompilationUnit unit,
			TypeDeclaration typeDecl,
			String outer,
			Set<String> processed) {
		String name = getPackage() + (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		if (!processed.add(name))
			return;

		it.unive.lisa.program.CompilationUnit lisaCU = null;
		if ((typeDecl.isInterface()))
			lisaCU = JavaInterfaceType.lookup(name).getUnit();
		else
			lisaCU = JavaClassType.lookup(name).getUnit();

		boolean isStatic = Modifier.isStatic(typeDecl.getModifiers());
		JavaClassType enclosing = outer == null || isStatic ? null : JavaClassType.lookup(getPackage() + outer);

		for (MethodDeclaration methodsDecl : typeDecl.getMethods()) {
			CodeMemberDescriptor codeMemberDescriptor;
			if (methodsDecl.isConstructor()) {
				codeMemberDescriptor = buildConstructorJavaCodeMemberDescriptor(methodsDecl, enclosing, lisaCU);
			} else {
				codeMemberDescriptor = buildJavaCodeMemberDescriptor(methodsDecl, lisaCU);
			}
			boolean isMain = isMain(methodsDecl);
			int modifiers = methodsDecl.getModifiers();
			CFG cfg = new CFG(codeMemberDescriptor);
			boolean added;
			if (!Modifier.isStatic(modifiers)) {
				added = lisaCU.addInstanceCodeMember(cfg);
			} else {
				added = lisaCU.addCodeMember(cfg);
			}
			if (!added)
				throw new ParsingException("duplicated_method_descriptor",
						ParsingException.Type.MALFORMED_SOURCE,
						"Duplicate descriptor " + cfg.getDescriptor() + " in unit " + lisaCU.getName(),
						getSourceCodeLocation(methodsDecl));

			if (isMain)
				getProgram().addEntryPoint(cfg);
		}

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		TypeDeclaration[] nested = typeDecl.getTypes();
		for (TypeDeclaration n : nested)
			initCodeMembersInDeclaration(unit, n, newOuter, processed);
		for (Object decl : typeDecl.bodyDeclarations())
			if (decl instanceof TypeDeclaration)
				initCodeMembersInDeclaration(unit, (TypeDeclaration) decl, newOuter, processed);
			else if (decl instanceof EnumDeclaration)
				initCodeMembersInEnum(unit, (EnumDeclaration) decl, newOuter, processed);
	}

	private void initCodeMembersInEnum(
			CompilationUnit unit,
			EnumDeclaration node,
			String outer,
			Set<String> processed) {
		String name = getPackage() + (outer == null ? "" : outer + ".") + node.getName().toString();
		if (!processed.add(name))
			return;
		EnumUnit enUnit = (EnumUnit) getProgram().getUnit(name);

		for (Object decl : node.bodyDeclarations())
			if (decl instanceof MethodDeclaration) {
				MethodDeclaration methodsDecl = (MethodDeclaration) decl;
				CodeMemberDescriptor codeMemberDescriptor = buildJavaCodeMemberDescriptor(methodsDecl, enUnit);
				boolean isMain = isMain(methodsDecl);
				int modifiers = methodsDecl.getModifiers();
				CFG cfg = new CFG(codeMemberDescriptor);
				boolean added;
				if (!Modifier.isStatic(modifiers)) {
					added = enUnit.addInstanceCodeMember(cfg);
				} else {
					added = enUnit.addCodeMember(cfg);
				}
				if (!added)
					throw new ParsingException("duplicated_method_descriptor",
							ParsingException.Type.MALFORMED_SOURCE,
							"Duplicate descriptor " + cfg.getDescriptor() + " in unit " + enUnit.getName(),
							getSourceCodeLocation(methodsDecl));

				if (isMain)
					getProgram().addEntryPoint(cfg);
			}

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? node.getName().toString() : outer + "." + node.getName().toString();
		for (Object decl : node.bodyDeclarations())
			if (decl instanceof TypeDeclaration)
				initCodeMembersInDeclaration(unit, (TypeDeclaration) decl, newOuter, processed);
			else if (decl instanceof EnumDeclaration)
				initCodeMembersInEnum(unit, (EnumDeclaration) decl, newOuter, processed);
	}

	private JavaCodeMemberDescriptor buildJavaCodeMemberDescriptor(
			MethodDeclaration node,
			it.unive.lisa.program.CompilationUnit lisaCU) {
		CodeLocation loc = getSourceCodeLocation(node);
		JavaCodeMemberDescriptor codeMemberDescriptor;
		boolean instance = !Modifier.isStatic(node.getModifiers());

		it.unive.lisa.type.Type returnType = null;

		// the method is generic
		if (node.typeParameters().stream().filter(tp -> tp.toString().equals(node.getReturnType2().toString()))
				.count() > 0)
			returnType = JavaClassType.getObjectType();
		// the method is not generic, but the class it is
		else {
			List topLevelTypes = compilationUnit.types();
			for (Object tlType : topLevelTypes) {
				if (tlType instanceof TypeDeclaration)
					if (((TypeDeclaration) tlType).typeParameters().stream()
							.filter(tp -> tp.toString().equals(node.getReturnType2().toString())).count() > 0)
						returnType = JavaClassType.getObjectType();
			}

			if (returnType == null) {
				TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit, this);
				node.getReturnType2().accept(typeVisitor);
				returnType = typeVisitor.getType();
			}
		}

		List<Parameter> parameters = new ArrayList<>();
		if (instance) {
			it.unive.lisa.type.Type type = getProgram().getTypes().getType(lisaCU.getName());
			parameters.add(new Parameter(getSourceCodeLocation(node), "this", new JavaReferenceType(type), null,
					new Annotations()));
		}

		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(parserContext, source,
					compilationUnit, this);
			sd.accept(vd);
			parameters.add(vd.getParameter());
		}

		// TODO annotations
		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, lisaCU, instance,
				node.getName().getIdentifier(),
				returnType.isInMemoryType() ? new JavaReferenceType(returnType) : returnType, annotations, paramArray);
		if (node.isConstructor() || Modifier.isStatic(node.getModifiers())) {
			codeMemberDescriptor.setOverridable(false);
		} else {
			codeMemberDescriptor.setOverridable(true);
		}

		return codeMemberDescriptor;
	}

	private JavaCodeMemberDescriptor buildConstructorJavaCodeMemberDescriptor(
			MethodDeclaration node,
			JavaClassType enclosing,
			it.unive.lisa.program.CompilationUnit lisaCU) {

		CodeLocation loc = getSourceCodeLocation(node);
		JavaCodeMemberDescriptor codeMemberDescriptor;
		boolean instance = !Modifier.isStatic(node.getModifiers());
		it.unive.lisa.type.Type type = getProgram().getTypes().getType(lisaCU.getName());

		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Parameter(getSourceCodeLocation(node), "this", new JavaReferenceType(type), null,
				new Annotations()));

		if (enclosing != null)
			parameters.add(new Parameter(getSourceCodeLocationManager(node).nextColumn(), "$enclosing",
					enclosing.getReference(),
					null, new Annotations()));

		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(parserContext, source,
					compilationUnit, this);
			sd.accept(vd);
			parameters.add(vd.getParameter());
		}

		// TODO annotations
		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, lisaCU, instance,
				node.getName().getIdentifier(), VoidType.INSTANCE, annotations, paramArray);
		if (node.isConstructor() || Modifier.isStatic(node.getModifiers())) {
			codeMemberDescriptor.setOverridable(false);
		} else {
			codeMemberDescriptor.setOverridable(true);
		}

		return codeMemberDescriptor;
	}

	private boolean isMain(
			MethodDeclaration node) {
		if (!Modifier.isStatic(node.getModifiers())) {
			return false;
		}
		if (!node.getName().getIdentifier().equals("main")) {
			return false;
		}
		if (node.getReceiverType() != null) {
			return false;
		}
		if (node.parameters().size() != 1) {
			return false;
		}
		SingleVariableDeclaration parameter = (SingleVariableDeclaration) node.parameters().getFirst();
		org.eclipse.jdt.core.dom.Type type = parameter.getType();
		if (parameter.getType().toString().equals("String[]")) {
			return true;
		}
		if (type instanceof SimpleType && ((SimpleType) type).getName().toString().equals("String")
				&& parameter.getExtraDimensions() == 1) {
			return true;
		}

		return false;
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

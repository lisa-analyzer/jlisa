package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.lisa.program.*;
import it.unive.lisa.type.UnitType;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompilationUnitASTVisitor extends JavaASTVisitor {
	public enum VisitorType {
		ADD_UNITS,
		VISIT_UNIT,
		SET_RELATIONSHIPS
	}
	public VisitorType visitorType;
	public CompilationUnitASTVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit unit,
			VisitorType visitorType) {
		super(parserContext, source, unit);
		this.visitorType = visitorType;
	}

	@Override
	public boolean visit(
			CompilationUnit node) {
		if (visitorType == VisitorType.VISIT_UNIT) {
			visitUnits(node);
		} else if (visitorType == VisitorType.ADD_UNITS) {
			addUnits(node);
		} else if(visitorType == VisitorType.SET_RELATIONSHIPS) {
			setRelationships(node);
		}
		return false;
	}

	private void setRelationships(CompilationUnit unit) {
		List<?> types = unit.types();
		for (Object type : types) {
			if (type instanceof TypeDeclaration) {
				TypeDeclaration typeDecl = (TypeDeclaration) type;
				it.unive.lisa.program.CompilationUnit lisaCU = null;
				if (typeDecl.isInterface()) {
					lisaCU = JavaInterfaceType.lookup(typeDecl.getName().getIdentifier(), null).getUnit();
				}
				else {
					lisaCU = JavaClassType.lookup(typeDecl.getName().getIdentifier(), null).getUnit();
				}
				if (typeDecl.getSuperclassType() != null) {
					TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, unit);
					typeDecl.getSuperclassType().accept(typeVisitor);
					it.unive.lisa.type.Type superClassType = typeVisitor.getType();
					if (superClassType != null) {
						UnitType unitType = superClassType.asUnitType();
						if (unitType != null) {
							lisaCU.addAncestor(unitType.getUnit());
						}
					}
				}
				for (Object oInterfaceType : typeDecl.superInterfaceTypes()) {
					TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, unit);
					((ASTNode)oInterfaceType).accept(typeVisitor);
					it.unive.lisa.type.Type superInterfaceClassType = typeVisitor.getType();
					if (superInterfaceClassType != null) {
						UnitType unitType = superInterfaceClassType.asUnitType();
						if (unitType != null) {
							lisaCU.addAncestor(unitType.getUnit());
						}
					}
				}
			}
		}
	}


	private void addUnits(
			CompilationUnit unit) {
		List<?> types = unit.types();
		for (Object type : types) {
			if (type instanceof TypeDeclaration) {
				TypeDeclaration typeDecl = (TypeDeclaration) type;
				if ((typeDecl.isInterface())) {
					InterfaceUnit iUnit = buildInterfaceUnit(source, unit, getProgram(), typeDecl);
					JavaInterfaceType.lookup(iUnit.getName(), iUnit);
				} else {
					ClassUnit cUnit = buildClassUnit(source, unit, getProgram(), typeDecl);
					JavaClassType.lookup(cUnit.getName(), cUnit);
					populateClassUnit(cUnit, typeDecl);
				}

			} else if (type instanceof EnumDeclaration) {
				EnumUnit emUnit = buildEnumUnit(source, getProgram(), (EnumDeclaration) type);
				JavaClassType.lookup(emUnit.getName(), emUnit);
			}
		}
	}

	private void visitUnits(
			CompilationUnit unit) {
		List<?> types = unit.types();
		for (Object type : types) {
			if (type instanceof TypeDeclaration) {
				TypeDeclaration typeDecl = (TypeDeclaration) type;
				if ((typeDecl.isInterface())) {
					InterfaceASTVisitor interfaceVisitor = new InterfaceASTVisitor(parserContext, source, unit);
					typeDecl.accept(interfaceVisitor);
				} else {
					ClassASTVisitor classVisitor = new ClassASTVisitor(parserContext, source, unit);
					typeDecl.accept(classVisitor);
				}
			} else if (type instanceof EnumDeclaration) {
				ClassASTVisitor classVisitor = new ClassASTVisitor(parserContext, source, unit);
				((EnumDeclaration) type).accept(classVisitor);
			}
		}

	}

	private InterfaceUnit buildInterfaceUnit(
			String source,
			CompilationUnit unit,
			Program program,
			TypeDeclaration typeDecl) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

		int modifiers = typeDecl.getModifiers();
		if (Modifier.isFinal(modifiers)) {
			throw new RuntimeException(
					new ProgramValidationException("Illegal combination of modifiers: interface and final"));
		}

		InterfaceUnit iUnit = new InterfaceUnit(loc, program, typeDecl.getName().toString(), false);
		program.addUnit(iUnit);
		return iUnit;
	}

	private ClassUnit buildClassUnit(
			String source,
			CompilationUnit unit,
			Program program,
			TypeDeclaration typeDecl) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

		int modifiers = typeDecl.getModifiers();
		if (Modifier.isPrivate(modifiers) && !(typeDecl.getParent() instanceof CompilationUnit)) {
			throw new RuntimeException(
					new ProgramValidationException("Modifier private not allowed in a top-level class"));
		}
		ClassUnit cUnit;
		if (Modifier.isAbstract(modifiers)) {
			if (Modifier.isFinal(modifiers)) {
				throw new RuntimeException(
						new ProgramValidationException("illegal combination of modifiers: abstract and final"));
			}
			cUnit = new AbstractClassUnit(loc, program, typeDecl.getName().toString(), Modifier.isFinal(modifiers));
		} else {
			cUnit = new ClassUnit(loc, program, typeDecl.getName().toString(), Modifier.isFinal(modifiers));
		}

		program.addUnit(cUnit);
		return cUnit;
	}

	private void populateClassUnit(it.unive.lisa.program.CompilationUnit unit, TypeDeclaration typeDecl) {
		// iterates over inner declarations
		for (Object decl : typeDecl.bodyDeclarations()) {
			// enum inner declaration
			if (decl instanceof EnumDeclaration) {
				EnumDeclaration innerEnum = (EnumDeclaration) decl;
				EnumUnit emUnit = buildEnumUnit(source, getProgram(), innerEnum);
				JavaClassType.lookup(emUnit.getName(), emUnit);
			}

			Set<String> visitedFieldNames = new HashSet<>();
			if (decl instanceof FieldDeclaration fdecl) {
				FieldDeclarationVisitor visitor = new FieldDeclarationVisitor(parserContext, source, unit, compilationUnit,
						visitedFieldNames);
				fdecl.accept(visitor);
			}
		}
	}


	private EnumUnit buildEnumUnit(
			String source,
			Program program,
			EnumDeclaration typeDecl) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);
		EnumUnit enUnit = new EnumUnit(loc, program, typeDecl.getName().toString(), true);
		program.addUnit(enUnit);
		return enUnit;
	}
}

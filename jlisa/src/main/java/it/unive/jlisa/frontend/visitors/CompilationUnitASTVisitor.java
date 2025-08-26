package it.unive.jlisa.frontend.visitors;

import java.util.List;

import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.lisa.program.AbstractClassUnit;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.SourceCodeLocation;

public class CompilationUnitASTVisitor extends JavaASTVisitor {
	private boolean visitUnit;
	
	public CompilationUnitASTVisitor(ParserContext parserContext, String source, CompilationUnit unit, boolean visitUnit) {
		super(parserContext, source, unit);
		this.visitUnit = visitUnit;
	}

	@Override
	public boolean visit(CompilationUnit node) {    	
		if (visitUnit) {
			visitUnits(node);
		} else {
			addUnits(node);
		}
		return false;
	}

	private void addUnits(CompilationUnit unit) {
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
				}

			} else if (type instanceof EnumDeclaration) {
				EnumUnit emUnit = buildEnumUnit(source, getProgram(), (EnumDeclaration) type);
				JavaClassType.lookup(emUnit.getName(), emUnit);
			}

			if (type instanceof AnnotationTypeDeclaration) {
				parserContext.addException(new ParsingException("annotation-type-declaration", ParsingException.Type.UNSUPPORTED_STATEMENT, "Annotation Type Declarations are not supported.", getSourceCodeLocation((AnnotationTypeDeclaration)type)));
			}
		}
	}

	private void visitUnits(CompilationUnit unit) {
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
			
			if (type instanceof AnnotationTypeDeclaration) {
				parserContext.addException(new ParsingException("annotation-type-declaration", ParsingException.Type.UNSUPPORTED_STATEMENT, "Annotation Type Declarations are not supported.", getSourceCodeLocation((AnnotationTypeDeclaration)type)));
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
			cUnit = new ClassUnit(loc, program, typeDecl.getName().toString(), Modifier.isFinal(modifiers));
		}

		// iterates over inner declarations
		for (Object decl : typeDecl.bodyDeclarations()) {
			// enum inner declaration
			if (decl instanceof EnumDeclaration) {
				EnumDeclaration innerEnum = (EnumDeclaration) decl;
				EnumUnit emUnit = buildEnumUnit(source, program, innerEnum);
				JavaClassType.lookup(emUnit.getName(), emUnit);
			}
		}
		
        program.addUnit(cUnit);
		return cUnit;
	}
	
	private EnumUnit buildEnumUnit(String source, Program program, EnumDeclaration typeDecl) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);
		EnumUnit enUnit = new EnumUnit(loc, program, typeDecl.getName().toString(), true);
		program.addUnit(enUnit);
		return enUnit;
	}
}

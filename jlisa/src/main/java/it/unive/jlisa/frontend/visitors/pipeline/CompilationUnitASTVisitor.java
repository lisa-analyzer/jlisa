package it.unive.jlisa.frontend.visitors.pipeline;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.util.FQNUtils;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.scope.ClassScope;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.frontend.visitors.structure.ClassASTVisitor;
import it.unive.jlisa.frontend.visitors.structure.InterfaceASTVisitor;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class CompilationUnitASTVisitor extends ScopedVisitor<UnitScope> {

	private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(CompilationUnitASTVisitor.class);

	public CompilationUnitASTVisitor(
			ParsingEnvironment environment,
			UnitScope scope) {
		super(environment, scope);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean visit(
			CompilationUnit node) {
		List<?> types = node.types();
		Set<String> processed = new TreeSet<>();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				visitUnitsInDeclaration(node, (TypeDeclaration) type, null, null, null, processed);
			else if (type instanceof EnumDeclaration)
				visitEnumUnit(node, (EnumDeclaration) type, null, null, processed);
		return false;
	}

	private void visitUnitsInDeclaration(
			CompilationUnit unit,
			TypeDeclaration typeDecl,
			String outer,
			ClassASTVisitor enclosing,
			ClassScope enclosingClassScope,
			Set<String> processed) {
		String name = FQNUtils.buildFQN(getScope().getPackage(), outer, typeDecl.getName().toString());

		it.unive.lisa.program.CompilationUnit cUnit = (it.unive.lisa.program.CompilationUnit) getProgram()
				.getUnit(name);
		if (!processed.add(name))
			return;
		ClassASTVisitor classVisitor = null;
		boolean isStatic = Modifier.isStatic(typeDecl.getModifiers());
		JavaClassType enclosingType = null;
		if (!isStatic && enclosingClassScope != null && enclosingClassScope.getLisaClassUnit() instanceof ClassUnit)
			enclosingType = JavaClassType.lookup(enclosingClassScope.getLisaClassUnit().getName());
		ClassScope scope = null;
		scope = new ClassScope(getScope(), enclosingClassScope, enclosingType, cUnit);
		if ((typeDecl.isInterface())) {
			InterfaceASTVisitor interfaceVisitor = new InterfaceASTVisitor(
					getParserContext(),
					getSource(),
					unit,
					getScope().getPackage(),
					getScope().getExplicitImports(),
					name);
			typeDecl.accept(interfaceVisitor);
		} else {
			classVisitor = new ClassASTVisitor(
					getEnvironment(), scope);

			typeDecl.accept(classVisitor);
		}

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		for (TypeDeclaration nested : typeDecl.getTypes())
			visitUnitsInDeclaration(unit, nested, newOuter, classVisitor, scope, processed);
		for (Object decl : typeDecl.bodyDeclarations())
			if (decl instanceof EnumDeclaration)
				visitEnumUnit(unit, (EnumDeclaration) decl, enclosingClassScope, newOuter, processed);
			else if (decl instanceof TypeDeclaration)
				visitUnitsInDeclaration(unit, (TypeDeclaration) decl, newOuter, classVisitor, scope, processed);
	}

	private void visitEnumUnit(
			CompilationUnit unit,
			EnumDeclaration enumDecl,
			ClassScope enclosingClassScope,
			String outer,
			Set<String> processed) {
		String name = FQNUtils.buildFQN(getScope().getPackage(), outer, enumDecl.getName().toString());
		ClassUnit cUnit = (ClassUnit) getProgram().getUnit(name);
		if (!processed.add(name))
			return;
		ClassScope scope = null;
		if (enclosingClassScope == null) {
			scope = getScope().toClassScope(null, cUnit);
		} else {
			JavaClassType enclosingEnumType = null;
			if (enclosingClassScope.getLisaClassUnit() instanceof ClassUnit)
				enclosingEnumType = JavaClassType.lookup(enclosingClassScope.getLisaClassUnit().getName());
			scope = new ClassScope(getScope(), enclosingClassScope, enclosingEnumType, cUnit);
		}
		ClassASTVisitor classVisitor = new ClassASTVisitor(getEnvironment(), scope);
		enumDecl.accept(classVisitor);

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? enumDecl.getName().toString() : outer + "." + enumDecl.getName().toString();
		for (Object decl : enumDecl.bodyDeclarations())
			if (decl instanceof EnumDeclaration)
				visitEnumUnit(unit, (EnumDeclaration) decl, scope, newOuter, processed);
			else if (decl instanceof TypeDeclaration)
				visitUnitsInDeclaration(unit, (TypeDeclaration) decl, newOuter, null, scope, processed);
	}
}

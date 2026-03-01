package it.unive.jlisa.frontend.visitors.pipeline;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.util.FQNUtils;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.lisa.program.*;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class PopulateUnitsASTVisitor extends ScopedVisitor<UnitScope> {

	public PopulateUnitsASTVisitor(
			ParsingEnvironment env,
			UnitScope scope) {
		super(env, scope);
	}

	public boolean visit(
			CompilationUnit node) {
		List<?> types = node.types();
		Set<String> processed = new TreeSet<>();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				addUnitsInDeclaration((TypeDeclaration) type, null, processed);
			else if (type instanceof EnumDeclaration)
				addEnumUnit(getParserContext().getProgram(), null, (EnumDeclaration) type, processed);

		return false;
	}

	private void addUnits(
			CompilationUnit unit,
			Set<String> processed) {
		List<?> types = unit.types();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				addUnitsInDeclaration((TypeDeclaration) type, null, processed);
			else if (type instanceof EnumDeclaration)
				addEnumUnit(getProgram(), null, (EnumDeclaration) type, processed);
	}

	private boolean buildInterfaceUnit(
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

		String name = FQNUtils.buildFQN(getScope().getPackage(), outer, typeDecl.getName().toString());
		if (!processed.add(name))
			return false;
		InterfaceUnit iUnit = new InterfaceUnit(loc, program, name, false);
		program.addUnit(iUnit);
		JavaInterfaceType.register(iUnit.getName(), iUnit);
		return true;
	}

	private boolean buildClassUnit(
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
		String name;

		name = FQNUtils.buildFQN(getScope().getPackage(), outer, typeDecl.getName().toString());
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

	private void addUnitsInDeclaration(
			TypeDeclaration typeDecl,
			String outer,
			Set<String> processed) {
		if ((typeDecl.isInterface())) {
			if (!buildInterfaceUnit(getProgram(), outer, typeDecl, processed))
				return;
		} else {
			if (!buildClassUnit(getProgram(), outer, typeDecl, processed))
				return;
		}

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		for (TypeDeclaration nested : typeDecl.getTypes())
			addUnitsInDeclaration(nested, newOuter, processed);
		for (Object decl : typeDecl.bodyDeclarations()) {
			if (decl instanceof EnumDeclaration)
				addEnumUnit(getProgram(), newOuter, (EnumDeclaration) decl, processed);
			else if (decl instanceof TypeDeclaration)
				addUnitsInDeclaration((TypeDeclaration) decl, newOuter, processed);
		}
	}

	private void addEnumUnit(
			Program program,
			String outer,
			EnumDeclaration typeDecl,
			Set<String> processed) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);
		String name = FQNUtils.buildFQN(getScope().getPackage(), outer, typeDecl.getName().toString());
		if (!processed.add(name))
			return;
		EnumUnit enUnit = new EnumUnit(loc, program, name, true);
		program.addUnit(enUnit);
		JavaClassType.register(enUnit.getName(), enUnit);

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		for (Object decl : typeDecl.bodyDeclarations())
			if (decl instanceof EnumDeclaration)
				addEnumUnit(getProgram(), newOuter, (EnumDeclaration) decl, processed);
			else if (decl instanceof TypeDeclaration)
				addUnitsInDeclaration((TypeDeclaration) decl, newOuter, processed);
	}
}

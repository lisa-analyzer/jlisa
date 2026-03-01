package it.unive.jlisa.frontend.visitors.scope;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.CompilationUnit;

public final class UnitScope extends Scope {
	private final String pkg;
	private final Map<String, String> explicitImports;
	private final Set<String> onDemandPackages = new HashSet<>();
	private final Map<String, it.unive.lisa.program.CompilationUnit> localTypes = new HashMap<>();
	private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(UnitScope.class);

	public UnitScope(
			String pkg,
			Map<String, String> explicitImports) {
		this.pkg = pkg != null ? pkg : "";
		this.explicitImports = explicitImports;
	}

	public String getPackage() {
		return pkg;
	}

	public Map<String, String> getExplicitImports() {
		return explicitImports;
	}

	public ClassScope toClassScope(
			JavaClassType enclosingClass,
			ClassUnit lisaUnit) {
		return new ClassScope(this, null, enclosingClass, lisaUnit);
	}

	public static UnitScope init(
			ParsingEnvironment environment,
			CompilationUnit cu) {
		String pkg = cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName() : null;
		UnitScope scope = new UnitScope(pkg, new HashMap<>());

		// Step 1: add java.lang imports
		addJavaLangImports(scope);

		// Step 2: process explicit and on-demand imports
		processImports(scope, environment, cu.imports());

		// Step 3: add local types (classes + enums) as explicit imports and
		// register in scope
		addLocalTypes(scope, environment, cu);

		return scope;
	}

	private void addLocalType(
			String string,
			it.unive.lisa.program.CompilationUnit unit) {
		this.localTypes.put(string, unit);
	}

	public it.unive.lisa.program.CompilationUnit getLocalType(
			String name) {
		return localTypes.get(name);
	}

	public Set<String> getOnDemandPackages() {
		return onDemandPackages;
	}

	/**
	 * Adds all local types (classes and enums) declared in this compilation
	 * unit to the scope as explicit imports, and registers nested types
	 * recursively.
	 */
	private static void addLocalTypes(
			UnitScope scope,
			ParsingEnvironment env,
			CompilationUnit cu) {
		for (Object type : cu.types()) {
			if (type instanceof TypeDeclaration td) {
				addTypeDeclaration(scope, env, td, null);
			} else if (type instanceof EnumDeclaration ed) {
				String fqn = (scope.getPackage().isEmpty() ? "" : scope.getPackage() + ".") + ed.getName().toString();
				scope.addExplicitImport(ed.getName().toString(), fqn);
				scope.addLocalType(ed.getName().toString(),
						(it.unive.lisa.program.CompilationUnit) env.parserContext().getProgram().getUnit(fqn));
			}
		}
	}

	private void addExplicitImport(
			String string,
			String fqn) {
		this.explicitImports.put(string, fqn);
	}

	/**
	 * Recursively adds a TypeDeclaration (class/interface) and its nested types
	 * to the scope.
	 */
	private static void addTypeDeclaration(
			UnitScope scope,
			ParsingEnvironment env,
			TypeDeclaration td,
			String outer) {
		String name = (scope.getPackage().isEmpty() ? "" : scope.getPackage() + ".")
				+ (outer == null ? "" : outer + ".") + td.getName().toString();

		// Register as explicit import
		scope.addExplicitImport(td.getName().toString(), name);

		// Look up the already-registered Unit (created by
		// PopulateUnitsASTVisitor)
		scope.addLocalType(td.getName().toString(),
				(it.unive.lisa.program.CompilationUnit) env.parserContext().getProgram().getUnit(name));

		// Handle nested types
		String newOuter = outer == null ? td.getName().toString() : outer + "." + td.getName().toString();
		for (TypeDeclaration nested : td.getTypes()) {
			addTypeDeclaration(scope, env, nested, newOuter);
		}

		for (Object decl : td.bodyDeclarations()) {
			if (decl instanceof EnumDeclaration ed) {
				String fqn = (scope.getPackage().isEmpty() ? "" : scope.getPackage() + ".") + newOuter + "."
						+ ed.getName().toString();
				scope.addExplicitImport(ed.getName().toString(), fqn);
				scope.addLocalType(ed.getName().toString(),
						(it.unive.lisa.program.CompilationUnit) env.parserContext().getProgram().getUnit(fqn));
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

	private void addOnDemandPackage(
			String importName) {
		onDemandPackages.add(importName);
	}

	private static void addJavaLangImports(
			UnitScope scope) {
		try (InputStream is = UnitScope.class.getResourceAsStream("/java-lang-imports.txt");
				BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.strip();
				if (!line.isEmpty())
					scope.getExplicitImports().put(line, "java.lang." + line);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load java.lang imports from resource file", e);
		}
	}

}
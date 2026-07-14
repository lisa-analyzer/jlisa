package it.unive.jlisa.frontend.visitors.expression;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.util.FQNUtils;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.program.libraries.loader.extensions.CompileTimeGlobal;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import org.eclipse.jdt.core.dom.*;

public class QualifiedNameVisitor extends ScopedVisitor<UnitScope> implements ResultHolder<Expression> {

	private Expression result;

	public QualifiedNameVisitor(
			ParsingEnvironment env,
			UnitScope scope) {
		super(env, scope);
	}

	@Override
	public boolean visit(
			QualifiedName node) {
		Unit unit = TypeASTVisitor.getUnit(node.getQualifier().getFullyQualifiedName(), getProgram(), getScope());
		if (unit == null)
			return false;

		String fieldName = node.getName().getIdentifier();

		result = resolveFromSource(fieldName, unit);
		if (result == null)
			result = resolveFromLibrary(node, fieldName, unit);

		return false;
	}

	private Expression resolveFromSource(
			String fieldName,
			Unit unit) {
		CompilationUnit[] allASTUnits = getEnvironment().allAstUnits();

		Expression initializer = null;
		if (allASTUnits != null)
			for (CompilationUnit cu : allASTUnits)
				for (Object t : cu.types())
					if (t instanceof TypeDeclaration td && FQNUtils.buildFQN(cu, td).equals(unit.getName()))
						for (FieldDeclaration fd : td.getFields()) {
							if (!Modifier.isStatic(fd.getModifiers()) && !td.isInterface())
								continue;

							for (Object f : fd.fragments()) {
								VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;

								if (fragment.getName().getIdentifier().equals(fieldName))
									initializer = fragment.getInitializer();
							}
						}

		return initializer;
	}

	private Expression resolveFromLibrary(
			QualifiedName node,
			String fieldName,
			Unit unit) {
		Global global = getEnvironment().parserContext().getGlobal(unit, fieldName, false);

		if (global instanceof CompileTimeGlobal compileTimeGlobal) {
			Object raw = compileTimeGlobal.getDefaultValue().getValue();
			AST ast = node.getAST();

			return switch (raw) {
			case String s -> {
				StringLiteral literal = ast.newStringLiteral();
				literal.setLiteralValue(s);
				yield literal;
			}
			case Boolean b -> ast.newBooleanLiteral(b);
			case Number n -> ast.newNumberLiteral(n.toString());
			case null, default -> null;
			};
		}

		return null;
	}

	@Override
	public Expression getResult() {
		return result;
	}
}

package it.unive.jlisa.frontend.visitors.expression;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.util.FQNUtils;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.lisa.program.Unit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

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

		if (unit == null || getEnvironment().allAstUnits() == null)
			return false;

		String fieldName = node.getName().getIdentifier();

		for (CompilationUnit cu : getEnvironment().allAstUnits())
			for (Object t : cu.types())
				if (t instanceof TypeDeclaration td && FQNUtils.buildFQN(cu, td).equals(unit.getName()))
					for (FieldDeclaration fd : td.getFields()) {
						if (!Modifier.isStatic(fd.getModifiers()) && !td.isInterface()) continue;

						for (Object f : fd.fragments()) {
							VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
							if (fragment.getName().getIdentifier().equals(fieldName))
								result = fragment.getInitializer();
						}
					}

		return false;
	}

	@Override
	public Expression getResult() {
		return result;
	}
}

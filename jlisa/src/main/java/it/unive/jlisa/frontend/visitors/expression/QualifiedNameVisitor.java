package it.unive.jlisa.frontend.visitors.expression;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import org.eclipse.jdt.core.dom.QualifiedName;

public class QualifiedNameVisitor extends ScopedVisitor<UnitScope> implements ResultHolder<Global> {

	private Global result;

	public QualifiedNameVisitor(
			ParsingEnvironment env,
			UnitScope scope) {
		super(env, scope);
	}

	@Override
	public boolean visit(
			QualifiedName node) {
		Unit unit = TypeASTVisitor.getUnit(node.getQualifier().getFullyQualifiedName(), getProgram(), getScope());
		if (unit != null)
			result = getParserContext().getGlobal(unit, node.getName().getIdentifier(), false);
		return false;
	}

	@Override
	public Global getResult() {
		return result;
	}
}

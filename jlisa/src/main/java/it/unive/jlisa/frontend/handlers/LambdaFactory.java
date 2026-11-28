package it.unive.jlisa.frontend.handlers;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.lisa.program.cfg.statement.Expression;
import org.eclipse.jdt.core.dom.LambdaExpression;

public interface LambdaFactory {
	Expression create(
			LambdaExpression node,
			ParsingEnvironment environment,
			MethodScope scope);
}

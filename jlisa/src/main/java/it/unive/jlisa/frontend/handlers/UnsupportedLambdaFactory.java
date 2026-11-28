package it.unive.jlisa.frontend.handlers;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.lisa.program.cfg.statement.Expression;
import org.eclipse.jdt.core.dom.LambdaExpression;

public class UnsupportedLambdaFactory implements LambdaFactory {

	@Override
	public Expression create(
			LambdaExpression node,
			ParsingEnvironment environment,
			MethodScope scope) {
		throw new ParsingException("lambda-expression",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Lambda expressions are not supported.",
				environment.getSourceCodeLocation(node));
	}
}

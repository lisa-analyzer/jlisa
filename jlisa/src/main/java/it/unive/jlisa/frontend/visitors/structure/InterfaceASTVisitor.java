package it.unive.jlisa.frontend.visitors.structure;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.visitors.scope.ClassScope;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class InterfaceASTVisitor extends ClassASTVisitor {

	public InterfaceASTVisitor(
			ParsingEnvironment environment,
			ClassScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			TypeDeclaration node) {
		// TODO manage superinterfaces
		if (node.getSuperclassType() != null)
			throw new ParsingException("extends-clause",
					ParsingException.Type.UNSUPPORTED_STATEMENT,
					"The 'extends' clause is not supported yet.",
					getSourceCodeLocation(node.getSuperclassType()));

		if (!node.superInterfaceTypes().isEmpty())
			throw new ParsingException("implements-clause",
					ParsingException.Type.UNSUPPORTED_STATEMENT,
					"The 'implements' clause is not supported yet.",
					// using first interface for location
					getSourceCodeLocation((ASTNode) node.superInterfaceTypes().get(0)));

		if (!node.permittedTypes().isEmpty())
			throw new ParsingException("permits-clause",
					ParsingException.Type.UNSUPPORTED_STATEMENT,
					"The 'permits' clause is not supported yet.",
					// using first permitted type for location
					getSourceCodeLocation((ASTNode) node.permittedTypes().get(0)));

		createClassInitializer(getScope().getLisaClassUnit(), node);

		return false;
	}

}

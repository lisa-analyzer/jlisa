package it.unive.jlisa.frontend.visitors;

import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;

public class InterfaceASTVisitor extends BaseUnitASTVisitor {

	private String outer;

	public InterfaceASTVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit,
			String pkg,
			Map<String, String> imports) {
		super(parserContext, source, pkg, imports, compilationUnit);
		outer = null;
	}

	public InterfaceASTVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit,
			String pkg,
			Map<String, String> imports,
			String outer) {
		super(parserContext, source, pkg, imports, compilationUnit);
		this.outer = outer;
	}

	@Override
	public boolean visit(
			TypeDeclaration node) {
		// InterfaceUnit iUnit = (InterfaceUnit)
		// getProgram().getUnit(node.getName().toString());
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

		return false;
	}

}

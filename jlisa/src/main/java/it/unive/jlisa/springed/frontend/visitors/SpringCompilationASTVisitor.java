package it.unive.jlisa.springed.frontend.visitors;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.util.FQNUtils;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.scope.ClassScope;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.frontend.visitors.structure.MethodASTVisitor;
import org.eclipse.jdt.core.dom.*;

public class SpringCompilationASTVisitor extends ScopedVisitor<UnitScope> {

	public SpringCompilationASTVisitor(
			ParsingEnvironment environment,
			UnitScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			CompilationUnit node) {

		for (Object type : node.types())
			if (type instanceof TypeDeclaration typeDecl) {
				visitSecurityChainMethods(typeDecl);
			}

		return false;
	}

	private void visitSecurityChainMethods(
			TypeDeclaration typeDecl) {

		String name = FQNUtils.buildFQN(getScope().getPackage(), null, typeDecl.getName().toString());
		it.unive.lisa.program.CompilationUnit cUnit = (it.unive.lisa.program.CompilationUnit) getProgram()
				.getUnit(name);
		ClassScope classScope = getScope().toClassScope(null, cUnit);

		for (MethodDeclaration method : typeDecl.getMethods()) {
			if (isAnnotatedAsBean(method) && isOfReturnTypeSecurityFilterChain(method)) {

				try {
					MethodASTVisitor methodASTVisitor = new MethodASTVisitor(getEnvironment(), classScope);
					method.accept(methodASTVisitor);

				} catch (ParsingException e) {
					getParserContext().addException(e);

				} catch (RuntimeException e) {
					getParserContext().addException(new ParsingException("security-chain-method-visit",
							ParsingException.Type.PARSING_ERROR,
							e.getMessage(),
							getSourceCodeLocation(method)));
				}
			}
		}
	}

	private boolean isAnnotatedAsBean(
			MethodDeclaration method) {
		for (Object modifier : method.modifiers()) {
			if (modifier instanceof Annotation annotation) {
				String name = annotation.getTypeName().getFullyQualifiedName();
				if (name.equals("Bean") || name.equals("org.springframework.context.annotation.Bean")) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean isOfReturnTypeSecurityFilterChain(
			MethodDeclaration method) {
		Type type = method.getReturnType2();

		if (type != null) {
			return type.toString().equals("SecurityFilterChain") ||
					type.toString().equals("org.springframework.security.web.SecurityFilterChain*");
		}

		return false;
	}
}

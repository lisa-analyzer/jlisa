package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.program.JavaLocation;
import it.unive.lisa.program.Program;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public abstract class JavaASTVisitor extends ASTVisitor {
	private final ParsingEnvironment environment;

	public JavaASTVisitor(
			ParsingEnvironment environment) {
		this.environment = environment;
	}

	public JavaLocation getSourceCodeLocation(
			ASTNode node) {
		return environment.getSourceCodeLocation(node);
	}

	public Program getProgram() {
		return environment.parserContext().getProgram();
	}

	public int getApiLevel() {
		return environment.parserContext().getApiLevel();
	}

	public ParserContext getParserContext() {
		return environment.parserContext();
	}

	public String getSource() {
		return environment.source();
	}

	public CompilationUnit getAstUnit() {
		return environment.astUnit();
	}

	public ParsingEnvironment getEnvironment() {
		return environment;
	}
}

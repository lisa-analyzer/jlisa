package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public abstract class JavaASTVisitor extends ASTVisitor {
	private final ParsingEnvironment environment;


	public JavaASTVisitor(
			ParsingEnvironment environment) {
		this.environment = environment;
	}

	public SourceCodeLocation getSourceCodeLocation(
			ASTNode node) {
		int startPos = node.getStartPosition();
		return environment.parserContext().getLocationManager(environment.source(), environment.astUnit().getLineNumber(startPos),
				environment.astUnit().getColumnNumber(startPos)).getCurrentLocation();
	}

	public SourceCodeLocationManager getSourceCodeLocationManager(
			ASTNode node) {
		int startPos = node.getStartPosition();
		return environment.parserContext().getLocationManager(environment.source(), environment.astUnit().getLineNumber(startPos),
				environment.astUnit().getColumnNumber(startPos));
	}

	public SourceCodeLocationManager getSourceCodeLocationManager(
			ASTNode node,
			boolean end) {
		int startPos = node.getStartPosition();
		if (end) {
			return environment.parserContext().getLocationManager(environment.source(), environment.astUnit().getLineNumber(startPos),
					environment.astUnit().getColumnNumber(startPos) + node.getLength());
		}
		return environment.parserContext().getLocationManager(environment.source(), environment.astUnit().getLineNumber(startPos),
				environment.astUnit().getColumnNumber(startPos));
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

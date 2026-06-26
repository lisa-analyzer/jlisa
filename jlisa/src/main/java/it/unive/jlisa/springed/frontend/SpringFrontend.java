package it.unive.jlisa.springed.frontend;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.util.FQNUtils;
import it.unive.jlisa.frontend.visitors.pipeline.InitCodeMembersASTVisitor;
import it.unive.jlisa.frontend.visitors.pipeline.PopulateUnitsASTVisitor;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.lisa.program.Unit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class SpringFrontend extends JavaFrontend {

	private static final Logger LOG = LogManager.getLogger(SpringFrontend.class);
	private final List<Throwable> parseExceptions = new ArrayList<>();

	public Unit[] parse(
			String sourcePath)
			throws IOException {

		List<String> expandedPaths = this.expandFilePaths(List.of(sourcePath));
		int n = expandedPaths.size();

		LibrarySpecificationProvider.load(getProgram());
		LibrarySpecificationProvider.importJavaLang(getProgram());

		CompilationUnit[] cus = new CompilationUnit[n];
		String[] fileNames = new String[n];
		UnitScope[] scopes = new UnitScope[n];

		for (int i = 0; i < n; i++) {
			Path path = Paths.get(expandedPaths.get(i));
			fileNames[i] = path.getFileName().toString();
			String source = Files.readString(path);
			cus[i] = getCompilationUnit(source);

			ParsingEnvironment environment = new ParsingEnvironment(this.getParserContext(), fileNames[i], cus[i]);
			scopes[i] = UnitScope.init(environment, cus[i]);
		}

		this.runPass(cus, fileNames, scopes, PopulateUnitsASTVisitor::new);
		this.runPass(cus, fileNames, scopes, InitCodeMembersASTVisitor::new);

		return this.getUnits(cus, scopes);
	}

	protected Unit[] getUnits(
			CompilationUnit[] cus,
			UnitScope[] scopes) {

		Unit[] units = new Unit[cus.length];

		for (int i = 0; i < cus.length; i++) {
			String pkg = scopes[i].getPackage();
			for (Object type : cus[i].types()) {
				if (type instanceof TypeDeclaration td) {
					String name = FQNUtils.buildFQN(pkg, null, td.getName().toString());
					units[i] = getProgram().getUnit(name);
				}
			}
		}

		return units;
	}

	public List<Throwable> getParseExceptions() {
		List<Throwable> all = new ArrayList<>(parseExceptions);
		all.addAll(getParserContext().getExceptions());
		return all;
	}

	@Override
	protected void runPass(
			CompilationUnit[] cus,
			String[] fileNames,
			UnitScope[] scopes,
			BiFunction<ParsingEnvironment, UnitScope, ASTVisitor> factory) {
		for (int i = 0; i < cus.length; i++) {
			ParsingEnvironment env = new ParsingEnvironment(parserContext, fileNames[i], cus[i]);

			try {
				cus[i].accept(factory.apply(env, scopes[i]));
			} catch (RuntimeException e) {
				LOG.error("Error while parsing " + fileNames[i], e);
				parseExceptions.add(e);
			}
		}
	}
}

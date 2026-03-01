package it.unive.jlisa.frontend;

import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.visitors.pipeline.CompilationUnitASTVisitor;
import it.unive.jlisa.frontend.visitors.pipeline.InitCodeMembersASTVisitor;
import it.unive.jlisa.frontend.visitors.pipeline.PopulateUnitsASTVisitor;
import it.unive.jlisa.frontend.visitors.pipeline.SetGlobalsASTVisitor;
import it.unive.jlisa.frontend.visitors.pipeline.SetRelationshipsASTVisitor;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.program.language.JavaLanguageFeatures;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.TypeSystem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class JavaFrontend {
	private final ParserContext parserContext;
	private int API_LEVEL = AST.getJLSLatest();

	public JavaFrontend() {
		// We are creating a new Program. We need to start from a blank state.
		clearAll();

		this.parserContext = new ParserContext(createProgram(), this.API_LEVEL);
	}

	public JavaFrontend(
			int apiLevel) {
		clearAll();

		this.API_LEVEL = apiLevel;
		this.parserContext = new ParserContext(createProgram(), this.API_LEVEL);
	}

	public JavaFrontend(
			Program program) {
		Program p;
		if (program == null) {
			p = createProgram();
		} else {
			p = program;
		}
		this.parserContext = new ParserContext(p, this.API_LEVEL);
	}

	public JavaFrontend(
			Program program,
			int apiLevel) {
		this.API_LEVEL = apiLevel;
		this.parserContext = new ParserContext(program, apiLevel);
	}

	public Program getProgram() {
		return this.parserContext.getProgram();
	}

	public ParserContext getParserContext() {
		return this.parserContext;
	}

	public void clearAll() {
		JavaClassType.clearAll();
		JavaInterfaceType.clearAll();
	}

	private void registerTypes() {
		TypeSystem typeSystem = this.parserContext.getProgram().getTypes();
		typeSystem.registerType(JavaBooleanType.INSTANCE);
		typeSystem.registerType(JavaByteType.INSTANCE);
		typeSystem.registerType(JavaShortType.INSTANCE);
		typeSystem.registerType(JavaIntType.INSTANCE);
		typeSystem.registerType(JavaLongType.INSTANCE);
		typeSystem.registerType(JavaFloatType.INSTANCE);
		typeSystem.registerType(JavaDoubleType.INSTANCE);

		typeSystem.registerType(StringType.INSTANCE);
		JavaClassType.all().forEach(typeSystem::registerType);
		JavaArrayType.all().forEach(typeSystem::registerType);
		JavaInterfaceType.all().forEach(typeSystem::registerType);
	}

	public static Program createProgram() {
		JavaLanguageFeatures features = new JavaLanguageFeatures();
		JavaTypeSystem typeSystem = new JavaTypeSystem();
		return new Program(features, typeSystem);
	}

	private ASTParser getParser(
			String source,
			int parseAs) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest()); // NOTE:
																	// JLS8 is
																	// deprecated.
																	// getJLSLatest
																	// will
																	// return
																	// JDK23
		parser.setKind(parseAs);
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(options);
		parser.setSource(source.toCharArray());
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);

		return parser;
	}

	private CompilationUnit getCompilationUnit(
			String source) {
		ASTParser parser = getParser(source, ASTParser.K_COMPILATION_UNIT);
		return (CompilationUnit) parser.createAST(null);
	}

	private List<String> expandFilePaths(
			List<String> paths)
			throws IOException {
		java.util.List<String> expandedPaths = new java.util.ArrayList<>();
		for (String pathStr : paths) {
			Path path = Paths.get(pathStr).normalize();
			if (Files.isDirectory(path)) {
				try (Stream<Path> stream = Files.walk(path)) {
					stream.filter(Files::isRegularFile)
							.filter(p -> p.toString().endsWith(".java"))
							.forEach(p -> expandedPaths.add(p.toString()));
				}
			} else if (Files.isRegularFile(path) && path.toString().endsWith(".java")) {
				expandedPaths.add(path.toString());
			} else {
				throw new FileNotFoundException(pathStr);
			}
		}
		return expandedPaths;
	}

	public Program parseFromListOfFile(
			List<String> filePaths)
			throws IOException {
		LibrarySpecificationProvider.load(getProgram());
		LibrarySpecificationProvider.importJavaLang(getProgram());
		List<String> expandedPaths = expandFilePaths(filePaths);
		int n = expandedPaths.size();

		// Parse all files once upfront
		CompilationUnit[] cus = new CompilationUnit[n];
		String[] fileNames = new String[n];
		UnitScope[] scopes = new UnitScope[n];
		for (int i = 0; i < n; i++) {
			Path path = Paths.get(expandedPaths.get(i));
			fileNames[i] = path.getFileName().toString();
			if (fileNames[i].equals("module-info.java"))
				throw new ParsingException("java-module", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Java modules are not supported.", new SourceCodeLocation(fileNames[i], -1, -1));
			String source = Files.readString(path);
			cus[i] = getCompilationUnit(source);
			ParsingEnvironment environment = new ParsingEnvironment(parserContext, fileNames[i], cus[i]);
			scopes[i] = UnitScope.init(environment, cus[i]);
		}

		runPass(cus, fileNames, scopes, (
				env,
				scope) -> new PopulateUnitsASTVisitor(env, scope));
		registerTypes();
		runPass(cus, fileNames, scopes, (
				env,
				scope) -> new SetRelationshipsASTVisitor(env, scope));
		runPass(cus, fileNames, scopes, (
				env,
				scope) -> new SetGlobalsASTVisitor(env, scope));
		runPass(cus, fileNames, scopes, (
				env,
				scope) -> new InitCodeMembersASTVisitor(env, scope));

		for (int i = 0; i < n; i++) {
			IProblem[] problems = cus[i].getProblems();
			for (IProblem problem : problems)
				if (problem.isError())
					System.out.println("Error at line " + problem.getSourceLineNumber() + ": " + problem.getMessage());
			if (problems.length != 0)
				throw new RuntimeException(problems.length + " problems found.");
			ParsingEnvironment env = new ParsingEnvironment(parserContext, fileNames[i], cus[i]);
			cus[i].accept(new CompilationUnitASTVisitor(env, scopes[i]));
			registerTypes();
		}

		return getProgram();
	}

	private void runPass(
			CompilationUnit[] cus,
			String[] fileNames,
			UnitScope[] scopes,
			BiFunction<ParsingEnvironment, UnitScope, ASTVisitor> factory) {
		for (int i = 0; i < cus.length; i++) {
			ParsingEnvironment env = new ParsingEnvironment(parserContext, fileNames[i], cus[i]);
			cus[i].accept(factory.apply(env, scopes[i]));
		}
	}

}

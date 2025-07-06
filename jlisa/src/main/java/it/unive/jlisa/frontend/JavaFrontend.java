package it.unive.jlisa.frontend;

import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.visitors.CompilationUnitASTVisitor;
import it.unive.jlisa.program.JavaProgram;
import it.unive.jlisa.program.type.*;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.program.Program;
import it.unive.jlisa.program.language.JavaLanguageFeatures;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.type.TypeSystem;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class JavaFrontend {
    private ParserContext parserContext;
    private int API_LEVEL = AST.getJLSLatest();


    public JavaFrontend() {
        // We are creating a new Program. We need to start from a blank state.
        clearAll();

        this.parserContext = new ParserContext(createProgram(), this.API_LEVEL, ParserContext.EXCEPTION_HANDLING_STRATEGY.COLLECT);
    }

    public JavaFrontend(int apiLevel) {
        clearAll();

        this.API_LEVEL = apiLevel;
        this.parserContext = new ParserContext(createProgram(), this.API_LEVEL, ParserContext.EXCEPTION_HANDLING_STRATEGY.COLLECT);
    }

    public JavaFrontend(JavaProgram program) {
        JavaProgram p;
        if (program == null) {
             p = createProgram();
        }
        p  = program;
        this.parserContext = new ParserContext(p, this.API_LEVEL, ParserContext.EXCEPTION_HANDLING_STRATEGY.COLLECT);
    }

    public JavaFrontend(JavaProgram program, int apiLevel) {
        this.API_LEVEL = apiLevel;
        this.parserContext = new ParserContext(program, apiLevel, ParserContext.EXCEPTION_HANDLING_STRATEGY.COLLECT);
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

    public void registerTypes() {
        TypeSystem typeSystem = this.parserContext.getProgram().getTypes();
        typeSystem.registerType(BoolType.INSTANCE);
        typeSystem.registerType(JavaByteType.INSTANCE);
        typeSystem.registerType(JavaShortType.INSTANCE);
        typeSystem.registerType(JavaIntType.INSTANCE);
        typeSystem.registerType(JavaLongType.INSTANCE);
        typeSystem.registerType(JavaFloatType.INSTANCE);
        typeSystem.registerType(JavaDoubleType.INSTANCE);


        typeSystem.registerType(JavaInstrumentedStringType.INSTANCE);
        JavaClassType.all().forEach(typeSystem::registerType);
        JavaArrayType.all().forEach(typeSystem::registerType);
        JavaInterfaceType.all().forEach(typeSystem::registerType);
    }
    public static JavaProgram createProgram() {
        JavaLanguageFeatures features = new JavaLanguageFeatures();
        JavaTypeSystem typeSystem = new JavaTypeSystem();
        return new JavaProgram(features, typeSystem);
    }

    public ASTParser getParser(String source, int parseAs) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest()); // NOTE: JLS8 is deprecated. getJLSLatest will return JDK23
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
        //String[] classpath = {};
        //parser.setEnvironment(classpath, new String[] { "." }, null, true);
        //parser.setCompilerOptions(JavaCore.getOptions());

        return parser;
    }

    public CompilationUnit getCompilationUnit(String source) {
        ASTParser parser = getParser(source, ASTParser.K_COMPILATION_UNIT);
        return (CompilationUnit) parser.createAST(null);
    }

    public List<String> expandFilePaths(List<String> paths) throws IOException {
        List<String> expandedPaths = new ArrayList<>();
        for (String pathStr : paths) {
            Path path = Paths.get(pathStr);
            if (Files.isDirectory(path)) {
                try (Stream<Path> stream = Files.walk(path)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".java"))
                            .forEach(p -> expandedPaths.add(p.toString()));
                }
            } else if (Files.isRegularFile(path) && path.toString().endsWith(".java")) {
                expandedPaths.add(path.toString());
            }
        }
        return expandedPaths;
    }

    public Program parseFromListOfFile(List<String> filePaths) throws IOException {
        List<String> expandedPaths = expandFilePaths(filePaths);
        populateUnits (expandedPaths);
        registerTypes();
        for (String filePath : expandedPaths) {
            Path path = Paths.get(filePath);
            String source = Files.readString(path);
            boolean module = path.getFileName().toString().equals("module-info.java");
            parse(source, path.getFileName().toString(), module);
        }
        return getProgram();
    }

    public void populateUnits(List<String> filePaths) throws IOException {
        for (String filePath : filePaths) {
            Path path = Paths.get(filePath);
            String source = Files.readString(path);
            CompilationUnit cu = getCompilationUnit(source);
            cu.accept(new CompilationUnitASTVisitor(parserContext, path.getFileName().toString(), cu, false));
        }
    }

    public Program parseFromFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        String source = Files.readString(path);
        boolean module = path.getFileName().toString().equals("module-info.java");
        return parse(source, path.getFileName().toString(), module);
    }

    public Program parseFromString(String source, boolean module) {
        return parse(source, "unknown", module);
    }

    private Program parse(String source, String fileName, boolean module) {
        CompilationUnit cu = getCompilationUnit(source);
        CompilationUnitASTVisitor visitor = new CompilationUnitASTVisitor(parserContext, fileName, cu, true);

        if (module) {
            parserContext.addException(new ParsingException("java-module", ParsingException.Type.UNSUPPORTED_STATEMENT, "Java modules are not supported.", new SourceCodeLocation(source, -1, -1)));
        }
        IProblem[] problems = cu.getProblems();
        for (IProblem problem : problems) {
            if (problem.isError()) {
                System.out.println("Error at line " + problem.getSourceLineNumber() + ": " + problem.getMessage());
            }
        }
        if (problems.length != 0) {
            throw new RuntimeException(problems.length + " problems found.");
        }
        cu.accept(visitor);

        registerTypes();
        return this.parserContext.getProgram();
    }

}

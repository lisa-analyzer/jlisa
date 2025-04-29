package it.unive.jlisa.frontend;

import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.frontend.visitors.CompilationUnitASTVisitor;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.jlisa.types.JavaClassType;
import it.unive.jlisa.types.JavaInterfaceType;
import it.unive.lisa.program.Program;
import it.unive.jlisa.program.language.JavaLanguageFeatures;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Float32Type;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.TypeSystem;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.core.JavaProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class JavaFrontend {
    private Program program;

    private int API_LEVEL = AST.getJLSLatest();


    public JavaFrontend() {
        // We are creating a new Program. We need to start from a blank state.
        clearAll();

        this.program = createProgram();
    }

    public JavaFrontend(int apiLevel) {
        this();
        this.API_LEVEL = apiLevel;
    }

    public JavaFrontend(Program program) {
        if (program == null) {
            this.program = createProgram();
        }
        this.program = program;
    }

    public JavaFrontend(Program program, int apiLevel) {
        this.program = program;
    }

    public Program getProgram() {
        return this.program;
    }

    public void clearAll() {
        JavaClassType.clearAll();
        JavaInterfaceType.clearAll();
    }

    public void registerTypes() {
        TypeSystem typeSystem = this.program.getTypes();
        typeSystem.registerType(BoolType.INSTANCE);
        typeSystem.registerType(Float32Type.INSTANCE);
        typeSystem.registerType(Int32Type.INSTANCE);
        typeSystem.registerType(StringType.INSTANCE);
        JavaClassType.all().forEach(typeSystem::registerType);
        //ArrayType.all().forEach(t -> typeSystem.registerType(t));
        JavaInterfaceType.all().forEach(typeSystem::registerType);
    }
    public static Program createProgram() {
        JavaLanguageFeatures features = new JavaLanguageFeatures();
        JavaTypeSystem typeSystem = new JavaTypeSystem();
        return new Program(features, typeSystem);
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
        CompilationUnitASTVisitor visitor = new CompilationUnitASTVisitor(this.program, fileName, this.API_LEVEL, cu);

        if (module) {
            throw new UnsupportedStatementException("Java modules are not supported.");
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
        return this.program;
    }

}

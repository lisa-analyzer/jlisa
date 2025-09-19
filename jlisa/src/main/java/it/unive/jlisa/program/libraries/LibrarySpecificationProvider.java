package it.unive.jlisa.program.libraries;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import it.unive.jlisa.antlr.LibraryDefinitionLexer;
import it.unive.jlisa.antlr.LibraryDefinitionParser;
import it.unive.jlisa.program.libraries.LibrarySpecificationParser.LibraryCreationException;
import it.unive.jlisa.program.libraries.loader.ClassDef;
import it.unive.jlisa.program.libraries.loader.Runtime;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;

public class LibrarySpecificationProvider {

	public static final String LIBS_FOLDER = "/libraries/";

	private static final Map<String, ClassDef> AVAILABLE_LIB_CLASSES = new HashMap<>();

	public static CompilationUnit hierarchyRoot;

	private static CFG init;

	private static final Collection<String> LOADED_LIB_CLASSES = new HashSet<>();

	public static void load(
			Program program)
			throws AnalysisSetupException {
		reset();
		init = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, program, false, "param_init"));
		Map<String, Runtime> parsedLibs = new HashMap<>();

		try (ScanResult scanResult = new ClassGraph().acceptPaths(LIBS_FOLDER).scan()) {
			for (String path : scanResult.getAllResources().getPaths())
				readLibrary(path, program, parsedLibs);
		}
	}

	private static void reset() {
		init = null;
		hierarchyRoot = null;
		AVAILABLE_LIB_CLASSES.clear();
		LOADED_LIB_CLASSES.clear();
	}

	private static void readLibrary(
			String path,
			Program program,
			Map<String, Runtime> parsedLibs) {
		if (!parsedLibs.containsKey(path)) {
			Runtime file = readFile(path.startsWith("/") ? path : "/" + path, program, parsedLibs);
			parsedLibs.put(path, file);
			file.addRuntimeMembers(program, init, hierarchyRoot);
			file.getClasses().forEach(cls -> AVAILABLE_LIB_CLASSES.put(cls.getName(), cls));
		}
	}

	private static Runtime readFile(
			String file,
			Program program,
			Map<String, Runtime> parsedLibs)
			throws AnalysisSetupException {
		LibraryDefinitionLexer lexer = null;
		try (InputStream stream = LibrarySpecificationParser.class.getResourceAsStream(file)) {
			lexer = new LibraryDefinitionLexer(CharStreams.fromStream(stream, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new AnalysisSetupException("Unable to parse '" + file + "'", e);
		}

		LibraryDefinitionParser parser = new LibraryDefinitionParser(new CommonTokenStream(lexer));
		LibrarySpecificationParser libParser = new LibrarySpecificationParser(file);
		return libParser.visitFile(parser.file());
	}

	public static void importJavaLang(
			Program program) {
		importClass(program, "java.lang.Object");
		importClass(program, "java.lang.String");
		for (String lib : AVAILABLE_LIB_CLASSES.keySet())
			if (lib.startsWith("java.lang."))
				importClass(program, lib);
	}

	public static void importClass(
			Program program,
			String name) {
		if (LOADED_LIB_CLASSES.contains(name))
			return;

		ClassDef library = AVAILABLE_LIB_CLASSES.get(name);
		if (library == null)
			throw new IllegalArgumentException("Class " + name + " is not available in the loaded libraries");

		if (library.getBase() != null && !LOADED_LIB_CLASSES.contains(library.getBase()))
			importClass(program, library.getBase());

		AtomicReference<CompilationUnit> root = new AtomicReference<>(hierarchyRoot);
		ClassUnit lib = library.toLiSAUnit(program, root);
		if (hierarchyRoot == null)
			hierarchyRoot = root.get();

		program.addUnit(lib);
		// create the corresponding type
		if (library.getTypeName() == null)
			JavaClassType.register(lib.getName(), lib);
		else
			try {
				Class<?> type = Class.forName(library.getTypeName());
				Constructor<?> constructor = type.getConstructor(CompilationUnit.class);
				constructor.newInstance(lib);
			} catch (ClassNotFoundException
					| SecurityException
					| IllegalArgumentException
					| IllegalAccessException
					| NoSuchMethodException
					| InstantiationException
					| InvocationTargetException e) {
				throw new LibraryCreationException(e);
			}
			
		library.populateUnit(init, hierarchyRoot);
		LOADED_LIB_CLASSES.add(name);
	}

	public static boolean isLibraryAvailable(
			String name) {
		return AVAILABLE_LIB_CLASSES.containsKey(name);
	}
}

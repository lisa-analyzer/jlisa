package it.unive.jlisa.program.libraries;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import it.unive.jlisa.antlr.LibraryDefinitionLexer;
import it.unive.jlisa.antlr.LibraryDefinitionParser;
import it.unive.jlisa.program.libraries.loader.Library;
import it.unive.jlisa.program.libraries.loader.Runtime;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.tuple.Pair;

public class LibrarySpecificationProvider {

	public static final String LIBS_FOLDER = "/libraries/";
	private static final String STDLIB_FILE = "stdlib.txt";

	private static final Map<String, Library> AVAILABLE_LIBS = new HashMap<>();

	public static CompilationUnit hierarchyRoot;

	private static CFG init;

	private static final Collection<String> LOADED_LIBS = new HashSet<>();

	public static void load(
			Program program)
			throws AnalysisSetupException {
		reset();
		// Load stdlib
		loadStdlib(program);
		var stdlib = readFile(LIBS_FOLDER + STDLIB_FILE);
		var root = new AtomicReference<CompilationUnit>();
		stdlib.getLeft().fillProgram(program, root);
		hierarchyRoot = root.get();
		stdlib.getLeft().populateProgram(program, init, hierarchyRoot);
		stdlib.getRight().forEach(lib -> AVAILABLE_LIBS.put(lib.getName(), lib));
		stdlib.getLeft().populateProgram(program, init, hierarchyRoot);
		for (Library lib : stdlib.getValue())
			AVAILABLE_LIBS.put(lib.getName(), lib);

		// Load other libraries
		try (ScanResult scanResult = new ClassGraph().acceptPaths(LIBS_FOLDER).scan()) {
			List<Pair<Runtime,
					Collection<Library>>> parsedLibs = readLibraries(scanResult.getAllResources().getPaths());
			fillUnits(program, parsedLibs, root);
			populateProgram(program, parsedLibs);
		}
	}

	private static void loadStdlib(
			Program program) {
		var stdlib = readFile(LIBS_FOLDER + STDLIB_FILE);
		var root = new AtomicReference<CompilationUnit>();
		stdlib.getLeft().fillProgram(program, root);
		hierarchyRoot = root.get();
		stdlib.getLeft().populateProgram(program, init, hierarchyRoot);
		stdlib.getRight().forEach(lib -> AVAILABLE_LIBS.put(lib.getName(), lib));
		stdlib.getLeft().populateProgram(program, init, hierarchyRoot);
	}

	private static void reset() {
		init = null;
		hierarchyRoot = null;
		AVAILABLE_LIBS.clear();
		LOADED_LIBS.clear();
	}

	private static List<Pair<Runtime, Collection<Library>>> readLibraries(
			List<String> paths) {
		List<Pair<Runtime, Collection<Library>>> result = new ArrayList<>();
		for (String path : paths) {
			if (!path.endsWith("/" + STDLIB_FILE)) {
				result.add(readFile(path.startsWith("/") ? path : "/" + path));
			}
		}
		return result;
	}

	private static void fillUnits(
			Program program,
			List<Pair<Runtime, Collection<Library>>> parsedLibs,
			AtomicReference<CompilationUnit> root) {
		for (Pair<Runtime, Collection<Library>> libs : parsedLibs) {
			libs.getLeft().fillProgram(program, root);
		}
	}

	private static void populateProgram(
			Program program,
			List<Pair<Runtime, Collection<Library>>> parsedLibs) {
		for (Pair<Runtime, Collection<Library>> libs : parsedLibs) {
			libs.getLeft().populateProgram(program, init, hierarchyRoot);
			for (Library lib : libs.getValue()) {
				AVAILABLE_LIBS.put(lib.getName(), lib);
				// TODO: to check if it is correct
				importLibrary(program, lib.getName());
			}
		}
	}

	private static Pair<Runtime, Collection<Library>> readFile(
			String file)
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

	public static void importLibrary(
			Program program,
			String name) {
		if (LOADED_LIBS.contains(name))
			return;

		Library library = AVAILABLE_LIBS.get(name);
		if (library == null) {
			return;
		}

		CodeUnit lib = library.toLiSAUnit(program, new AtomicReference<>(hierarchyRoot));
		library.populateUnit(init, hierarchyRoot, lib);
		LOADED_LIBS.add(name);
	}

	public static Collection<Library> getLibraryUnits() {
		return AVAILABLE_LIBS.values();
	}

	public static Library getLibraryUnit(
			String name) {
		return AVAILABLE_LIBS.get(name);
	}

	public static boolean isLibraryLoaded(
			String name) {
		return LOADED_LIBS.contains(name);
	}
}

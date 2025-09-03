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
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


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
		init = null;
		hierarchyRoot = null;
		AVAILABLE_LIBS.clear();
		LOADED_LIBS.clear();

		Pair<Runtime, Collection<Library>> stdlib = readFile(LIBS_FOLDER + STDLIB_FILE);
		AtomicReference<CompilationUnit> root = new AtomicReference<CompilationUnit>(null);
		stdlib.getLeft().fillProgram(program, root);
		hierarchyRoot = root.get();
//		makeInit(program);
		stdlib.getLeft().populateProgram(program, init, hierarchyRoot);
		for (Library lib : stdlib.getValue())
			AVAILABLE_LIBS.put(lib.getName(), lib);

		try (ScanResult scanResult = new ClassGraph().acceptPaths(LIBS_FOLDER).scan()) {
			// NOTE: THE NEXT TWO LINES ARE TEMPORARILY INCLUDED TO ALLOW SV-COMP ANALYSIS TO RUN.
			// HOWEVER, THIS DOES NOT GUARANTEE CORRECT EXECUTION IF NEW .TXT MODELS ARE INTRODUCED.
			// The issue arises when a parameter of a class defined in a .txt file is of a library type, and its definition
			// resides in another .txt file that has not yet been processed. In such cases, the analysis may fail.
			// Currently, an (approximate) correct order of parsing is the sorted one.
			// However, is not guaranteed that scanResult.getAllResources().getPaths() is sorted.
			System.out.println("WARNING: library files are being sorted by name to enable pipeline execution.");
			List<String> sortedPath = scanResult.getAllResources().getPaths();
			Collections.sort(sortedPath);

			// for (String path : scanResult.getAllResources().getPaths())
			for (String path : sortedPath) { // temporary
				if (!path.endsWith("/" + STDLIB_FILE)) {
					// need to add the / since the returned paths are relative
					Pair<Runtime, Collection<Library>> libs = readFile("/" + path);
					libs.getLeft().fillProgram(program, root);
					libs.getLeft().populateProgram(program, init, hierarchyRoot);
					for (Library lib : libs.getValue()) {
						AVAILABLE_LIBS.put(lib.getName(), lib);
						// TODO: to check if it is correct
						importLibrary(program, lib.getName());
					}
				}
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

//	private static CFG makeInit(
//			Program program) {
//		init = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, program, false, "LiSA$init"));
//		init.addNode(new Ret(init, SyntheticLocation.INSTANCE), true);
//		program.addCodeMember(init);
//		return init;
//	}

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

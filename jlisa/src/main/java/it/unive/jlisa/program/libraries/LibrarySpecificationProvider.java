package it.unive.jlisa.program.libraries;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.tuple.Pair;

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

	// Add here other source folders with libraries
	public static final List<String> LIBS_FOLDER = List.of(
			"/java-libraries/");

	private static final Map<String, ClassDef> AVAILABLE_LIB_CLASSES = new TreeMap<>();

	private static final Map<String, Collection<String>> EXCEPTION_HIERARCHY = new TreeMap<>();

	public static CompilationUnit hierarchyRoot;

	private static CFG init;

	private static boolean loadingJavaLang = false;

	private static final Collection<String> LOADED_LIB_CLASSES = new TreeSet<>();

	private static final Queue<Runnable> PENDING_POPULATIONS = new LinkedList<>();

	public static void load(
			Program program)
			throws AnalysisSetupException {
		reset();
		init = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, program, false, "param_init"));
		Map<String, Runtime> parsedLibs = new TreeMap<>();

		for (String lib : LIBS_FOLDER) {
			try (ScanResult scanResult = new ClassGraph().acceptPaths(lib).scan()) {
				for (String path : scanResult.getAllResources().getPaths())
					readLibrary(path, program, parsedLibs);
			}
		}

		Collection<String> frontier = new TreeSet<>();
		frontier.add("java.lang.Throwable");
		Collection<String> nextFrontier;
		do {
			nextFrontier = new TreeSet<>();
			for (ClassDef def : AVAILABLE_LIB_CLASSES.values())
				if (def.getBase() != null && frontier.contains(def.getBase())) {
					EXCEPTION_HIERARCHY.computeIfAbsent(def.getBase(), k -> new TreeSet<>()).add(def.getName());
					nextFrontier.add(def.getName());
				}
			frontier = nextFrontier;
		} while (!nextFrontier.isEmpty());
	}

	private static void reset() {
		init = null;
		hierarchyRoot = null;
		AVAILABLE_LIB_CLASSES.clear();
		EXCEPTION_HIERARCHY.clear();
		LOADED_LIB_CLASSES.clear();
		PENDING_POPULATIONS.clear();
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
		loadingJavaLang = true;
		importClass(program, "java.lang.Object", true);
		importClass(program, "java.lang.reflect.Method", true);
		importClass(program, "java.lang.Class", true);
		importClass(program, "java.lang.String", true);
		for (String lib : AVAILABLE_LIB_CLASSES.keySet())
			if (getPackage(lib).equals("java.lang"))
				importClass(program, lib);
		loadingJavaLang = false;

		executePendingPopulations();
	}

	private static String getPackage(
			String name) {
		int idx = name.lastIndexOf('.');
		if (idx < 0)
			return "";
		String pkg = name.substring(0, idx);
		if (isLibraryAvailable(pkg))
			// name points to an inner class, so
			// we have to return the package of the outer class
			return getPackage(pkg);
		return pkg;
	}

	public static void importClass(
			Program program,
			String name) {
		importClass(program, name, false);
	}

	private static void importClass(
			Program program,
			String name,
			boolean forceImport) {
		if (LOADED_LIB_CLASSES.contains(name))
			return;

		ClassDef requestedLibrary = AVAILABLE_LIB_CLASSES.get(name);
		if (requestedLibrary == null)
			throw new IllegalArgumentException("Class " + name + " is not available in the loaded libraries");

		AtomicReference<CompilationUnit> root = new AtomicReference<>(hierarchyRoot);
		ClassUnit requestedLib = requestedLibrary.toLiSAUnit(program, root);
		if (hierarchyRoot == null)
			hierarchyRoot = root.get();

		// list to preserve discovery order
		List<String> toLoad = new LinkedList<>();
		if (!loadingJavaLang && EXCEPTION_HIERARCHY.containsKey(requestedLibrary.getName())) {
			// if the library is an exception, we also load its known
			// subtypes as they might be thrown by native constructs
			// without being explicitly imported (eg `throws IOException`
			// in the method declaration, but a construct throws
			// `FileNotFoundException` which is a subtype of `IOException`)
			Collection<String> frontier = EXCEPTION_HIERARCHY.get(requestedLibrary.getName());
			Collection<String> nextFrontier;
			do {
				toLoad.addAll(frontier);
				nextFrontier = new TreeSet<>();
				for (String n : frontier) 
					nextFrontier.addAll(EXCEPTION_HIERARCHY.getOrDefault(n, List.of()));
				frontier = nextFrontier;
			} while (!nextFrontier.isEmpty());
		}

		List<Pair<ClassDef, ClassUnit>> toAdd = new LinkedList<>();
		toAdd.add(Pair.of(requestedLibrary, requestedLib));
		for (String n : toLoad) {
			ClassDef classDef = AVAILABLE_LIB_CLASSES.get(n);
			toAdd.add(Pair.of(classDef, classDef.toLiSAUnit(program, root)));
		}
		for (Pair<ClassDef, ClassUnit> pair : toAdd) {
			ClassDef def = pair.getLeft();
			ClassUnit lib = pair.getRight();
			String libname = lib.getName();
			String typeName = def.getTypeName();

			if (LOADED_LIB_CLASSES.contains(libname))
				continue;

			program.addUnit(lib);
			// create the corresponding type
			if (typeName == null) {
				JavaClassType.register(libname, lib);
			} else
				try {
					Class<?> type = Class.forName(typeName);
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

			LOADED_LIB_CLASSES.add(libname);

			if (!forceImport)
				def.populateUnit(program, init, hierarchyRoot);
			else {
				final CompilationUnit capturedRoot = hierarchyRoot;
				PENDING_POPULATIONS.add(() -> {
					requestedLibrary.populateUnit(program, init, capturedRoot);
				});
			}

			// nested classes should be loaded as well
			for (String n : getNestedUnits(libname))
				importClass(program, n);
		}
	}

	private static void executePendingPopulations() {
		while (!PENDING_POPULATIONS.isEmpty()) {
			Runnable task = PENDING_POPULATIONS.poll();
			task.run();
		}
	}

	public static boolean isLibraryAvailable(
			String name) {
		return AVAILABLE_LIB_CLASSES.containsKey(name);
	}

	public static Collection<String> getLibrariesOfPackage(
			String name) {
		return AVAILABLE_LIB_CLASSES.keySet().stream().filter(n -> getPackage(n).equals(name)).toList();
	}

	public static Collection<String> getNestedUnits(
			String name) {
		return AVAILABLE_LIB_CLASSES.keySet().stream().filter(n -> n.startsWith(name + ".")).toList();
	}
}

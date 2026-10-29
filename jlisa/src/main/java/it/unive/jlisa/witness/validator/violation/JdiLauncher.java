package it.unive.jlisa.witness.validator.violation;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.ListeningConnector;
import it.unive.jlisa.witness.validator.ValidationException;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Launches the target JVM under JDI control: the validator listens on a local
 * port ({@link ListeningConnector}), and starts the target JVM with a
 * {@link ProcessBuilder} (no shell), asking it to connect to that port.
 * <h3>Working directory</h3>
 * <p>
 * The target program runs in a new temporary directory, deleted when it exits:
 * what it writes never ends up in the directory of the validator (for SV-COMP,
 * the directory of the run, whose files BenchExec keeps). The validator itself
 * keeps its working directory, and reads the task with the paths it was given.
 * <h3>Cross-version operation</h3>
 * <p>
 * The validator runs on Java 21 or newer but can debug a Java 8 JVM, given its
 * Java executable. JDWP (Java Debug Wire Protocol) is backwards-compatible, so
 * a newer JDI can control a Java 8 target transparently.
 * <h3>Assertions</h3>
 * <p>
 * The target JVM is launched with {@code -ea} by default, so that
 * {@code assert} statements throw {@link AssertionError} (required for the
 * {@code valid-assert} property). For {@code no-runtime-exception}, a run can
 * also be launched without {@code -ea}, to check that an exception does not
 * depend on assertions.
 */
public final class JdiLauncher {

	/** Milliseconds to wait for the target JVM to connect. */
	private static final String CONNECT_TIMEOUT_MILLIS = "30000";

	/**
	 * The process of each target JVM (a JVM that connects to the validator has
	 * no {@link VirtualMachine#process()}).
	 */
	private static final Map<VirtualMachine, Process> PROCESSES = new ConcurrentHashMap<>();

	/**
	 * The working directory of each target JVM launched and not yet cleaned up:
	 * when the validator exits first (e.g., right after its verdict), the
	 * target JVMs still running are killed and the directories deleted here,
	 * since the callbacks on their exit would not run.
	 */
	private static final Map<Process, Path> WORKING_DIRS = new ConcurrentHashMap<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			for (Map.Entry<Process, Path> entry : WORKING_DIRS.entrySet()) {
				Process process = entry.getKey();
				process.destroyForcibly();
				try {
					// a JVM still writing in its directory would keep some
					// files there
					process.waitFor(1, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				delete(entry.getValue());
			}
		}));
	}

	private JdiLauncher() {
	}

	/**
	 * Launches the target program, with assertions enabled ({@code -ea}), and
	 * returns a connected, suspended {@link VirtualMachine}.
	 * <p>
	 * The VM starts suspended: no user code runs until the caller calls
	 * {@link VirtualMachine#resume()}. This window is used to register event
	 * requests before execution begins.
	 *
	 * @param mainClass       fully-qualified main class name (e.g.
	 *                            {@code "Main"})
	 * @param classpath       class-path string to pass to the target JVM
	 * @param java8Executable path to the Java executable for the target JVM, or
	 *                            {@code null} to use the {@code java} found on
	 *                            {@code PATH}
	 * 
	 * @return a connected, suspended {@link VirtualMachine}
	 * 
	 * @throws ValidationException if the VM cannot be launched
	 */
	public static VirtualMachine launch(
			String mainClass,
			String classpath,
			String java8Executable)
			throws ValidationException {
		return launch(mainClass, classpath, java8Executable, true);
	}

	/**
	 * As {@link #launch(String, String, String)}, with Java assertions enabled
	 * ({@code -ea}) or not.
	 */
	public static VirtualMachine launch(
			String mainClass,
			String classpath,
			String java8Executable,
			boolean assertions)
			throws ValidationException {

		// Point at the desired Java executable (Java 8 for SV-COMP benchmarks).
		// Defaults to "java" (uses PATH) when the user did not supply --java.
		String exec = (java8Executable != null && !java8Executable.isBlank())
				? absolute(java8Executable)
				: "java";

		ListeningConnector connector = findListeningConnector();
		Map<String, Connector.Argument> args = connector.defaultArguments();
		args.get("localAddress").setValue("127.0.0.1");
		args.get("port").setValue("0");
		args.get("timeout").setValue(CONNECT_TIMEOUT_MILLIS);

		Path workingDir;
		try {
			workingDir = Files.createTempDirectory("jlisa-wit-run-");
		} catch (IOException e) {
			throw new ValidationException("Cannot create a working directory for the target JVM: " + e.getMessage(), e);
		}

		Process process = null;
		try {
			String address = connector.startListening(args);
			List<String> command = new ArrayList<>();
			command.add(exec);
			// Start suspended so we can register event requests before any code
			// runs.
			command.add("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=" + address);
			if (assertions) {
				command.add("-ea");
			}
			command.add("-cp");
			command.add(absoluteClasspath(classpath));
			command.add(mainClass);
			ValidatorLogger.jdi("Launching JVM in {}: {}", workingDir, command);

			// The output is not needed, since the validator observes the
			// program through JDI
			// events. Standard input stays an open pipe that is never written,
			// as with the JDI
			// launching connector.
			process = new ProcessBuilder(command)
					.directory(workingDir.toFile())
					.redirectOutput(ProcessBuilder.Redirect.DISCARD)
					.redirectError(ProcessBuilder.Redirect.DISCARD)
					.start();
			WORKING_DIRS.put(process, workingDir);
			// a target JVM that exits before connecting must not leave accept
			// waiting
			Process started = process;
			process.onExit().thenRun(() -> {
				stopListening(connector, args);
				delete(workingDir);
				WORKING_DIRS.remove(started);
			});

			VirtualMachine vm = connector.accept(args);
			stopListening(connector, args);
			PROCESSES.put(vm, process);
			Process launched = process;
			process.onExit().thenRun(() -> PROCESSES.remove(vm, launched));
			ValidatorLogger.jdi("Connected to target VM ({})", vm.version());
			return vm;
		} catch (Exception e) {
			stopListening(connector, args);
			if (process != null) {
				process.destroyForcibly();
			} else {
				delete(workingDir);
			}
			throw new ValidationException("Failed to launch target JVM: " + e.getMessage(), e);
		}
	}

	/**
	 * Stops the target JVM of the given VM, if it is still running: asks it to
	 * exit, then kills its process.
	 */
	public static void stop(
			VirtualMachine vm) {
		try {
			vm.exit(1);
		} catch (Exception ignored) {
			// already gone
		}
		Process process = PROCESSES.remove(vm);
		if (process != null) {
			process.destroyForcibly();
		}
	}

	// ------------------------------------------------------------------
	// private helpers
	// ------------------------------------------------------------------

	/**
	 * Returns the JDI connector that listens on a socket
	 * ({@code com.sun.jdi.SocketListen}).
	 */
	private static ListeningConnector findListeningConnector() throws ValidationException {
		for (ListeningConnector connector : Bootstrap.virtualMachineManager().listeningConnectors()) {
			if (connector.transport().name().equals("dt_socket")) {
				return connector;
			}
		}
		throw new ValidationException("No JDI socket ListeningConnector found in this JDK");
	}

	private static void stopListening(
			ListeningConnector connector,
			Map<String, Connector.Argument> args) {
		try {
			connector.stopListening(args);
		} catch (Exception ignored) {
			// not listening any more
		}
	}

	/**
	 * Returns the given path as an absolute path, if it is a path and not a
	 * command name.
	 */
	private static String absolute(
			String path) {
		return path.contains(File.separator) ? new File(path).getAbsolutePath() : path;
	}

	/**
	 * Returns the classpath with absolute entries: the target JVM runs in
	 * another directory.
	 */
	private static String absoluteClasspath(
			String cp) {
		if (cp == null || cp.isBlank()) {
			cp = ".";
		}
		return Arrays.stream(cp.split(File.pathSeparator))
				.filter(entry -> !entry.isEmpty())
				.map(entry -> new File(entry).getAbsolutePath())
				.collect(Collectors.joining(File.pathSeparator));
	}

	private static void delete(
			Path dir) {
		try (Stream<Path> paths = Files.walk(dir)) {
			paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
		} catch (IOException | UncheckedIOException e) {
			// a temporary directory
		}
	}

	// ------------------------------------------------------------------
	// compilation helper (no shell)
	// ------------------------------------------------------------------

	/**
	 * Compiles the Java sources in {@code sourceDir} — and optionally
	 * additional sources rooted at {@code extraSourcesDir} — using the
	 * in-process Java Compiler API
	 * ({@link javax.tools.ToolProvider#getSystemJavaCompiler()}) — no
	 * {@code javac} subprocess.
	 * <p>
	 * Produces Java 8 bytecode ({@code --release 8}) with full debug
	 * information ({@code -g}) required for JDI line-level breakpoints.
	 * <p>
	 * All class files are written to {@code outputDir}. For extra sources that
	 * declare a package (e.g. {@code org.sosy_lab.sv_benchmarks.Verifier}), the
	 * compiler creates the corresponding sub-directory tree under
	 * {@code outputDir} automatically, so the classpath entry {@code outputDir}
	 * is sufficient for the launched JVM to resolve all classes.
	 *
	 * @param sourceDir      directory containing {@code .java} source files
	 *                           (non-recursive)
	 * @param outputDir      directory to write {@code .class} files into (may
	 *                           equal sourceDir)
	 * @param extraClasspath additional classpath entries
	 *                           (colon/semicolon-separated), or {@code null}
	 * @param extraSources   optional root of an additional source tree to
	 *                           compile together with the benchmark (e.g. the
	 *                           SV-COMP {@code common/} directory); sources are
	 *                           collected recursively; may be {@code null}
	 * 
	 * @throws ValidationException if compilation fails or the Java compiler is
	 *                                 unavailable
	 */
	public static void compileIfNeeded(
			File sourceDir,
			File outputDir,
			String extraClasspath,
			List<File> extraSources)
			throws ValidationException {

		// Collect .java files from sourceDir (flat)
		java.util.List<File> recSourceDir = new ArrayList<>();
		collectJavaSources(sourceDir, recSourceDir);

		// File[] directSources = recSourceDir; // sourceDir.listFiles((dir,
		// name) -> name.endsWith(".java"));
		if (recSourceDir.isEmpty()) {
			throw new ValidationException("No .java source files found in " + sourceDir.getAbsolutePath());
		}

		// Check if .class files already exist at the top level — skip if so
		File[] existing = sourceDir.listFiles((
				dir,
				name) -> name.endsWith(".class"));
		if (existing != null && existing.length > 0) {
			ValidatorLogger.jdi("Skipping compilation — .class files already present in {}",
					sourceDir.getAbsolutePath());
			return;
		}

		// Accumulate all sources: benchmark dir (flat) + extra sources dir
		// (recursive)
		java.util.List<File> allSources = recSourceDir;

		if (extraSources != null) {
			for (File source : extraSources) {
				if (source != null && source.isDirectory()) {
					int before = allSources.size();
					collectJavaSources(source, allSources);
					ValidatorLogger.jdi("Extra sources from {}: {} files added",
							source.getAbsolutePath(), allSources.size() - before);
				}
			}
		}

		javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new ValidationException(
					"Java compiler (tools.jar / java.compiler module) is not available. "
							+ "Run the validator with a JDK, not a JRE.");
		}

		ValidatorLogger.jdi("Compiling {} .java file(s) → {}", allSources.size(),
				outputDir.getAbsolutePath());

		javax.tools.DiagnosticCollector<
				javax.tools.JavaFileObject> diagnostics = new javax.tools.DiagnosticCollector<>();

		try (javax.tools.StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null)) {

			fm.setLocationFromPaths(
					javax.tools.StandardLocation.CLASS_OUTPUT,
					java.util.List.of(outputDir.toPath()));

			Iterable<? extends javax.tools.JavaFileObject> units = fm
					.getJavaFileObjects(allSources.toArray(new File[0]));

			java.util.List<String> options = new java.util.ArrayList<>(
					java.util.List.of("-source", "8", "-target", "8", "-g"));
			if (extraClasspath != null && !extraClasspath.isBlank()) {
				options.add("-cp");
				options.add(extraClasspath);
			}

			boolean ok = compiler.getTask(null, fm, diagnostics, options, null, units).call();

			if (!ok) {
				StringBuilder sb = new StringBuilder("Compilation failed:\n");
				for (javax.tools.Diagnostic<?> d : diagnostics.getDiagnostics()) {
					if (d.getKind() == javax.tools.Diagnostic.Kind.ERROR) {
						sb.append("  ").append(d).append('\n');
					}
				}
				throw new ValidationException(sb.toString());
			}

		} catch (java.io.IOException e) {
			throw new ValidationException("Compiler I/O error: " + e.getMessage(), e);
		}

		ValidatorLogger.jdi("Compilation successful → {}", outputDir.getAbsolutePath());
	}

	/**
	 * Recursively collects all {@code .java} files under {@code dir} and
	 * appends them to {@code result}.
	 */
	private static void collectJavaSources(
			File dir,
			java.util.List<File> result) {
		File[] entries = dir.listFiles();
		if (entries == null) {
			return;
		}
		for (File f : entries) {
			if (f.isDirectory()) {
				collectJavaSources(f, result);
			} else if (f.getName().endsWith(".java")) {
				result.add(f);
			}
		}
	}
}

package it.unive.jlisa.witness.validator.violation;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import it.unive.jlisa.witness.validator.ValidationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Launches the target JVM under JDI control using the standard
 * {@link LaunchingConnector} API — no {@link ProcessBuilder}, no shell invocations.
 *
 * <h3>Cross-version operation</h3>
 * <p>The validator runs on Java 23 but can debug a Java 8 JVM by setting the
 * {@code vmexec} connector argument to a Java 8 executable.  JDWP (Java Debug Wire
 * Protocol) is backwards-compatible, so Java 23 JDI can control a Java 8 target
 * transparently.
 *
 * <h3>Assertions</h3>
 * <p>The target JVM is always launched with {@code -ea} so that {@code assert}
 * statements throw {@link AssertionError} (required for the {@code valid_assert}
 * property).
 */
public final class JdiLauncher {

    private JdiLauncher() {
    }

    /**
     * Launches the target program and returns a connected, suspended {@link VirtualMachine}.
     *
     * <p>The VM starts suspended: no user code runs until the caller calls
     * {@link VirtualMachine#resume()}.  This window is used to register event requests
     * before execution begins.
     *
     * @param mainClass       fully-qualified main class name (e.g. {@code "Main"})
     * @param classpath       class-path string to pass to the target JVM
     * @param java8Executable path to the Java executable for the target JVM, or {@code null}
     *                        to use the {@code java} found on {@code PATH}
     * @return a connected, suspended {@link VirtualMachine}
     * @throws ValidationException if the VM cannot be launched
     */
    public static VirtualMachine launch(
            String mainClass,
            String classpath,
            String java8Executable) throws ValidationException {

        LaunchingConnector connector = findLaunchingConnector();
        Map<String, Connector.Argument> args = connector.defaultArguments();

        args.get("main").setValue(mainClass);
        // Enable assertions (-ea) and set classpath; the connector appends these to
        // the JVM command line before starting the process.
        args.get("options").setValue("-ea -cp " + quoteIfNeeded(classpath));
        // Start suspended so we can register event requests before any code runs.
        args.get("suspend").setValue("true");

        // Point at the desired Java executable (Java 8 for SV-COMP benchmarks).
        // Defaults to "java" (uses PATH) when the user did not supply --java.
        String exec = (java8Executable != null && !java8Executable.isBlank())
                ? java8Executable
                : "java";
        args.get("vmexec").setValue(exec);

        ValidatorLogger.jdi("Launching JVM: vmexec={} main={} options={}",
                exec, mainClass, args.get("options").value());

        try {
            VirtualMachine vm = connector.launch(args);
            ValidatorLogger.jdi("Connected to target VM ({})",
                    vm.version());
            return vm;
        } catch (Exception e) {
            throw new ValidationException("Failed to launch target JVM: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    /**
     * Returns the default {@link LaunchingConnector} (typically
     * {@code com.sun.jdi.connect.CommandLineLaunch}).
     */
    private static LaunchingConnector findLaunchingConnector() throws ValidationException {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        if (connector == null) {
            throw new ValidationException("No default JDI LaunchingConnector found in this JDK");
        }
        return connector;
    }

    /**
     * Wraps a classpath in double quotes when it contains spaces, to prevent the connector
     * from splitting it into multiple arguments.
     */
    private static String quoteIfNeeded(String cp) {
        if (cp == null || cp.isBlank()) {
            return ".";
        }
        return cp.contains(" ") ? "\"" + cp + "\"" : cp;
    }

    // ------------------------------------------------------------------
    // compilation helper (no shell)
    // ------------------------------------------------------------------

    /**
     * Compiles the Java sources in {@code sourceDir} — and optionally additional sources rooted
     * at {@code extraSourcesDir} — using the in-process Java Compiler API
     * ({@link javax.tools.ToolProvider#getSystemJavaCompiler()}) — no {@code javac} subprocess.
     *
     * <p>Produces Java 8 bytecode ({@code --release 8}) with full debug information
     * ({@code -g}) required for JDI line-level breakpoints.
     *
     * <p>All class files are written to {@code outputDir}.  For extra sources that declare a
     * package (e.g. {@code org.sosy_lab.sv_benchmarks.Verifier}), the compiler creates the
     * corresponding sub-directory tree under {@code outputDir} automatically, so the classpath
     * entry {@code outputDir} is sufficient for the launched JVM to resolve all classes.
     *
     * @param sourceDir      directory containing {@code .java} source files (non-recursive)
     * @param outputDir      directory to write {@code .class} files into (may equal sourceDir)
     * @param extraClasspath additional classpath entries (colon/semicolon-separated), or {@code null}
     * @param extraSources   optional root of an additional source tree to compile together with the
     *                       benchmark (e.g. the SV-COMP {@code common/} directory); sources are
     *                       collected recursively; may be {@code null}
     * @throws ValidationException if compilation fails or the Java compiler is unavailable
     */
    public static void compileIfNeeded(File sourceDir, File outputDir, String extraClasspath,
                                       List<File> extraSources)
            throws ValidationException {

        // Collect .java files from sourceDir (flat)
        java.util.List<File> recSourceDir = new ArrayList<>();
        collectJavaSources(sourceDir, recSourceDir);

        //File[] directSources = recSourceDir; // sourceDir.listFiles((dir, name) -> name.endsWith(".java"));
        if (recSourceDir.isEmpty()) {
            throw new ValidationException("No .java source files found in " + sourceDir.getAbsolutePath());
        }

        // Check if .class files already exist at the top level — skip if so
        File[] existing = sourceDir.listFiles((dir, name) -> name.endsWith(".class"));
        if (existing != null && existing.length > 0) {
            ValidatorLogger.jdi("Skipping compilation — .class files already present in {}",
                    sourceDir.getAbsolutePath());
            return;
        }

        // Accumulate all sources: benchmark dir (flat) + extra sources dir (recursive)
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

        javax.tools.DiagnosticCollector<javax.tools.JavaFileObject> diagnostics =
                new javax.tools.DiagnosticCollector<>();

        try (javax.tools.StandardJavaFileManager fm =
                     compiler.getStandardFileManager(diagnostics, null, null)) {

            fm.setLocationFromPaths(
                    javax.tools.StandardLocation.CLASS_OUTPUT,
                    java.util.List.of(outputDir.toPath()));

            Iterable<? extends javax.tools.JavaFileObject> units =
                    fm.getJavaFileObjects(allSources.toArray(new File[0]));

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
     * Recursively collects all {@code .java} files under {@code dir} and appends them to
     * {@code result}.
     */
    private static void collectJavaSources(File dir, java.util.List<File> result) {
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

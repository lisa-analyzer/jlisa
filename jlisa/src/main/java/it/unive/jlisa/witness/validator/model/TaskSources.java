package it.unive.jlisa.witness.validator.model;

import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.parser.BenchmarkPropertiesParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The sources of a task and where its program is compiled.
 *
 * @param inputs     the input directories of the task, compiled together
 * @param benchmark  the input with the main class
 * @param classesDir where the program is compiled and run from
 */
public record TaskSources(
		List<File> inputs,
		File benchmark,
		File classesDir) {

	/**
	 * The task of an SV-COMP task file ({@code --benchmark}): its benchmark
	 * directory is the input named as the task file, and the program is
	 * compiled into it.
	 *
	 * @param extraSourcesDir more sources to compile with the task, or null
	 */
	public static TaskSources fromTaskFile(
			File taskFile,
			File extraSourcesDir)
			throws ValidationException {
		BenchmarkProperties properties;
		try {
			properties = BenchmarkPropertiesParser.parse(taskFile);
		} catch (FileNotFoundException e) {
			throw new ValidationException(e.getMessage());
		}
		List<File> inputs = new ArrayList<>();
		if (extraSourcesDir != null) {
			inputs.add(extraSourcesDir);
		}
		inputs.addAll(properties.inputFiles());
		return new TaskSources(inputs, properties.directory(), properties.directory());
	}

	/**
	 * The task of the given inputs, as SV-COMP gives them to the tool
	 * ({@code --source}): the benchmark is the input with {@code Main.java}
	 * (the last input if none has it), an input inside another input is left
	 * out (it is compiled with its parent), and the program is compiled into a
	 * temporary directory, deleted when the tool exits, so that the inputs are
	 * only read.
	 */
	public static TaskSources fromInputs(
			List<String> paths)
			throws ValidationException {
		List<File> files = new ArrayList<>();
		for (String path : paths) {
			files.add(new File(path).getAbsoluteFile().toPath().normalize().toFile());
		}
		List<File> inputs = new ArrayList<>();
		for (File f : files) {
			boolean nested = files.stream()
					.anyMatch(other -> !other.equals(f) && f.toPath().startsWith(other.toPath()));
			if (!nested && f.isDirectory()) {
				inputs.add(f);
			}
		}
		if (inputs.isEmpty()) {
			throw new ValidationException("No input directory among " + paths);
		}
		File benchmark = inputs.stream().filter(f -> new File(f, "Main.java").isFile()).findFirst()
				.orElse(inputs.getLast());
		File classesDir;
		try {
			classesDir = Files.createTempDirectory("jlisa-wit-").toFile();
		} catch (IOException e) {
			throw new ValidationException("Cannot create a directory for the classes: " + e.getMessage(), e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> delete(classesDir.toPath())));
		return new TaskSources(List.copyOf(inputs), benchmark, classesDir);
	}

	private static void delete(
			Path dir) {
		try (Stream<Path> paths = Files.walk(dir)) {
			paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
		} catch (IOException | UncheckedIOException e) {
			// a temporary directory
		}
	}
}

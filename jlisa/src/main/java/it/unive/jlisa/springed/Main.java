package it.unive.jlisa.springed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.unive.jlisa.springed.exceptions.SpringCSVExceptionWriter;
import it.unive.jlisa.springed.frontend.SpringFrontend;
import it.unive.jlisa.springed.frontend.SpringProjectVisitor;
import it.unive.jlisa.springed.p1.P1Impl;
import it.unive.jlisa.springed.p1.constructs.Registry;
import it.unive.jlisa.springed.p1.output.P1Output;
import it.unive.lisa.program.Unit;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class Main {
	private static final String OUTPUT_DIR = "spring-outputs";
	private static final String OUTPUT_FILE_NAME = "registry.json";
	private static final String ERROR_FILE_NAME = "errors-from-{}.csv";

	private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger(Main.class);
	private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	public static void main(
			String[] args)
			throws IOException {

		if (!isInputOk(args))
			return;

		Path rootPath = Paths.get(args[0]).normalize();
		List<Path> springProjects = detectSpringProjects(rootPath);

		P1Output p1Output = new P1Output();
		for (Path project : springProjects) {
			String projectName = projectName(project);
			SpringFrontend springFrontend = new SpringFrontend();

			Path sourceRoot = project.resolve("src/main/java");
			Unit[] projectUnits = springFrontend.parse(sourceRoot.toString());

			Registry registry = new P1Impl().produceRegistry(projectUnits);

			p1Output.addRegistry(projectName, registry);
			dumpCollectedErrors(springFrontend, projectName);
		}

		writeOutput(p1Output);
	}

	public static List<Path> detectSpringProjects(
			Path rootPath)
			throws IOException {
		SpringProjectVisitor springProjectVisitor = new SpringProjectVisitor(rootPath);
		Files.walkFileTree(rootPath, springProjectVisitor);

		List<Path> projects = springProjectVisitor.getProjects();

		if (projects.isEmpty())
			LOG.warn("No Spring project (build file + src/main/java) found under {}", rootPath);

		return projects;
	}

	private static void writeOutput(
			P1Output output)
			throws IOException {
		Path out = Paths.get(OUTPUT_DIR, OUTPUT_FILE_NAME);
		Files.createDirectories(out.getParent());

		MAPPER.writeValue(out.toFile(), output);

		int mappings = output.get().values().stream()
				.mapToInt(r -> r.getMappings().size())
				.sum();

		LOG.info("Wrote {} project(s), {} mapping(s) total, to {}",
				output.get().size(), mappings, out.toAbsolutePath());
	}

	private static boolean isInputOk(
			String[] args) {
		if (args.length == 0) {
			LOG.error("usage: <spring-boot-source-path>");
			return false;
		}

		return true;
	}

	private static String projectName(
			Path projectDir) {

		for (int i = 1; i < projectDir.getNameCount(); i++)
			if (projectDir.getName(i).toString().equals("src"))
				return projectDir.getName(i - 1).toString();

		return projectDir.getFileName().toString().toLowerCase();
	}

	private static void dumpCollectedErrors(
			SpringFrontend springFrontend,
			String projectName) {
		List<Throwable> parseExceptions = springFrontend.getParseExceptions();

		if (!parseExceptions.isEmpty()) {
			Path errors = Paths.get(OUTPUT_DIR, ERROR_FILE_NAME.replace("{}", projectName));
			SpringCSVExceptionWriter.writeCSV(errors.toString(), parseExceptions);

			LOG.warn("Collected {} parsing issue(s); written to {}",
					parseExceptions.size(), errors.toAbsolutePath());
		}
	}
}

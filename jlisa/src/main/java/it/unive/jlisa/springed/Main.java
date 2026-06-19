package it.unive.jlisa.springed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.jlisa.springed.frontend.SpringFrontend;
import it.unive.jlisa.springed.p1.P1Impl;
import it.unive.jlisa.springed.p1.constructs.Registry;
import it.unive.jlisa.springed.p1.output.P1Output;
import it.unive.lisa.program.Unit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class Main {
	private static final String OUTPUT_DIR = "spring-outputs";
	private static final String OUTPUT_FILE_NAME = "-registry.json";
	private static final String ERROR_FILE_NAME = "-errors.csv";

	private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger(Main.class);

	private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	private static final SpringFrontend FRONTEND = new SpringFrontend();
	private static final P1Impl P1 = new P1Impl();

	public static void main(
			String[] args)
			throws IOException {

		if (!areArgsOk(args))
			return;

		String source = args[0];
		String projectName = projectName(source);

		P1Output p1Output = run(source, projectName);
		writeOutput(p1Output, projectName);
		dumpCollectedErrors(projectName);
	}

	private static P1Output run(
			String source,
			String projectName)
			throws IOException {
		P1Output output = new P1Output();

		Unit[] units = FRONTEND.parse(source);
		Registry registry = P1.produceRegistry(units);
		output.addRegistry(projectName, registry);

		return output;
	}

	private static void writeOutput(
			P1Output output,
			String projectName)
			throws IOException {
		Path out = Paths.get(OUTPUT_DIR, projectName + OUTPUT_FILE_NAME);
		Files.createDirectories(out.getParent());

		MAPPER.writeValue(out.toFile(), output);

		int mappings = output.get().values().stream()
				.mapToInt(r -> r.getMappings().size())
				.sum();

		LOG.info("Wrote " + output.get().size() + " project(s), " + mappings
				+ " mapping(s) total, to " + out.toAbsolutePath());
	}

	private static boolean areArgsOk(
			String[] args) {
		if (args.length == 0) {
			LOG.error("usage: <spring-boot-source-path>");
			return false;
		}

		return true;
	}

	private static String projectName(
			String source) {
		Path p = Paths.get(source).normalize();

		for (int i = 1; i < p.getNameCount(); i++)
			if (p.getName(i).toString().equals("src"))
				return p.getName(i - 1).toString();

		return p.getFileName().toString();
	}

	private static void dumpCollectedErrors(
			String projectName) {
		List<Throwable> parseExceptions = FRONTEND.getParseExceptions();

		if (!parseExceptions.isEmpty()) {
			Path errors = Paths.get(OUTPUT_DIR, projectName + ERROR_FILE_NAME);
			CSVExceptionWriter.writeCSV(errors.toString(), parseExceptions);

			LOG.warn("Collected " + parseExceptions.size() + " parsing issue(s); written to "
					+ errors.toAbsolutePath());
		}
	}
}

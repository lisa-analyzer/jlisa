package it.unive.jlisa.springed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.unive.jlisa.springed.frontend.SpringFrontend;
import it.unive.jlisa.springed.p1.output.P1Output;
import it.unive.jlisa.springed.p1.P1Impl;
import it.unive.jlisa.springed.p1.constructs.Registry;
import it.unive.lisa.program.Unit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

	private static final String OUTPUT_DIR = "spring-outputs";
	private static final String OUTPUT_FILE_NAME = "registry.json";

	private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	private static final SpringFrontend FRONTEND = new SpringFrontend();
	private static final P1Impl P1 = new P1Impl();

	public static void main(
			String[] args)
			throws IOException {

		if (!areArgsOk(args)) return;

		writeOutput(runAnalysis(args));
	}

	private static P1Output runAnalysis(String[] args) throws IOException {
		P1Output output = new P1Output();

		for (String source : args) {
			Unit[] units = FRONTEND.parse(source);
			Registry registry = P1.produceRegistry(units);

			output.addRegistry(projectName(source), registry);
		}

		return output;
	}

	private static void writeOutput(P1Output output) throws IOException {

		Path out = Paths.get(OUTPUT_DIR, OUTPUT_FILE_NAME);
		Files.createDirectories(out.getParent());

		MAPPER.writeValue(out.toFile(), output);

		int mappings = output.get().values().stream()
				.mapToInt(r -> r.getMappings().size())
				.sum();

		System.out.println("Wrote " + output.get().size() + " project(s), " + mappings
				+ " mapping(s) total, to " + out.toAbsolutePath());
	}

	private static boolean areArgsOk(String[] args) {
		if (args.length == 0) {
			System.out.println("usage: <spring-boot-source-path> [more paths...]");
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
}

package it.unive.jlisa.springed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.unive.jlisa.springed.frontend.SpringFrontend;
import it.unive.jlisa.springed.p1.P1Impl;
import it.unive.jlisa.springed.p1.constructs.Mapping;
import it.unive.jlisa.springed.p1.constructs.Registry;
import it.unive.lisa.program.Unit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Main {

	public static void main(
			String[] args)
			throws IOException {

		if (args.length == 0) {
			System.out.println("usage: <spring-boot-source-path> [more paths...]");
			return;
		}

		Map<String, Object> root = new LinkedHashMap<>();
		int total = 0;

		for (String source : args) {
			Unit[] units = new SpringFrontend().parse(source);
			Registry registry = new P1Impl().produceRegistry(units);

			Map<String, Mapping> byMethod = new LinkedHashMap<>();
			for (Mapping mapping : registry.getMappings())
				byMethod.put(mapping.getMethod().getDescriptor().getName(), mapping);

			root.put(projectName(source), Map.of("registry", byMethod));
			total += byMethod.size();
		}

		String fileName = "registry-" + UUID.randomUUID().toString().substring(0, 8) + ".json";
		Path out = Paths.get("spring-outputs", fileName);
		Files.createDirectories(out.getParent());

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		mapper.writeValue(out.toFile(), root);

		System.out.println("Wrote " + root.size() + " project(s), " + total
				+ " mapping(s) total, to " + out.toAbsolutePath());
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

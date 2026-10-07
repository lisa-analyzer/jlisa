package it.unive.jlisa.springed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MainTest {

	private static final Path OUTPUT = Path.of("spring-outputs", "registry.json");

	private static final Map<String, Integer> MAPPINGS = new TreeMap<>(Map.of(
			"case-1", 1,
			"case-2", 4,
			"case-3", 4,
			"case-4", 20,
			"case-5", 3));

	@TempDir
	static Path cases;

	@BeforeAll
	public static void runMain() throws IOException {
		for (String name : MAPPINGS.keySet())
			SpringTestHelper.extract(name, cases);

		Main.main(new String[] { cases.toString() });
	}

	@AfterAll
	public static void cleanUp() throws IOException {
		Files.deleteIfExists(OUTPUT);

		try {
			Files.deleteIfExists(OUTPUT.getParent());
		} catch (DirectoryNotEmptyException keepOtherOutputs) {
			// the output directory holds other files; leave it in place
		}
	}

	@Test
	public void writesOneRegistryPerProject() throws IOException {
		Map<String, Integer> mappings = new TreeMap<>();
		readOutput().fields().forEachRemaining(project -> mappings.put(project.getKey(), project.getValue().size()));

		assertEquals(MAPPINGS, mappings);
	}

	@Test
	public void case1JsonOutput() throws IOException {
		JsonNode registry = readOutput().get("case-1");
		assertNotNull(registry, "missing project 'case-1'");

		assertEquals(1, registry.size(), () -> "unexpected mappings: " + registry);
		JsonNode mapping = registry.get("Controller_endpoint1");
		assertNotNull(mapping, () -> "missing mapping 'Controller_endpoint1' in " + registry);

		assertEquals(
				"it.unive.jlisa.jlisa.testcases.case_1.controllers.Controller::endpoint1",
				mapping.get("method").asText());

		JsonNode annotation = mapping.get("annotation");
		assertNotNull(annotation, () -> "missing annotation in " + mapping);
		assertEquals(new ObjectMapper().readTree("""
				{
				  "methods" : [ "GET" ],
				  "paths" : [ "/hello-world" ],
				  "params" : [ ],
				  "headers" : [ ],
				  "consumes" : [ ],
				  "produces" : [ ],
				  "version" : ""
				}"""), annotation);
	}

	private static JsonNode readOutput() throws IOException {
		assertTrue(Files.isRegularFile(OUTPUT), () -> "expected output file at " + OUTPUT.toAbsolutePath());
		return new ObjectMapper().readTree(Files.readString(OUTPUT));
	}
}

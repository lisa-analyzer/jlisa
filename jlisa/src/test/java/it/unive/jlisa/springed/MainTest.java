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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MainTest {

	private static final Path OUTPUT = Path.of("spring-outputs", "registry.json");

	@BeforeAll
	public static void unpackCase() throws IOException {
		SpringTestCases.extract("case-1");
	}

	@AfterAll
	public static void cleanUp() throws IOException {
		SpringTestCases.delete(SpringTestCases.ROOT.resolve("case-1"));
		Files.deleteIfExists(OUTPUT);

		try {
			Files.deleteIfExists(OUTPUT.getParent());
		} catch (DirectoryNotEmptyException keepOtherOutputs) {
			// the output directory holds other files; leave it in place
		}
	}

	@Test
	public void case1JsonOutput() throws IOException {
		Main.main(new String[] { "spring-testcases/case-1/src/main/java" });
		assertTrue(Files.isRegularFile(OUTPUT), () -> "expected output file at " + OUTPUT.toAbsolutePath());

		String json = Files.readString(OUTPUT);
		JsonNode root = new ObjectMapper().readTree(json);

		assertEquals(1, root.size(), () -> "unexpected projects: " + root);
		JsonNode registry = root.get("case-1");
		assertNotNull(registry, () -> "missing project 'case-1' in " + root);

		assertEquals(1, registry.size(), () -> "unexpected mappings: " + registry);
		JsonNode mapping = registry.get("Controller_endpoint1");
		assertNotNull(mapping, () -> "missing mapping 'Controller_endpoint1' in " + registry);

		assertEquals(
				"it.unive.jlisa.jlisa.testcases.case_1.controllers.Controller::endpoint1",
				mapping.get("method").asText());

		JsonNode annotation = mapping.get("annotation");
		assertNotNull(annotation, () -> "missing annotation in " + mapping);
		assertEquals("GET", annotation.get("httpMethod").asText());
		assertEquals("/hello-world", annotation.get("addressPath").asText());
	}
}

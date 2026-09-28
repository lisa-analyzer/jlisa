package it.unive.jlisa.springed.regression;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unive.jlisa.springed.p1.NewP1Impl;
import it.unive.jlisa.springed.p1.P1Impl;
import it.unive.jlisa.springed.p1.constructs.RequestMapping;
import it.unive.jlisa.springed.regression.SpringFixtures.Parsed;
import it.unive.lisa.program.Unit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Golden-master regression tests over the sample Spring projects in
 * {@code spring-testcases}. They pin what {@code SpringFrontend} and P1 produce
 * on the springed branch, so that behaviour changes brought in by a merge of
 * master show up as snapshot diffs. The anchors re-state, with explicit
 * assertions, the facts that matter the most. See {@link Snapshots} for how to
 * run the suite and re-record the baseline.
 */
public class SpringCasesRegressionTest {

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "case-1", "case-2", "case-3", "case-4" })
	public void matchesRecordedBaseline(
			String caseName,
			@TempDir Path workDir)
			throws IOException {
		Parsed parsed = SpringFixtures.parseCase(caseName, workDir);

		String model = SpringDump.normalize(
				SpringDump.frontendModel(caseName, parsed.frontend(), parsed.units(), SpringDump.CASES_PACKAGE),
				workDir);
		String errors = SpringDump.parseErrors(parsed.frontend().getParseExceptions(),
				workDir.resolve("errors.csv"), workDir);
		String p1 = SpringDump.normalize(SpringDump.p1(caseName, parsed.units()), workDir);

		assertAll(
				() -> Snapshots.assertMatches(caseName + "/frontend-model.txt", model),
				() -> Snapshots.assertMatches(caseName + "/parse-errors.csv", errors),
				() -> Snapshots.assertMatches(caseName + "/p1.txt", p1));
	}

	@Test
	public void reparsingInTheSameJvmIsStable(
			@TempDir Path workDir)
			throws IOException {
		String first = dump("case-1", workDir.resolve("first"));
		dump("case-4", workDir.resolve("second"));
		String again = dump("case-1", workDir.resolve("third"));

		assertEquals(first, again, "parsing another project in the same JVM changed the outcome for case-1");
	}

	@Test
	public void case1Anchors(
			@TempDir Path workDir)
			throws IOException {
		Parsed parsed = SpringFixtures.parseCase("case-1", workDir);

		assertEquals(Map.of("Controller::endpoint1", "GET /hello-world"), summaries(parsed.units()));
		assertEquals(List.of("GET /hello-world"), legacyMappings(parsed.units()));

		String json = SpringDump.registryJson("case-1", new P1Impl().produceRegistry(parsed.units()));
		JsonNode root = new ObjectMapper().readTree(json);
		assertEquals(1, root.size(), () -> "unexpected projects: " + root);
		JsonNode registry = root.get("case-1");
		assertNotNull(registry, () -> "missing project 'case-1' in " + root);
		assertEquals(1, registry.size(), () -> "unexpected mappings: " + registry);
		JsonNode mapping = registry.get("Controller_endpoint1");
		assertNotNull(mapping, () -> "missing mapping 'Controller_endpoint1' in " + registry);
		assertEquals("it.unive.jlisa.jlisa.testcases.case_1.controllers.Controller::endpoint1",
				mapping.get("method").asText());
		assertEquals("GET", mapping.get("annotation").get("httpMethod").asText());
		assertEquals("/hello-world", mapping.get("annotation").get("addressPath").asText());
	}

	@Test
	public void case2Anchors(
			@TempDir Path workDir)
			throws IOException {
		Parsed parsed = SpringFixtures.parseCase("case-2", workDir);

		// updateUser declares its path through the "path" alias
		assertEquals(Map.of(
				"UserController::listUsers", "GET /users",
				"UserController::createUser", "POST /users",
				"UserController::updateUser", "PUT /users/{id}",
				"UserController::deleteUser", "DELETE /users/{id}"),
				summaries(parsed.units()));
		assertEquals(List.of("DELETE /users/{id}", "GET /users", "POST /users", "PUT /users/{id}"),
				legacyMappings(parsed.units()));
	}

	@Test
	public void case3Anchors(
			@TempDir Path workDir)
			throws IOException {
		Parsed parsed = SpringFixtures.parseCase("case-3", workDir);

		// ClientLayer is a @Controller, PublicLayer a @RestController
		assertEquals(List.of(
				"it.unive.jlisa.jlisa.testcases.case_3.controllers.ClientLayer",
				"it.unive.jlisa.jlisa.testcases.case_3.controllers.PublicLayer"),
				new NewP1Impl().getControllers(parsed.units()).stream().map(Unit::getName).sorted().toList());
		assertEquals(Map.of(
				"ClientLayer::status", "GET /client/status",
				"ClientLayer::patchProfile", "PATCH /client/profile",
				"PublicLayer::info", "GET /public/info",
				"PublicLayer::submitFeedback", "POST /public/feedback"),
				summaries(parsed.units()));
		assertEquals(List.of("GET /client/status", "GET /public/info", "PATCH /client/profile",
				"POST /public/feedback"), legacyMappings(parsed.units()));
	}

	@Test
	public void case4Anchors(
			@TempDir Path workDir)
			throws IOException {
		Parsed parsed = SpringFixtures.parseCase("case-4", workDir);
		Map<String, RequestMapping> mappings = SpringFixtures.newP1Mappings(parsed.units());

		// every method but endpoint20 and endpoint22 is mapped
		assertEquals(20, mappings.size(), () -> "unexpected mappings: " + mappings.keySet());
		assertEquals(20, new NewP1Impl().p1(parsed.units()).size());
		assertFalse(mappings.containsKey("Controller::endpoint20"));
		assertFalse(mappings.containsKey("Controller::endpoint22"));

		// class-level @RequestMapping merged into the method-level one,
		// MediaType constants folded through the spring-libraries stubs
		RequestMapping endpoint1 = mappings.get("Controller::endpoint1");
		assertEquals(Set.of("GET"), endpoint1.getMethods());
		assertEquals(Set.of("/api/hello-world", "/api/legacy/hello-world"), endpoint1.getPaths());
		assertEquals(Set.of("!disabled"), endpoint1.getParams());
		assertEquals(Set.of("!X-Blocked"), endpoint1.getHeaders());
		assertEquals(Set.of("application/json", "text/*"), endpoint1.getConsumes());
		assertEquals(Set.of("text/plain"), endpoint1.getProduces());
		assertEquals(Set.of("1.0+"), endpoint1.getVersion());

		// method-level consumes overrides the class-level one
		assertEquals(Set.of("application/json"), mappings.get("Controller::endpoint2").getConsumes());

		// RequestMethod constants folded through the spring-libraries stubs
		assertEquals(Set.of("GET", "POST"), mappings.get("Controller::endpoint6").getMethods());
		assertEquals(Set.of("OPTIONS"), mappings.get("Controller::endpoint17").getMethods());
		assertEquals(Set.of("HEAD"), mappings.get("Controller::endpoint18").getMethods());
		assertEquals(Set.of("TRACE"), mappings.get("Controller::endpoint19").getMethods());
	}

	private static String dump(
			String caseName,
			Path workDir)
			throws IOException {
		Parsed parsed = SpringFixtures.parseCase(caseName, workDir);
		return SpringDump.normalize(
				SpringDump.frontendModel(caseName, parsed.frontend(), parsed.units(), SpringDump.CASES_PACKAGE)
						+ SpringDump.p1(caseName, parsed.units()),
				workDir)
				+ SpringDump.parseErrors(parsed.frontend().getParseExceptions(), workDir.resolve("errors.csv"),
						workDir);
	}

	private static Map<String, String> summaries(
			Unit[] units) {
		Map<String, String> summaries = new TreeMap<>();
		SpringFixtures.newP1Mappings(units).forEach((
				method,
				mapping) -> summaries.put(method,
						SpringFixtures.summary(mapping)));
		return summaries;
	}

	private static List<String> legacyMappings(
			Unit[] units) {
		return new P1Impl().produceRegistry(units).getMappings().stream()
				.map(mapping -> mapping.getAnnotation().getHttpMethod() + " "
						+ mapping.getAnnotation().getAddressPath())
				.sorted()
				.toList();
	}
}

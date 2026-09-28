package it.unive.jlisa.springed.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.springed.exceptions.UnresolvedTypeException;
import it.unive.jlisa.springed.p1.P1Impl;
import it.unive.jlisa.springed.p1.constructs.RequestMapping;
import it.unive.jlisa.springed.regression.SpringFixtures.Parsed;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.values.AnnotationValue;
import it.unive.lisa.program.annotations.values.StringAnnotationValue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests of the springed-specific parsing behaviour on small
 * synthetic Spring sources: the spring-libraries stubs, the collection (instead
 * of the propagation) of parsing errors, and the isolation of files whose
 * parsing fails. See {@link Snapshots} for how to run the suite.
 */
public class SpringFrontendRegressionTest {

	private static final List<String> STUBS = List.of(
			"org.springframework.http.MediaType",
			"org.springframework.util.MimeType",
			"org.springframework.web.bind.annotation.RequestMethod");

	@Test
	public void springLibraryConstantsAreFoldedIntoAnnotations(
			@TempDir Path root)
			throws IOException {
		Parsed parsed = SpringFixtures.parseSources(root, Map.of("demo/StubController.java", """
				package demo;

				import org.springframework.http.MediaType;
				import org.springframework.web.bind.annotation.RequestMapping;
				import org.springframework.web.bind.annotation.RequestMethod;
				import org.springframework.web.bind.annotation.RestController;

				@RestController
				@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
				public class StubController {

					@RequestMapping(path = "/all", method = { RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST,
							RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS,
							RequestMethod.TRACE }, consumes = { MediaType.TEXT_PLAIN_VALUE, MediaType.TEXT_HTML_VALUE })
					public String all() {
						return "all";
					}

					@RequestMapping(path = "/one", method = RequestMethod.PATCH, produces = MediaType.TEXT_HTML_VALUE)
					public String one() {
						return "one";
					}
				}
				"""));

		for (String stub : STUBS)
			assertTrue(LibrarySpecificationProvider.isLibraryAvailable(stub), () -> stub
					+ " is not available: is \"/spring-libraries\" still in LibrarySpecificationProvider.LIBS_FOLDER?");
		assertEquals(List.of(), parsed.frontend().getParseExceptions(), "unexpected parsing errors");

		Map<String, RequestMapping> mappings = SpringFixtures.newP1Mappings(parsed.units());
		RequestMapping all = mappings.get("StubController::all");
		assertNotNull(all, () -> "missing mapping of all(): " + mappings.keySet());
		assertEquals(Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE"), all.getMethods());
		assertEquals(Set.of("/api/all"), all.getPaths());
		assertEquals(Set.of("text/plain", "text/html"), all.getConsumes());
		assertEquals(Set.of("application/json"), all.getProduces());

		RequestMapping one = mappings.get("StubController::one");
		assertNotNull(one, () -> "missing mapping of one(): " + mappings.keySet());
		assertEquals(Set.of("PATCH"), one.getMethods());
		assertEquals(Set.of("text/html"), one.getProduces());

		// the constant is folded into its (quoted) literal value, instead of
		// being kept as the symbolic "MediaType.APPLICATION_JSON_VALUE"
		CompilationUnit controller = (CompilationUnit) parsed.frontend().getProgram().getUnit("demo.StubController");
		AnnotationValue produces = member(annotation(controller.getAnnotationList(), "RequestMapping"), "produces");
		assertInstanceOf(StringAnnotationValue.class, produces);
		assertEquals("\"application/json\"", produces.toString());
	}

	@Test
	public void unresolvedTypesAndStaticImportsAreCollectedNotThrown(
			@TempDir Path root)
			throws IOException {
		Parsed parsed = SpringFixtures.parseSources(root, Map.of("demo/DemoController.java", """
				package demo;

				import static java.util.Collections.emptyList;

				import org.springframework.http.ResponseEntity;
				import org.springframework.web.bind.annotation.GetMapping;
				import org.springframework.web.bind.annotation.RestController;

				@RestController
				public class DemoController {

					@GetMapping("/items")
					public ResponseEntity<String> items(UnknownFilter filter) {
						return null;
					}

					@GetMapping("/ping")
					public String ping() {
						return "pong";
					}
				}
				"""));
		List<Throwable> errors = parsed.frontend().getParseExceptions();

		assertTrue(errors.stream().anyMatch(e -> e instanceof ParsingException
				&& e.getMessage().contains("Static imports are not supported.")),
				() -> "the static import was not collected: " + errors);
		assertEquals(Set.of("ResponseEntity", "UnknownFilter"), errors.stream()
				.filter(UnresolvedTypeException.class::isInstance)
				.map(e -> ((UnresolvedTypeException) e).getUnresolvedName())
				.collect(Collectors.toSet()), () -> "unexpected unresolved types in " + errors);

		assertEquals(Map.of(
				"DemoController::items", "GET /items",
				"DemoController::ping", "GET /ping"),
				summaries(parsed));
		assertEquals(List.of("GET /items", "GET /ping"), legacyMappings(parsed));
	}

	@Test
	public void aFileThatFailsToParseDoesNotAffectTheOthers(
			@TempDir Path root)
			throws IOException {
		Parsed parsed = SpringFixtures.parseSources(root, Map.of(
				"demo/BrokenController.java", """
						package demo;

						import org.springframework.web.bind.annotation.GetMapping;
						import org.springframework.web.bind.annotation.RestController;

						@RestController
						public class BrokenController {

							@GetMapping("/broken")
							public org.unknown.Thing broken() {
								return null;
							}
						}
						""",
				"demo/HealthyController.java", """
						package demo;

						import org.springframework.web.bind.annotation.GetMapping;
						import org.springframework.web.bind.annotation.PostMapping;
						import org.springframework.web.bind.annotation.RestController;

						@RestController
						public class HealthyController {

							@GetMapping("/a")
							public String a() {
								return "a";
							}

							@PostMapping("/b")
							public String b() {
								return "b";
							}
						}
						"""));
		List<Throwable> errors = parsed.frontend().getParseExceptions();

		assertTrue(errors.stream().anyMatch(e -> String.valueOf(e.getMessage()).contains("org.unknown.Thing")),
				() -> "the failure of BrokenController was not collected: " + errors);
		Map<String, String> summaries = summaries(parsed);
		assertEquals("GET /a", summaries.get("HealthyController::a"), () -> "unexpected mappings: " + summaries);
		assertEquals("POST /b", summaries.get("HealthyController::b"), () -> "unexpected mappings: " + summaries);
	}

	@Test
	public void importingAnExceptionTypeKeepsAllTheEndpoints(
			@TempDir Path root)
			throws IOException {
		// importing a library exception (here, as the first parameter type)
		// also imports its known subtypes on master: a failure there would
		// silently drop the remaining methods of the file
		Parsed parsed = SpringFixtures.parseSources(root, Map.of("demo/IoController.java", """
				package demo;

				import java.io.IOException;
				import org.springframework.web.bind.annotation.GetMapping;
				import org.springframework.web.bind.annotation.PostMapping;
				import org.springframework.web.bind.annotation.RestController;

				@RestController
				public class IoController {

					@GetMapping("/io")
					public String io(IOException cause) {
						return "io";
					}

					@PostMapping("/after")
					public String after() {
						return "after";
					}

					@GetMapping("/last")
					public String last() {
						return "last";
					}
				}
				"""));

		assertEquals(List.of(), parsed.frontend().getParseExceptions(), "unexpected parsing errors");
		assertEquals(Map.of(
				"IoController::io", "GET /io",
				"IoController::after", "POST /after",
				"IoController::last", "GET /last"),
				summaries(parsed));
	}

	private static Map<String, String> summaries(
			Parsed parsed) {
		return SpringFixtures.newP1Mappings(parsed.units()).entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> SpringFixtures.summary(entry.getValue())));
	}

	private static List<String> legacyMappings(
			Parsed parsed) {
		return new P1Impl().produceRegistry(parsed.units()).getMappings().stream()
				.map(mapping -> mapping.getAnnotation().getHttpMethod() + " "
						+ mapping.getAnnotation().getAddressPath())
				.sorted()
				.toList();
	}

	private static Annotation annotation(
			Iterable<Annotation> annotations,
			String name) {
		for (Annotation annotation : annotations)
			if (annotation.getAnnotationName().equals(name))
				return annotation;
		throw new AssertionError("missing @" + name + " in " + annotations);
	}

	private static AnnotationValue member(
			Annotation annotation,
			String id) {
		for (AnnotationMember member : annotation.getAnnotationMembers())
			if (member.getId().equals(id))
				return member.getValue();
		throw new AssertionError("missing member " + id + " in " + SpringDump.annotation(annotation));
	}
}

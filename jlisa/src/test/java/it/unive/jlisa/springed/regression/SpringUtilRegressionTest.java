package it.unive.jlisa.springed.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.jlisa.springed.Main;
import it.unive.jlisa.springed.SpringTestCases;
import it.unive.jlisa.springed.p1.NewP1Impl;
import it.unive.jlisa.springed.p1.constructs.RequestMapping;
import it.unive.jlisa.springed.p1.util.P1Util;
import it.unive.jlisa.springed.p1.util.RequestMappingBuilder;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.values.AnnotationValue;
import it.unive.lisa.program.annotations.values.ArrayAnnotationValue;
import it.unive.lisa.program.annotations.values.BasicAnnotationValue;
import it.unive.lisa.program.annotations.values.StringAnnotationValue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Regression tests of the springed building blocks that do not need a parsed
 * program: the detection of Spring projects, the P1 helpers working on LiSA
 * annotations (whose {@code toString()} they partly rely on), and the path
 * patterns that {@link NewP1Impl} already supports. See {@link Snapshots} for
 * how to run the suite.
 */
public class SpringUtilRegressionTest {

	@Test
	public void detectsSpringProjectsBelowTheRoot(
			@TempDir Path root)
			throws IOException {
		project(root.resolve("svc-maven"), "pom.xml");
		project(root.resolve("svc-gradle"), "build.gradle");
		project(root.resolve("svc-kts"), "build.gradle.kts");
		project(root.resolve("group").resolve("nested"), "pom.xml");
		project(root.resolve("outer"), "pom.xml");
		// a project inside a project is not reported
		project(root.resolve("outer").resolve("inner"), "pom.xml");
		// a build file without sources, and sources without a build file
		Files.createDirectories(root.resolve("no-sources"));
		Files.createFile(root.resolve("no-sources").resolve("build.gradle"));
		Files.createDirectories(root.resolve("sources-only").resolve("src/main/java"));
		// skipped directories
		project(root.resolve("build").resolve("generated"), "pom.xml");
		project(root.resolve("node_modules").resolve("pkg"), "pom.xml");
		project(root.resolve(".hidden").resolve("svc"), "pom.xml");

		assertEquals(List.of("group/nested", "outer", "svc-gradle", "svc-kts", "svc-maven"),
				relative(root, Main.detectSpringProjects(root)));
	}

	@Test
	public void theRootItselfCanBeASpringProject(
			@TempDir Path root)
			throws IOException {
		project(root, "build.gradle");

		assertEquals(List.of(root), Main.detectSpringProjects(root));
	}

	@Test
	public void detectsTheSampleProjects(
			@TempDir Path root)
			throws IOException {
		for (String caseName : SpringFixtures.CASES)
			SpringTestCases.extract(caseName, root);

		assertEquals(SpringFixtures.CASES, relative(root, Main.detectSpringProjects(root)));
	}

	@Test
	public void requestMappingBuilderReadsLisaAnnotations() {
		RequestMapping full = RequestMappingBuilder.build(annotation("RequestMapping",
				new AnnotationMember("path", strings("/a", "/b")),
				new AnnotationMember("method", strings("GET", "POST")),
				new AnnotationMember("params", string("q")),
				new AnnotationMember("headers", strings("X-A", "X-B=1")),
				new AnnotationMember("consumes", string("application/json")),
				new AnnotationMember("produces", strings("text/plain")),
				new AnnotationMember("version", string("1.0+"))));
		assertEquals(Set.of("GET", "POST"), full.getMethods());
		assertEquals(Set.of("/a", "/b"), full.getPaths());
		assertEquals(Set.of("q"), full.getParams());
		assertEquals(Set.of("X-A", "X-B=1"), full.getHeaders());
		assertEquals(Set.of("application/json"), full.getConsumes());
		assertEquals(Set.of("text/plain"), full.getProduces());
		assertEquals(Set.of("1.0+"), full.getVersion());

		RequestMapping get = RequestMappingBuilder.build(annotation("GetMapping",
				new AnnotationMember("value", string("/x"))));
		assertEquals(Set.of("GET"), get.getMethods());
		assertEquals(Set.of("/x"), get.getPaths());

		RequestMapping bare = RequestMappingBuilder.build(new Annotation("RequestMapping"));
		assertEquals(Set.of(), bare.getMethods());
		assertEquals(Set.of(), bare.getPaths());
		assertEquals(Set.of(), bare.getConsumes());
	}

	@Test
	public void p1UtilReadsLisaAnnotations() {
		assertEquals("/x", P1Util.getPath(annotation("GetMapping", new AnnotationMember("value", string("/x")))));
		assertEquals("/a", P1Util.getPath(annotation("DeleteMapping",
				new AnnotationMember("path", strings("/a", "/b")))));
		assertNull(P1Util.getPath(new Annotation("GetMapping")));

		assertEquals("GET", P1Util.getHttpMethod(new Annotation("GetMapping")));
		assertEquals("POST", P1Util.getHttpMethod(new Annotation("PostMapping")));
		assertEquals("PUT", P1Util.getHttpMethod(new Annotation("PutMapping")));
		assertEquals("DELETE", P1Util.getHttpMethod(new Annotation("DeleteMapping")));
		assertEquals("PATCH", P1Util.getHttpMethod(new Annotation("PatchMapping")));
		assertNull(P1Util.getHttpMethod(new Annotation("RestController")));
		assertThrows(UnsupportedOperationException.class,
				() -> P1Util.getHttpMethod(new Annotation("RequestMapping")));
	}

	/**
	 * The rows of {@code p1_1/combining-path-patterns-table.md} that are marked
	 * as supported by P1.
	 */
	private static Stream<Arguments> supportedPathPatterns() {
		return Stream.of(
				Arguments.of(1, "", "", ""),
				Arguments.of(2, "", "/a", "/a"),
				Arguments.of(3, "/a", "", "/a"),
				Arguments.of(4, "/a", null, "throws NullPointerException"),
				Arguments.of(5, "/hotels", "/booking", "/hotels/booking"),
				Arguments.of(6, "/projects", "/spring-framework", "/projects/spring-framework"),
				Arguments.of(7, "/usr", "/user", "/usr/user"),
				Arguments.of(8, "/hotels", "/hotels", "/hotels/hotels"),
				Arguments.of(9, "/a.html", "/a.html", "/a.html/a.html"),
				Arguments.of(10, "/hotels", "booking", "/hotels/booking"),
				Arguments.of(12, "/a", "/", "/a/"),
				Arguments.of(14, "/projects", "/{project}", "/projects/{project}"),
				Arguments.of(21, "/*", "/hotel", "/hotel"),
				Arguments.of(25, "/**", "/booking", "/booking"),
				Arguments.of(26, "/hotels/*", "/booking", "/hotels/booking"),
				Arguments.of(27, "/projects/*", "/spring-framework", "/projects/spring-framework"),
				Arguments.of(28, "/hotels/*", "/booking/rooms", "/hotels/booking/rooms"),
				Arguments.of(29, "/hotels/*", "booking", "/hotels/booking"),
				Arguments.of(31, "/{foo}/*", "/bar", "/{foo}/bar"),
				Arguments.of(32, "/*", "/a/b", "/a/b"),
				Arguments.of(34, "/hotels/**", "/booking", "/hotels/booking"),
				Arguments.of(35, "/hotels/**", "/booking/rooms", "/hotels/booking/rooms"));
	}

	@ParameterizedTest(name = "#{0} \"{1}\" + \"{2}\" = [{3}]")
	@MethodSource("supportedPathPatterns")
	public void combinesSupportedPathPatterns(
			int row,
			String typeLevel,
			String methodLevel,
			String expected)
			throws ReflectiveOperationException {
		Method mergePathPatterns = NewP1Impl.class.getDeclaredMethod("mergePathPatterns", String.class,
				String.class);
		mergePathPatterns.setAccessible(true);

		String actual;
		try {
			actual = (String) mergePathPatterns.invoke(new NewP1Impl(), typeLevel, methodLevel);
		} catch (InvocationTargetException e) {
			actual = "throws " + e.getCause().getClass().getSimpleName();
		}
		assertEquals(expected, actual, () -> "row " + row + ": combined pattern");
	}

	private static void project(
			Path dir,
			String buildFile)
			throws IOException {
		Files.createDirectories(dir.resolve("src/main/java"));
		Files.createFile(dir.resolve(buildFile));
	}

	private static List<String> relative(
			Path root,
			List<Path> projects) {
		return projects.stream()
				.map(project -> root.relativize(project).toString().replace('\\', '/'))
				.sorted()
				.toList();
	}

	private static Annotation annotation(
			String name,
			AnnotationMember... members) {
		return new Annotation(name, List.of(members));
	}

	/** A string value as the frontend stores it: quoted. */
	private static StringAnnotationValue string(
			String value) {
		return new StringAnnotationValue("\"" + value + "\"");
	}

	private static AnnotationValue strings(
			String... values) {
		return new ArrayAnnotationValue(Arrays.stream(values)
				.map(SpringUtilRegressionTest::string)
				.toArray(BasicAnnotationValue[]::new));
	}
}

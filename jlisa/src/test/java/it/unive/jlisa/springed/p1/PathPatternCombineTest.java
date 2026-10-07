package it.unive.jlisa.springed.p1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.jlisa.springed.exceptions.PathMergeException;
import it.unive.jlisa.springed.frontend.SpringFrontend;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PathPatternCombineTest {

	private static final Path TABLE = Paths
			.get("src/test/java/it/unive/jlisa/springed/p1/combining-path-patterns-table.md");

	private static final Map<Integer, String> TABLE_ROWS = new TreeMap<>();

	private static final P1 P1_IMPL = new P1Impl(new SpringFrontend().getParserContext());

	private static Stream<Arguments> instances() {
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
				Arguments.of(11, "/", "/a", "/a"),
				Arguments.of(12, "/a", "/", "/a/"),
				Arguments.of(13, "/", "/", "/"),
				Arguments.of(14, "/projects", "/{project}", "/projects/{project}"),
				Arguments.of(15, "/{foo}", "/bar", "/{foo}/bar"),
				Arguments.of(16, "/a/{x}", "/b", "/a/{x}/b"),
				Arguments.of(17, "/hotels/?", "/booking", "/hotels/?/booking"),
				Arguments.of(18, "/projects/*/releases", "/{id}", "/projects/*/releases/{id}"),
				Arguments.of(19, "/{x}/*.html", "/y.pdf", "/{x}/*.html/y.pdf"),
				Arguments.of(20, "/{id}", "/{id}", "throws PatternParseException"),
				Arguments.of(21, "/*", "/hotel", "/hotel"),
				Arguments.of(22, "/*", "/{project}", "/{project}"),
				Arguments.of(23, "/*", "/**", "/**"),
				Arguments.of(24, "/a/*/c", "/a/b/c", "/a/b/c"),
				Arguments.of(25, "/**", "/booking", "/booking"),
				Arguments.of(26, "/hotels/*", "/booking", "/hotels/booking"),
				Arguments.of(27, "/projects/*", "/spring-framework", "/projects/spring-framework"),
				Arguments.of(28, "/hotels/*", "/booking/rooms", "/hotels/booking/rooms"),
				Arguments.of(29, "/hotels/*", "booking", "/hotels/booking"),
				Arguments.of(30, "/projects/*", "/{project}", "/projects/{project}"),
				Arguments.of(31, "/{foo}/*", "/bar", "/{foo}/bar"),
				Arguments.of(32, "/*", "/a/b", "/a/b"),
				Arguments.of(33, "/*", "/*", "/*"),
				Arguments.of(34, "/hotels/**", "/booking", "/hotels/booking"),
				Arguments.of(35, "/hotels/**", "/booking/rooms", "/hotels/booking/rooms"),
				Arguments.of(36, "/projects/**", "/*.html", "/projects/*.html"),
				Arguments.of(37, "/a/**", "/**", "/a/**"),
				Arguments.of(38, "/hotels/**", "/hotels/**", "/hotels/hotels/**"),
				Arguments.of(39, "/**", "/**", "throws StringIndexOutOfBoundsException"),
				Arguments.of(40, "/{*path}", "/booking", "throws PatternParseException"),
				Arguments.of(41, "/hotels/{*path}", "/booking", "throws PatternParseException"),
				Arguments.of(42, "a.b", "c", "a.b/c"),
				Arguments.of(43, "a.*", "b", "a.*/b"),
				Arguments.of(44, "a.**", "b.c", "a.**/b.c"),
				Arguments.of(45, "*.html", "hotel", "throws StringIndexOutOfBoundsException"),
				Arguments.of(46, "/*.html", "/hotel.html", "/hotel.html"),
				Arguments.of(47, "/*.html", "/{name}.html", "/{name}.html"),
				Arguments.of(48, "/*.*", "/hotel.pdf", "/hotel.pdf"),
				Arguments.of(49, "/*.*", "/*.html", "/*.html"),
				Arguments.of(50, "/*.*", "/{name:.+}", "/{name:.+}"),
				Arguments.of(51, "/*.html", "/hotel", "/hotel.html"),
				Arguments.of(52, "/*.html", "/{name}", "/{name}.html"),
				Arguments.of(53, "/*.html", "/hotel.*", "/hotel.html"),
				Arguments.of(54, "/*.html", "/x{y}.*", "/x{y}.html"),
				Arguments.of(55, "/hotels/*.html", "/hotels/x", "/hotels/x.html"),
				Arguments.of(56, "/projects/*.html", "/projects/spring.*", "/projects/spring.html"),
				Arguments.of(57, "/*/*.html", "/{x}/y.*", "/{x}/y.html"),
				Arguments.of(58, "/dir/*.*", "/dir/hotel", "/dir/hotel"),
				Arguments.of(59, "/*.html", "/*.*", "/*.html"),
				Arguments.of(60, "/*.*", "/*.*", "/*.*"),
				Arguments.of(61, "/*.html", "/**", "/**.html"),
				Arguments.of(62, "/*.html", "/hotel.pdf", "throws IllegalArgumentException"),
				Arguments.of(63, "/*.html", "/*.pdf", "throws IllegalArgumentException"),
				Arguments.of(64, "/*.html", "/*.html", "throws IllegalArgumentException"),
				Arguments.of(65, "/*.html", "/hotel.html.gz", "throws IllegalArgumentException"),
				Arguments.of(66, "/*.html", "/v1.2/hotel", "throws IllegalArgumentException"),
				Arguments.of(67, "/*.html", "/{name:.*}", "throws IllegalArgumentException"),
				Arguments.of(68, "/dir/*.html", "/hotel", "throws IllegalArgumentException"),
				Arguments.of(69, "/projects/*.html", "/spring-framework.html", "throws IllegalArgumentException"),
				Arguments.of(70, "/projects/*.html", "/spring-framework.*", "throws IllegalArgumentException"),
				Arguments.of(71, "/dir/*.*", "/other/hotel.pdf", "throws IllegalArgumentException"),
				Arguments.of(72, "/a/*.html", "/{x}/y.*", "throws IllegalArgumentException"),
				Arguments.of(73, "/*.html", "/a/b", "throws IllegalArgumentException"),
				Arguments.of(74, "/*.html/x", "/bar", "throws IllegalArgumentException"),
				Arguments.of(75, "/projects/*.html/releases", "/{id}", "throws IllegalArgumentException"),
				Arguments.of(76, "/*.html", "hotel", "throws StringIndexOutOfBoundsException"),
				Arguments.of(77, "*.html", "/hotel", "throws StringIndexOutOfBoundsException"));
	}

	@ParameterizedTest(name = "#{0} \"{1}\" + \"{2}\" = [{3}]")
	@MethodSource("instances")
	public void combinesTypeLevelWithMethodLevel(
			int row,
			String typeLevel,
			String methodLevel,
			String expected) {
		String actual = combine(typeLevel, methodLevel);
		boolean supported = actual.equals(expected)
				|| expected.startsWith("throws ")
						&& actual.equals("throws " + PathMergeException.class.getSimpleName());
		TABLE_ROWS.put(row, "| " + row + " | " + code(typeLevel) + " | " + code(methodLevel) + " | " + result(expected)
				+ " | " + result(actual) + " | " + (supported ? "✅" : "❌") + " |");
		if (supported)
			return;
		assertEquals(expected, actual, () -> "row " + row + ": combined pattern");
	}

	@AfterAll
	static void writeTable()
			throws IOException {
		if (TABLE_ROWS.size() != instances().count())
			return;
		long supported = TABLE_ROWS.values().stream().filter(r -> r.endsWith("✅ |")).count();
		List<String> lines = new ArrayList<>(List.of(
				"# `Combining path patterns`",
				"",
				"Supported by P1: ✅ " + supported + " of " + TABLE_ROWS.size() + " ("
						+ Math.round(100.0 * supported / TABLE_ROWS.size()) + "%)",
				"",
				"| # | `this` | `other` | Result | P1 result | Supported by P1 |",
				"|---:|---|---|---|---|:---:|"));
		lines.addAll(TABLE_ROWS.values());
		Files.write(TABLE, (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
	}

	private static String code(
			String pattern) {
		return pattern == null ? "`null`" : pattern.isEmpty() ? "`\"\"`" : "`" + pattern + "`";
	}

	private static String result(
			String result) {
		return result.startsWith("throws ") ? result : code(result);
	}

	private static String combine(
			String typeLevel,
			String methodLevel) {
		try {
			Set<String> merged = P1_IMPL.mergeMappings(mappingWithPath(typeLevel), mappingWithPath(methodLevel)).getPaths();
			return merged.size() == 1 ? merged.iterator().next() : new TreeSet<>(merged).toString();
		} catch (RuntimeException e) {
			return "throws " + e.getClass().getSimpleName();
		}
	}

	private static RequestMapping mappingWithPath(
			String path) {
		return new RequestMapping(Set.of(), new HashSet<>(Collections.singleton(path)), Set.of(), Set.of(), Set.of(),
				Set.of(), "");
	}
}

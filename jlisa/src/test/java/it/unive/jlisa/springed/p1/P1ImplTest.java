package it.unive.jlisa.springed.p1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.jlisa.springed.SpringTestHelper;
import it.unive.jlisa.springed.frontend.SpringFrontend;
import it.unive.lisa.program.Unit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class P1ImplTest {

	@TempDir
	Path cases;

	private static Stream<Arguments> controllers() {
		return Stream.of(
				Arguments.of("case-1", Set.of(
						"it.unive.jlisa.jlisa.testcases.case_1.controllers.Controller")),
				Arguments.of("case-2", Set.of(
						"it.unive.jlisa.jlisa.testcases.case_2.controllers.UserController")),
				Arguments.of("case-3", Set.of(
						"it.unive.jlisa.jlisa.testcases.case_3.controllers.ClientLayer",
						"it.unive.jlisa.jlisa.testcases.case_3.controllers.PublicLayer")),
				Arguments.of("case-4", Set.of(
						"it.unive.jlisa.jlisa.testcases.case_4.controllers.Controller")),
				Arguments.of("case-5", Set.of(
						"it.unive.jlisa.jlisa.testcases.case_5.controllers.ClassMappingWithoutPath",
						"it.unive.jlisa.jlisa.testcases.case_5.controllers.NoClassMapping")));
	}

	private static Stream<Arguments> registries() {
		return Stream.of(
				Arguments.of("case-1", List.of(
						"Controller_endpoint1 {GET [/hello-world]}")),
				Arguments.of("case-2", List.of(
						"UserController_createUser {POST [/users]}",
						"UserController_deleteUser {DELETE [/users/{id}]}",
						"UserController_listUsers {GET [/users]}",
						"UserController_updateUser {PUT [/users/{id}]}")),
				Arguments.of("case-3", List.of(
						"ClientLayer_patchProfile {PATCH [/client/profile]}",
						"ClientLayer_status {GET [/client/status]}",
						"PublicLayer_info {GET [/public/info]}",
						"PublicLayer_submitFeedback {POST [/public/feedback]}")),
				Arguments.of("case-4", List.of(
						"Controller_endpoint1 {GET [/api/hello-world || /api/legacy/hello-world], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint10 {POST [/api/legacy/secure || /api/secure], params [!disabled], headers [!X-Blocked && X-Api-Key && X-Client=web], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint11 {GET [/api/filter || /api/filter/all || /api/legacy/filter || /api/legacy/filter/all], params [!disabled && type], headers [!X-Blocked && Accept-Language=it], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint12 { [/api/everything || /api/legacy/everything], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint13 {GET [/api/items/{id} || /api/legacy/items/{id}], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint14 {GET [/api/files/** || /api/legacy/files/** || /api/legacy/users/{name:[a-z]+} || /api/users/{name:[a-z]+}], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint15 {POST [/api/legacy/upload || /api/upload], params [!disabled], headers [!X-Blocked && !X-Debug], consumes [!text/plain || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint16 {GET [/api || /api/legacy], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint17 {OPTIONS [/api/legacy/options || /api/options], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint18 {HEAD [/api/head || /api/legacy/head], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint19 {TRACE [/api/legacy/trace || /api/trace], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint2 {POST [/api/greet || /api/legacy/greet], params [!disabled], headers [!X-Blocked], consumes [application/json], produces [text/plain], version [1.0+]}",
						"Controller_endpoint21 { [/api || /api/legacy], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint3 {PUT [/api/legacy/update || /api/update], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint4 {DELETE [/api/delete || /api/legacy/delete || /api/legacy/remove || /api/remove], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint5 {PATCH [/api/legacy/patch || /api/patch], params [!disabled], headers [!X-Blocked], consumes [application/json], produces [application/json], version [1.0+]}",
						"Controller_endpoint6 {[GET, POST] [/api/all || /api/any || /api/legacy/all || /api/legacy/any], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}",
						"Controller_endpoint7 {GET [/api/legacy/versioned || /api/versioned], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0]}",
						"Controller_endpoint8 {GET [/api/legacy/versioned || /api/versioned], params [!disabled], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [2.0+]}",
						"Controller_endpoint9 {GET [/api/legacy/search || /api/search], params [!debug && !disabled && lang=en && q], headers [!X-Blocked], consumes [application/json || text/*], produces [text/plain], version [1.0+]}")),
				Arguments.of("case-5", List.of(
						"ClassMappingWithoutPath_root {GET [ || /], produces [text/plain]}",
						"NoClassMapping_emptyPath {POST [ || /]}",
						"NoClassMapping_root {GET [ || /]}")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("controllers")
	public void getsControllers(
			String name,
			Set<String> expected)
			throws IOException {
		SpringFrontend frontend = new SpringFrontend();
		Unit[] units = frontend.parse(SpringTestHelper.sourceRoot(name, cases).toString());

		Set<String> controllers = new P1Impl(frontend.getParserContext()).getControllers(units).stream()
				.map(Unit::getName)
				.collect(Collectors.toSet());

		assertEquals(expected, controllers);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("registries")
	public void buildsRegistry(
			String name,
			List<String> expected)
			throws IOException {
		SpringFrontend frontend = new SpringFrontend();
		Unit[] units = frontend.parse(SpringTestHelper.sourceRoot(name, cases).toString());

		Registry registry = new P1Impl(frontend.getParserContext()).p1(units);

		assertEquals(lines(expected.stream()), lines(registry.getMappings().stream()
				.map(record -> record.getJsonFieldName() + " " + render(record.getAnnotation()))));
		assertEquals(List.of(), frontend.getParseExceptions(), "parse exceptions");
	}

	private static String lines(
			Stream<String> lines) {
		return lines.sorted().collect(Collectors.joining("\n"));
	}

	private static String render(
			RequestMapping mapping) {
		StringBuilder builder = new StringBuilder("{");
		Set<String> methods = mapping.getMethods();
		if (!methods.isEmpty())
			builder.append(methods.size() == 1 ? methods.iterator().next() : sorted(methods, ", "));
		builder.append(' ').append(sorted(mapping.getPaths(), " || "));
		append(builder, "params", mapping.getParams(), " && ");
		append(builder, "headers", mapping.getHeaders(), " && ");
		append(builder, "consumes", mapping.getConsumes(), " || ");
		append(builder, "produces", mapping.getProduces(), " || ");
		if (!mapping.getVersion().isEmpty())
			builder.append(", version [").append(mapping.getVersion()).append(']');
		return builder.append('}').toString();
	}

	private static void append(
			StringBuilder builder,
			String attribute,
			Set<String> values,
			String separator) {
		if (!values.isEmpty())
			builder.append(", ").append(attribute).append(' ').append(sorted(values, separator));
	}

	private static String sorted(
			Set<String> values,
			String separator) {
		return values.stream().sorted().collect(Collectors.joining(separator, "[", "]"));
	}
}

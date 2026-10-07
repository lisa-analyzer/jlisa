package it.unive.jlisa.springed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.jlisa.springed.frontend.SpringFrontend;
import it.unive.lisa.program.Unit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SpringFrontendTest {

	@TempDir
	Path cases;

	private static Stream<Arguments> units() {
		return Stream.of(
				Arguments.of("case-1", Set.of(
						"it.unive.jlisa.jlisa.testcases.case_1.Case1Application",
						"it.unive.jlisa.jlisa.testcases.case_1.controllers.Controller")),
				Arguments.of("case-2", Set.of(
						"it.unive.jlisa.jlisa.testcases.case_2.Case2Application",
						"it.unive.jlisa.jlisa.testcases.case_2.controllers.UserController")),
				Arguments.of("case-3", Set.of(
						"it.unive.jlisa.jlisa.testcases.case_3.Case3Application",
						"it.unive.jlisa.jlisa.testcases.case_3.controllers.ClientLayer",
						"it.unive.jlisa.jlisa.testcases.case_3.controllers.PublicLayer")),
				Arguments.of("case-4", Set.of(
						"it.unive.jlisa.jlisa.testcases.case_4.Case4Application",
						"it.unive.jlisa.jlisa.testcases.case_4.controllers.Controller")),
				Arguments.of("case-5", Set.of(
						"it.unive.jlisa.jlisa.testcases.case_5.Case5Application",
						"it.unive.jlisa.jlisa.testcases.case_5.controllers.ClassMappingWithoutPath",
						"it.unive.jlisa.jlisa.testcases.case_5.controllers.NoClassMapping")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("units")
	public void extractsClasses(
			String name,
			Set<String> expected)
			throws IOException {
		SpringFrontend frontend = new SpringFrontend();
		Unit[] classes = frontend.parse(SpringTestHelper.sourceRoot(name, cases).toString());

		Set<String> classNames = Arrays.stream(classes)
				.map(Unit::getName)
				.collect(Collectors.toSet());

		assertEquals(expected, classNames);
		assertEquals(List.of(), frontend.getParseExceptions(), "parse exceptions");
	}
}

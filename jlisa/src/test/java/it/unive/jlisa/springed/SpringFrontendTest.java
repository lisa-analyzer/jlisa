package it.unive.jlisa.springed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.jlisa.springed.frontend.SpringFrontend;
import it.unive.jlisa.springed.p1.P1Impl;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SpringFrontendTest {

	@BeforeAll
	public static void unpackCase() throws IOException {
		SpringTestCases.extract("case-1");
	}

	@AfterAll
	public static void cleanUpCase() throws IOException {
		SpringTestCases.delete(SpringTestCases.ROOT.resolve("case-1"));
	}

	@Test
	public void case1ClassExtraction() throws IOException {
		SpringFrontend frontend = new SpringFrontend();
		Unit[] classes = frontend.parse(List.of("spring-testcases/case-1/src/main/java"));

		Set<String> classNames = Arrays.stream(classes)
				.map(Unit::getName)
				.collect(Collectors.toSet());

		int classCount = classNames.size();
		String main = "it.unive.jlisa.jlisa.testcases.case_1.Case1Application";
		String controller = "it.unive.jlisa.jlisa.testcases.case_1.controllers.Controller";

		assertEquals(2, classCount, () -> "unexpected units: " + classNames);
		assertTrue(classNames.contains(main), () -> "missing Case1Application, got: " + classNames);
		assertTrue(classNames.contains(controller), () -> "missing Controller, got: " + classNames);
	}

	@Test
	public void case1ControllerExistenceCheck() throws IOException {
		SpringFrontend frontend = new SpringFrontend();
		Unit[] p = frontend.parse(List.of("spring-testcases/case-1/src/main/java"));

		P1Impl p1 = new P1Impl();
		List<ClassUnit> controllers = p1.getControllerClasses(p);

		Set<String> controllerNames = controllers.stream()
				.map(Unit::getName)
				.collect(Collectors.toSet());

		String controller = "it.unive.jlisa.jlisa.testcases.case_1.controllers.Controller";

		assertEquals(1, controllers.size(), () -> "unexpected controllers: " + controllerNames);
		assertTrue(controllerNames.contains(controller), () -> "missing Controller, got: " + controllerNames);
	}
}

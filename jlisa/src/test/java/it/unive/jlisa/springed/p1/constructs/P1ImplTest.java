package it.unive.jlisa.springed.p1.constructs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.jlisa.springed.SpringTestCases;
import it.unive.jlisa.springed.frontend.SpringFrontend;
import it.unive.jlisa.springed.p1.NewP1Impl;
import it.unive.jlisa.springed.p1.P1Impl;
import it.unive.lisa.program.Unit;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class P1ImplTest {

	@BeforeAll
	public static void unpackCase() throws IOException {
		SpringTestCases.extract("case-4");
	}

	@AfterAll
	public static void cleanUpCase() throws IOException {
		SpringTestCases.delete(SpringTestCases.ROOT.resolve("case-4"));
	}

	@Test
	public void case4RegistryOutputCheck() throws IOException {
		SpringFrontend frontend = new SpringFrontend();
		Unit[] p = frontend.parse("spring-testcases/case-4");

		List<RequestMapping> p1 = new NewP1Impl().p1(p);

//		assertEquals("endpoint1", mapping.getMethod().getDescriptor().getName());
//		assertEquals("GET", annotation.getHttpMethod());
//		assertEquals("/hello-world", annotation.getAddressPath());
	}
}

package it.unive.jlisa;

import org.junit.jupiter.api.Test;

public class MicroSmokeTest {

	// Smoke test for the micro example (com.example.micro)
	@Test
	public void testMicroDemo() throws Exception {
		String[] args = new String[] {
				"-s", "src/test/resources/com/example/micro",
				"-o", "tests-output/micro",
				"-n", "ConstantPropagation",
				"-l", "INFO"
		};

		// Run JLiSA main entry point
		Main.main(args);
	}
}

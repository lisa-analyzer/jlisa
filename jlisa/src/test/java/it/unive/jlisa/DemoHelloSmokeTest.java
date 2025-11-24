package it.unive.jlisa;

import org.junit.jupiter.api.Test;

public class DemoHelloSmokeTest {

	@Test
	public void testHelloDemo() throws Exception {
		String[] args = new String[] {
				"-s", "src/test/resources/demo/com/example/demo",
				"-o", "tests-output/demo-hello",
				"-n", "ConstantPropagation",
				"-l", "INFO"
		};

		Main.main(args);
	}
}

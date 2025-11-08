package it.unive.jlisa;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class DemoHelloSmokeTest {

	@Test
	public void runOnDemoApplication() throws IOException {

		CronConfiguration conf = TestHelpers.constantPropagation(
				"src/test/resources", // testDir
				"demo/com/example/demo", // subDir
				"DemoApplication.java" // program file
		);
		conf.workdir = "tests-output/demo-hello";
		conf.serializeResults = true;

		JLiSAAnalysisExecutor().perform(conf);
	}
}

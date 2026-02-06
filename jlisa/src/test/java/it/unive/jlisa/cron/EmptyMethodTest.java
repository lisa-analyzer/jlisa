package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.outputs.JSONInputs;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class EmptyMethodTest extends JLiSAAnalysisExecutor {

	private static CronConfiguration createConfiguration(
			String testDir,
			String subDir,
			String... programFiles) {
		CronConfiguration configuration = TestHelpers.createConfiguration(testDir, subDir, programFiles);
		configuration.outputs.add(new JSONInputs());
		configuration.analysis = null;
		return configuration;
	}

	@Test
	public void testEmptyMethod() throws IOException {
		CronConfiguration conf = createConfiguration("empty-method", "", "empty-method.java");
		perform(conf);
	}
}

package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.outputs.JSONInputs;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class VariableScopingTest extends JLiSAAnalysisExecutor {

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
	public void testVarScoping1() throws IOException {
		CronConfiguration conf = createConfiguration("var-scoping", "vs1", "vs1.java");
		perform(conf);
	}

	@Test
	public void testVarScoping2() throws IOException {
		CronConfiguration conf = createConfiguration("var-scoping", "vs2", "vs2.java");
		perform(conf);
	}

	@Test
	public void testVarScoping3() throws IOException {
		CronConfiguration conf = createConfiguration("var-scoping", "vs3", "vs3.java");
		perform(conf);
	}
}

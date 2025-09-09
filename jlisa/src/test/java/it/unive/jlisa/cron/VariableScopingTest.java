package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;

public class VariableScopingTest extends JLiSAAnalysisExecutor {

	private static CronConfiguration createConfiguration(String testDir, String subDir, String... programFiles) {
		CronConfiguration configuration = TestHelpers.createConfiguration(testDir, subDir, programFiles);

		configuration.serializeInputs = true;
		configuration.serializeResults = false;
		configuration.jsonOutput = true;
		configuration.analysis = null;
		configuration.analysisGraphs = GraphType.NONE;
		return configuration;
	}

    
	@Test
	public void testVarScoping1()  throws IOException {
		CronConfiguration conf = createConfiguration("var-scoping", "vs1", "vs1.java");
		perform(conf);
	}
	
	@Test
	public void testVarScoping2()  throws IOException {
		CronConfiguration conf = createConfiguration("var-scoping", "vs2", "vs2.java");
		perform(conf);
	}
	
	@Test
	public void testVarScoping3()  throws IOException {
		CronConfiguration conf = createConfiguration("var-scoping", "vs3", "vs3.java");
		perform(conf);
	}
}

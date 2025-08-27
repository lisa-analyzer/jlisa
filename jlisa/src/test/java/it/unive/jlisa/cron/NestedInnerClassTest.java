package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;

public class NestedInnerClassTest extends JLiSAAnalysisExecutor {

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
	public void testNestedInnerClass1()  throws IOException {
		CronConfiguration conf = createConfiguration("nested-inner-class", "nest-1", "nest-1.java");
		perform(conf);
	}
	
}

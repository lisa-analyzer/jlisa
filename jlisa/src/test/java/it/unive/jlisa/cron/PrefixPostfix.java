package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class PrefixPostfix extends JLiSAAnalysisExecutor {
	
	@Test
	public void testPrefixPostfix()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("prefix-postfix", "", "Main.java");
		perform(conf);
	}
}

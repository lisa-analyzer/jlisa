package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class TypingTest extends JLiSAAnalysisExecutor {

	@Test
	public void testTypingConversion() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("typing", "", "A.java");
		perform(conf);
	}
}

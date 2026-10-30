package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class TarsisTest extends JLiSAAnalysisExecutor {

	@Test
	public void testTarsis() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithTarsis("tarsis", "", "TarsisTest.java");
		perform(conf);
	}

}

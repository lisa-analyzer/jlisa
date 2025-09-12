package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class LongTest extends JLiSAAnalysisExecutor {

	@Test
	public void testLong() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("long", "", "LongTest.java");
		perform(conf);
	}

}
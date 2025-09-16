package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class SynchronizedTest extends JLiSAAnalysisExecutor {

	@Test
	public void testSynchronizedStatement1() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("synchronized", "synchronized-stmt-1", "Main.java");
		perform(conf);
	}
}

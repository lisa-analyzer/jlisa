package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class AtomicIntegerTest extends JLiSAAnalysisExecutor {

	@Test
	public void testAtomicInteger() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("atomic-integer", "", "Main.java");
		perform(conf);
	}
}

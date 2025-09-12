package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class WriterTest extends JLiSAAnalysisExecutor {

	@Test
	public void writerTest() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("writer", "", "Main.java");
		perform(conf);
	}
}

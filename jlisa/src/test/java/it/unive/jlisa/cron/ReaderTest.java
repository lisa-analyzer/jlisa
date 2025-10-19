package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ReaderTest extends JLiSAAnalysisExecutor {

	@Test
	public void readerTest() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("reader", "", "Main.java");
		conf.forceUpdate = true;
		perform(conf);
	}
}

package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class InstanceOfTest extends JLiSAAnalysisExecutor {

	@Test
	public void instanceofTest() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("instanceof", "", "Main.java");
		perform(conf);
	}
}

package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class StaticCallAutoboxingTest extends JLiSAAnalysisExecutor {

	@Test
	public void staticCallAutoboxingTest() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("static-call-autobox", "", "Main.java", "A.java");
		perform(conf);
	}
}

package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class WrapperClassTest extends JLiSAAnalysisExecutor {

	@Test
	public void wrapperClassTest() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("wrapper", "", "Main.java");
		perform(conf);
	}
}

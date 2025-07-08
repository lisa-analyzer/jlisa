package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class WrapperClassTest extends JLiSAAnalysisExecutor {
	
	@Test
	public void characterTest()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("wrapper", "", "Main.java");
		perform(conf);
	}
}

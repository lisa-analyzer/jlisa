package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class ReaderTest extends JLiSAAnalysisExecutor  {
	
	@Test
	public void readerTest()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("reader", "", "Main.java");
		perform(conf);
	}
}

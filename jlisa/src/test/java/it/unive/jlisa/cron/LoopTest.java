package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class LoopTest extends JLiSAAnalysisExecutor {
    
	@Test
	public void testWhile1()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("loops", "while", "while-1.java");
		perform(conf);
	}
	
	@Test
	public void testDoWhile1()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("loops", "do-while", "do-while-1.java");
		perform(conf);
	}
}

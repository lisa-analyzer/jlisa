package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class SynchronizedTest extends JLiSAAnalysisExecutor {

	@Test
	public void testNestedInnerClass1()  throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("synchronized", "synchronized-stmt-1", "Main.java");
		perform(conf);
	}
	
}

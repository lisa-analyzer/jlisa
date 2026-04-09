package it.unive.jlisa.recencyAbstraction;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class RecencyTestcases extends JLiSAAnalysisExecutor{
	
	@Test
	public void recency_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("recencyAbstraction", "",
				"Main.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_r() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Main.java");
		perform(conf);
	}

}

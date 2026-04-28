package it.unive.jlisa.recencyAbstraction;

import java.io.IOException;

import org.junit.Ignore;
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
	
	// Ignore for the moment, recency works only with heap
	@Ignore
	public void recency_test_stack() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test1.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_forloop() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test2.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_forloop2() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test3.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_whileloop() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test4.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_ifelse() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test5.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_forif() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test6.java");
		perform(conf);
	}

}

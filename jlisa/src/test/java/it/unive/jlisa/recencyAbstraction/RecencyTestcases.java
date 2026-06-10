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
	
	@Test
	public void recency_test_creation() throws IOException {
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
	public void recency_test_ifelsenested() throws IOException {
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

	@Test
	public void recency_test_ifelse1() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test7.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_ifelse2() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test8.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_aliasing() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test9.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_nullIdentifiers() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("recencyAbstraction", "",
				"Test10.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_nullIdentifiers2() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test10.java");
		perform(conf);
	}
	
	@Test
	public void recency_test_multipleFields() throws IOException {
		CronConfiguration conf = TestHelpers.recency("recencyAbstraction", "",
				"Test11.java");
		perform(conf);
	}
}

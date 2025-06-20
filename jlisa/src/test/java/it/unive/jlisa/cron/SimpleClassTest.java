package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class SimpleClassTest extends JLiSAAnalysisExecutor {
	
	@Test
	public void simpleClassTes1()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("test01", "", "Main.java", "B.java");
		perform(conf);
	}
	
	@Test
	public void simpleClassTes2()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("test02", "", "Main.java");
		perform(conf);
	}
	
	@Test
	public void simpleClassTes3()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("test03", "", "Test.java");
		perform(conf);
	}
}
package it.unive.jlisa.svcomp;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class SVCompTestcases extends JLiSAAnalysisExecutor {

	@Test
	public void instanceof1_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof1", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	@Test
	public void instanceof3_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof3", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	@Test
	public void instanceof4_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof4", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	@Test
	public void instanceof5_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof5", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	@Test
	public void iarith1_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "iarith1", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}	
	
	@Test
	public void iarith2_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "iarith2", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	@Test
	public void return1_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "return1", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}	
	
	@Test
	public void virtual2_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "virtual2", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
}

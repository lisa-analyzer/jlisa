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
	
	@Test
	public void athrow1_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "athrow1", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	@Test
	public void boolean1_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "boolean1", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	
	@Test
	public void const1_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "const1", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	@Test
	public void StaticCharMethods01_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StaticCharMethods01", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	@Test
	public void StaticCharMethods04_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StaticCharMethods04", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	@Test
	public void interface1_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "interface1", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}	
	
	// FIXME
	@Test
	public void NegativeArraySizeException1_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "NegativeArraySizeException1", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	@Test
	public void NullPointerException1_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "NullPointerException1", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
	
	// FIXME
	@Test
	public void StringCompare01_test()  throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringCompare01", 
				"Main.java",
				"Verifier.java");
		perform(conf);
	}
}

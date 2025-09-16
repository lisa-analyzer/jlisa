package it.unive.jlisa.svcomp;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

public class SVCompTestcases extends JLiSAAnalysisExecutor {

	@Test
	public void instanceof1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void instanceof3_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof3",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void instanceof4_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof4",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void instanceof5_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof5",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void iarith1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "iarith1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void iarith2_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "iarith2",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void return1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "return1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void virtual2_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "virtual2",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Ignore
	public void athrow1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "athrow1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void boolean1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "boolean1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void const1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "const1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void StaticCharMethods01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StaticCharMethods01",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void StaticCharMethods04_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StaticCharMethods04",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void interface1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "interface1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void NullPointerException1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "NullPointerException1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void NullPointerException4_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "NullPointerException4",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void ClassCastException1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "ClassCastException1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void NegativeArraySizeException1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"NegativeArraySizeException1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void exceptions1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "exceptions1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void exceptions6_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "exceptions6",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Ignore
	public void exceptions8_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "exceptions8",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void exceptions16_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "exceptions16",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void StringValueOf05_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringValueOf05",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void StringContains02_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringContains02",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void ArithmeticException6_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "ArithmeticException6",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void Class_method1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "Class_method1",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void instanceof8_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof8",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void StringValueOf08_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringValueOf08",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void StringValueOf09_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringValueOf09",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void StringConcatenation01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringConcatenation01",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void exceptions9_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "exceptions9",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void StringValueOf02_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringValueOf02",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	// FIXME: need equalsIgnoreCase and regionMatches
	@Test
	public void StringCompare01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringCompare01",
				"Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void Inheritance1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "inheritance1",
				"Main.java");
		perform(conf);
	}
}

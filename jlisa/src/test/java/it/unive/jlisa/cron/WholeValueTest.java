package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The whole-value analysis of constants, intervals, kinds of floating point
 * values and strings (JavaWholeValueAnalysis).
 */
public class WholeValueTest extends JLiSAAnalysisExecutor {

	private void run(
			String program)
			throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithWholeValue("wholevalue", program, "Main.java",
				"../../svcomp/common/");
		perform(conf);
	}

	@Test
	public void nanTest() throws IOException {
		// !(n <= 5) holds for NaN, which is not > 5
		run("nan");
	}

	@Test
	public void loopExitTest() throws IOException {
		// the exit of while (e != null) is reachable
		run("loop-exit");
	}

	@Test
	public void staleValueTest() throws IOException {
		// x = s.length() forgets x == 5 in the constants
		run("stale-value");
	}

	@Test
	public void floatTest() throws IOException {
		// NaN and infinities of float values, and comparisons with constants
		// that exclude an infinity (e.g., x <= 3.4e38f excludes +Inf)
		run("float");
	}

	@Test
	public void weakSelfComparisonTest() throws IOException {
		// a.length != b.length and a[0] != b[0] for two arrays of the same
		// allocation site, whose lengths and elements are the same (weak)
		// identifiers: x != x is not false for them
		run("weak-self-comparison");
	}
}

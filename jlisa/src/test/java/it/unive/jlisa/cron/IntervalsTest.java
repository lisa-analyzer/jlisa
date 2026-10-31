package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The intervals on floating point values (strict comparisons, rounding,
 * division by zero), on the overflow of integers, and on the narrowing
 * conversions of ranges.
 */
public class IntervalsTest extends JLiSAAnalysisExecutor {

	private void run(
			String program)
			throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithTarsis("intervals", program, "Main.java",
				"../../svcomp/common/");
		perform(conf);
	}

	@Test
	public void strictComparisonTest() throws IOException {
		// d < 0.7 does not give d <= -0.3 on doubles
		run("strict-comparison");
	}

	@Test
	public void roundingSumTest() throws IOException {
		// 0.1 + 0.2 is 0.30000000000000004
		run("rounding-sum");
	}

	@Test
	public void roundingDownTest() throws IOException {
		// 0.1 + 0.7 is 0.7999999999999999
		run("rounding-down");
	}

	@Test
	public void overflowTest() throws IOException {
		// -Integer.MIN_VALUE is Integer.MIN_VALUE
		run("overflow");
	}

	@Test
	public void divisionTest() throws IOException {
		// 1.0 / -0.0 is NEGATIVE_INFINITY, 0.0 / 0.0 is NaN
		run("division");
	}

	@Test
	public void wraparoundTest() throws IOException {
		// (byte) [128, 130] is [-128, -126]; (byte) [126, 129] crosses 127
		run("wraparound");
	}
}

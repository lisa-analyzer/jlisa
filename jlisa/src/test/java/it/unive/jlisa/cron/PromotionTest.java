package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The numeric promotion of byte, short and char (JLS 5.6) in the arithmetic and
 * bitwise operators, and the width of the shifts (JLS 15.19).
 */
public class PromotionTest extends JLiSAAnalysisExecutor {

	private void run(
			String program)
			throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithTarsis("promotion", program, "Main.java",
				"../../svcomp/common/");
		perform(conf);
	}

	@Test
	public void compoundAssignmentsTest() throws IOException {
		// x op= y on two byte, short or char variables, with overflow
		run("compound-assignments");
	}

	@Test
	public void charSumTest() throws IOException {
		// c += c1 with c = 65535 and c1 = 2 gives 1
		run("char-sum");
	}

	@Test
	public void shiftsTest() throws IOException {
		// 1 << 40L is an int shift: 1 << 8
		run("shifts");
	}
}

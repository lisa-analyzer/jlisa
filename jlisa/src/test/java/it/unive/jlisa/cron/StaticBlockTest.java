package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class StaticBlockTest extends JLiSAAnalysisExecutor {

	@Test
	public void testStaticBlock1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("static-block", "static-block-1", "Main.java");
		perform(conf);
	}

	@Test
	public void testStaticBlock2() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("static-block", "static-block-2", "Main.java");
		perform(conf);
	}

	@Test
	public void testStaticBlock3() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("static-block", "static-block-3", "Main.java");
		perform(conf);
	}

	@Test
	public void testStaticBlock4() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("static-block", "static-block-4", "Main.java");
		perform(conf);
	}
}


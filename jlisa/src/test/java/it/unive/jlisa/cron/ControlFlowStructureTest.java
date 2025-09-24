package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ControlFlowStructureTest extends JLiSAAnalysisExecutor {

	private static CronConfiguration createConfiguration(
			String testDir,
			String subDir,
			String... programFiles) {
		CronConfiguration configuration = TestHelpers.createConfiguration(testDir, subDir, programFiles);

		configuration.serializeInputs = true;
		configuration.serializeResults = false;
		configuration.jsonOutput = true;
		configuration.analysis = null;
		configuration.analysisGraphs = GraphType.NONE;
		return configuration;
	}

	@Test
	public void testWhile1() throws IOException {
		CronConfiguration conf = createConfiguration("control-flow-structures", "while", "while-1.java");
		perform(conf);
	}

	@Test
	public void testDoWhile1() throws IOException {
		CronConfiguration conf = createConfiguration("control-flow-structures", "do-while", "do-while-1.java");
		perform(conf);
	}

	@Test
	public void testFor() throws IOException {
		CronConfiguration conf = createConfiguration("control-flow-structures", "for/for", "for-1.java");
		perform(conf);
	}

	@Test
	public void testForEach() throws IOException {
		CronConfiguration conf = createConfiguration("control-flow-structures", "for/foreach", "for-2.java");
		perform(conf);
	}

	@Test
	public void testIf() throws IOException {
		CronConfiguration conf = createConfiguration("control-flow-structures", "if", "if-1.java");
		perform(conf);
	}

	@Test
	public void testBreakContinue() throws IOException {
		CronConfiguration conf = createConfiguration("control-flow-structures", "break-continue",
				"break-continue.java");
		perform(conf);
	}

	@Test
	public void testSwitch() throws IOException {
		CronConfiguration conf = createConfiguration("control-flow-structures", "switch", "Main.java");
		perform(conf);
	}

	@Test
	public void testException1() throws IOException {
		CronConfiguration conf = createConfiguration("control-flow-structures", "try-catch/try-catch1", "Main.java");
		perform(conf);
	}

	@Test
	public void testException2() throws IOException {
		CronConfiguration conf = createConfiguration("control-flow-structures", "try-catch/try-catch2", "Main.java");
		perform(conf);
	}
}

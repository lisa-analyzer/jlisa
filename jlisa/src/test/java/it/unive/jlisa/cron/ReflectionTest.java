package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.outputs.HtmlResults;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ReflectionTest extends JLiSAAnalysisExecutor {

	@Test
	public void testReflection() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "", "Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}
}

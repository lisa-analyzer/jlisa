package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.outputs.HtmlResults;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ReflectionTest extends JLiSAAnalysisExecutor {

	@Test
	public void testClassForName1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-for-name-1", "Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testGetField1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-get-field-1", "Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}
}

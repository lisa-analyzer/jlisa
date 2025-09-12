package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ParenthesizedExpressionTest extends JLiSAAnalysisExecutor {

	@Test
	public void parenthesizedTest1() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("parenthesized-expr", "", "Main.java");
		perform(conf);
	}
}

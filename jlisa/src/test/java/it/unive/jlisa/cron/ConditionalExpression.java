package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class ConditionalExpression extends JLiSAAnalysisExecutor {

	@Test
	public void testConditionalExpr()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("conditional-expr", "", "ConditionalExpr.java");
		perform(conf);
	}
}

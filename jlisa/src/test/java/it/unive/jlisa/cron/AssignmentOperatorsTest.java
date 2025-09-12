package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class AssignmentOperatorsTest extends JLiSAAnalysisExecutor {

	@Test
	public void testAssignmentOperators() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("asg-operators", "", "Main.java");
		perform(conf);
	}

}

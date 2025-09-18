package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.Test;

public class NestedInnerClassTest extends JLiSAAnalysisExecutor {

	@Test
	public void testNestedInnerClass1() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("nested-inner-class", "nest-1", "nest-1.java");
		perform(conf);
	}

}

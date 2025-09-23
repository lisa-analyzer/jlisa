package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.Ignore;

public class NestedInnerClassTest extends JLiSAAnalysisExecutor {

	// FIXME support for inner classes must be changed
	@Ignore
	public void testNestedInnerClass1() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("nested-inner-class", "nest-1", "nest-1.java");
		perform(conf);
	}

}

package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class NestedInnerClassTest extends JLiSAAnalysisExecutor {

	@Test
	public void testBasic() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("inner-classes", "inner1", "Basic.java");
		perform(conf);
	}

	@Test
	public void testMoreNested() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("inner-classes", "inner2", "MoreNested.java");
		perform(conf);
	}

}

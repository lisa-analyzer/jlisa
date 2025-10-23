package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class NestedInnerClassTest extends JLiSAAnalysisExecutor {
	
	@Test
	public void testBasic() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("inner-classes", "inner1", "Basic.java");
		perform(conf);
	}

	// FIXME support for inner classes must be changed
	@Ignore
	public void testNestedInnerClass1() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("nested-inner-class", "nest-1", "nest-1.java");
		perform(conf);
	}

}

package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class AnonymousClassTest extends JLiSAAnalysisExecutor {
	@Test
	public void testAnonymousClasses1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("anonymous-classes",
				"anonymous-classes-1",
				"Main.java");
		perform(conf);
	}

	@Test
	public void testAnonymousClasses2() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("anonymous-classes",
				"anonymous-classes-2",
				"Main.java");
		perform(conf);
	}

	@Test
	public void testAnonymousClasses3() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("anonymous-classes",
				"anonymous-classes-3",
				"Main.java");
		perform(conf);
	}

	@Test
	public void testAnonymousClasses4() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("anonymous-classes",
				"anonymous-classes-4",
				"Main.java");
		perform(conf);
	}
}

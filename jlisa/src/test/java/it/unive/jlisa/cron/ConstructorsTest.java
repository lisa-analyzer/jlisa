package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ConstructorsTest extends JLiSAAnalysisExecutor {

	@Test
	public void testThisConstructor() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "this-constructors", "Main.java",
				"B.java");
		perform(conf);
	}

	@Test
	public void testSuperConstructor1() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "super-constructor/test-1",
				"Main.java", "B.java");
		perform(conf);
	}

	@Test
	public void testSuperConstructor2() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "super-constructor/test-2",
				"Main.java", "B.java");
		perform(conf);
	}

	@Test
	public void testSuperConstructor3() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "super-constructor/test-3",
				"Main.java", "B.java", "A.java");
		perform(conf);
	}

	@Test
	public void testSuperConstructor4() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "super-constructor/test-4",
				"Main.java", "B.java", "A.java");
		perform(conf);
	}

	@Test
	public void testDefaultConstructor1() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "default-constructor", "Main.java",
				"B.java");
		perform(conf);
	}
}

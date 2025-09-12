package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ArrayCreationTest extends JLiSAAnalysisExecutor {

	@Test
	public void testArray() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("array", "", "Main.java");
		perform(conf);
	}

	@Test
	public void testArrayWithInitializer() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("array-with-initializer", "", "Main.java");
		perform(conf);
	}

}

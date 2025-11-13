package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

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

	@Disabled
	public void testTwoDimArray() throws IOException {
		// FIXME this has a double array access, see comment in JavaArrayAccess
		CronConfiguration conf = TestHelpers.constantPropagation("2dim-array", "", "Main.java");
		perform(conf);
	}

}

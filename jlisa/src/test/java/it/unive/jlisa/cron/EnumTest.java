package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class EnumTest extends JLiSAAnalysisExecutor {

	@Test
	public void testEnum01() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("enum", "enum01", "EnumExample.java", "Main.java");
		perform(conf);
	}

	@Test
	public void testEnum02() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("enum", "enum02", "Main.java");
		perform(conf);
	}
}

package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class EnumTest extends JLiSAAnalysisExecutor {

	@Test
	public void testEnum()  throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("enum", "enum01", "EnumExample.java", "Main.java");
		perform(conf);
	}
}

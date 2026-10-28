package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class RelationsTest extends JLiSAAnalysisExecutor {

	@Test
	public void testInBounds() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithRelations("relations", "in-bounds", "Main.java");
		perform(conf);
	}

	@Test
	public void testOverflow() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithRelations("relations", "overflow", "Main.java");
		perform(conf);
	}

	@Test
	public void testLength() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithRelations("relations", "length", "Main.java");
		perform(conf);
	}

	@Test
	public void testJoin() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithRelations("relations", "join", "Main.java");
		perform(conf);
	}
}

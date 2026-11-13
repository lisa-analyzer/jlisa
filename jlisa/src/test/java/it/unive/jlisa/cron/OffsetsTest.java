package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The relations with offsets of the upper bounds (JavaWholeValueUBOFF).
 */
public class OffsetsTest extends JLiSAAnalysisExecutor {

	@Test
	public void relationsTest() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithOffsets("offsets", "relations", "Main.java",
				"../../svcomp/common/");
		perform(conf);
	}
}

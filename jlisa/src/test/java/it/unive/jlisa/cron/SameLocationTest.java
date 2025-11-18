package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SameLocationTest extends JLiSAAnalysisExecutor {

	@Test
	public void testDataStructures3() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("same-location", "data-structures-3",
				"DataStructures3.java");
		perform(conf);
	}

	@Disabled
	public void testAdditionVerifier() throws IOException {
		// FIXME without recursion support, this now reaches the maximum
		// recursion depth
		CronConfiguration conf = TestHelpers.constantPropagation("same-location", "addition-verifier", "Main.java",
				"Verifier.java");
		perform(conf);
	}

	@Test
	public void testArrayIndexOutOfBoundException1() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("same-location", "ArrayIndexOutOfBoundsException",
				"Main.java", "Verifier.java");
		perform(conf);
	}
}

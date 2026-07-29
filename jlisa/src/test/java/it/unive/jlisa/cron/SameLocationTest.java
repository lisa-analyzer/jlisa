package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class SameLocationTest
		extends
		JLiSAAnalysisExecutor {

	@Test
	public void testDataStructures3() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("same-location", "data-structures-3",
				"DataStructures3.java");
		perform(conf);
	}

	@Test
	public void testAdditionVerifier() throws IOException {
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

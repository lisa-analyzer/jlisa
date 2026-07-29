package it.unive.jlisa.cron;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.testing.TestException;

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
		TestException thrown = assertThrows(TestException.class, () -> perform(conf));
		// get to the root cause
		Throwable rootCause = thrown;
		while (rootCause.getCause() != null)
			rootCause = rootCause.getCause();
		if (!(rootCause instanceof SemanticException)
				|| rootCause.getMessage() == null
				|| !rootCause.getMessage().equals("Maximum call stack depth reached"))
			fail("Expected a SemanticException with message 'Maximum call stack depth reached', but got: "
					+ rootCause.getClass().getName() + " with message: " + rootCause.getMessage());
	}

	@Test
	public void testArrayIndexOutOfBoundException1() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("same-location", "ArrayIndexOutOfBoundsException",
				"Main.java", "Verifier.java");
		perform(conf);
	}
}

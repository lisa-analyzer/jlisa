package it.unive.jlisa.analysis;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class ConstantPropagationTest extends JLiSAAnalysisExecutor {

	@Test
	public void ConstantPropagation()  throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("constant-propagation", "", "Main.java");
		perform(conf);
	}
	
}

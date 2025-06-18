package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class TypingTest extends JLiSAAnalysisExecutor {
	 
	@Test
	public void testTypingConversion()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("typing", "", "A.java");
		perform(conf);
	}
    
//    @Test
//    public void testTypingConversion() throws IOException {
//        JavaFrontend frontend = new JavaFrontend();
//        List<String> inputs = new ArrayList<>();
//        inputs.add("tests/typing/A.java");
//        frontend.parseFromListOfFile(inputs);
//
//        LiSA lisa = TestHelpers.getLiSA("outputs/typing/typing-1");
//        lisa.run(frontend.getProgram());
//    }
}

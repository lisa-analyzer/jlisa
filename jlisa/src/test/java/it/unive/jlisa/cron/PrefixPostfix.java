package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class PrefixPostfix extends JLiSAAnalysisExecutor {
	
	@Test
	public void testPrefixPostfix()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("prefix-postfix", "", "Main.java");
		perform(conf);
	}
	
//    @Test
//    public void testPrefixPostfix() throws IOException {
//        JavaFrontend frontend = new JavaFrontend();
//        List<String> inputs = new ArrayList<>();
//        inputs.add("tests/prefix-postfix/Main.java");
//        frontend.parseFromListOfFile(inputs);
//        LiSA lisa = TestHelpers.getLiSA("tests-output/prefix-postfix");
//        lisa.run(frontend.getProgram());
//    }
}

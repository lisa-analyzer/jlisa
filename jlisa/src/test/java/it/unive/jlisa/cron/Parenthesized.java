package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class Parenthesized extends JLiSAAnalysisExecutor {
	
//    @Test
//    public void parenthesizedTest1() throws IOException {
//        JavaFrontend frontend = new JavaFrontend();
//        List<String> inputs = new ArrayList<>();
//        inputs.add("tests/parenthesized-expr/Main.java");
//        frontend.parseFromListOfFile(inputs);
//
//        LiSA lisa = TestHelpers.getLiSA("tests-outputs/parenthesized-expr/parenthesized-expr-1");
//        lisa.run(frontend.getProgram());
//    }
    
	@Test
	public void parenthesizedTest1()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("parenthesized-expr", "", "Main.java");
		perform(conf);
	}
}

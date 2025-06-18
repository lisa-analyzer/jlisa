package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class Return extends JLiSAAnalysisExecutor {

	//	@Test
	//	public void testVoidReturn() throws IOException {
	//		JavaFrontend frontend = new JavaFrontend();
	//		List<String> inputs = new ArrayList<>();
	//		inputs.add("src/main");
	//		//inputs.add("tests/return-stmt/void-return/A.java");
	//		frontend.parseFromListOfFile(inputs);
	//		LiSA lisa = TestHelpers.getLiSA("tests-output/return-stmt/void-return");
	//		lisa.run(frontend.getProgram());
	//	}
	//

	@Test
	public void testReturn()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("return", "", "A.java", "Main.java");
		perform(conf);
	}
}

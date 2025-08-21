package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class ReturnTest extends JLiSAAnalysisExecutor {

	@Test
	public void testReturn1()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("return", "ret1", "A.java", "Main.java");
		perform(conf);
	}
	
	@Test
	public void testReturnIf1ControlFlow()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("return", "control-flow/if-1", "if1.java", "Main.java");
		perform(conf);
	}
	
	@Test
	public void testReturnIf2ControlFlow()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("return", "control-flow/if-2", "if2.java", "Main.java");
		perform(conf);
	}
	
	@Test
	public void testReturnDoWhileControlFlow()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("return", "control-flow/do-while", "do-while.java", "Main.java");
		perform(conf);
	}
}

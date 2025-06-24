package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class ControlFlowStructureTest extends JLiSAAnalysisExecutor {
    
	@Test
	public void testWhile1()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("control-flow-structures", "while", "while-1.java");
		perform(conf);
	}
	
	@Test
	public void testDoWhile1()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("control-flow-structures", "do-while", "do-while-1.java");
		perform(conf);
	}
	
	@Test
	public void testFor()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("control-flow-structures", "for/for", "for-1.java");
		perform(conf);
	}
	
	@Test
	public void testForEach()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("control-flow-structures", "for/foreach", "for-2.java");
		perform(conf);
	}
	
	@Test
	public void testIf()  throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("control-flow-structures", "if", "if-1.java");
		perform(conf);
	}
}

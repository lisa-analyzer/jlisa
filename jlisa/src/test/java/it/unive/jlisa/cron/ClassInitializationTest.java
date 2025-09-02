package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class ClassInitializationTest extends JLiSAAnalysisExecutor {

	@Test
	public void tesClassInit01()  throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("class-init", "class-init01", "Main.java", "MyClass.java");
		perform(conf);
	}
	
	@Test
	public void tesClassInit02()  throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("class-init", "class-init02", "Main.java", "A.java", "B.java");
		perform(conf);
	}
}

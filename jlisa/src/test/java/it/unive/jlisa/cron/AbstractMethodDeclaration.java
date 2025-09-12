package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class AbstractMethodDeclaration extends JLiSAAnalysisExecutor {

	@Test
	public void testAbstractMethodDeclaration() throws IOException {
		CronConfiguration conf = TestHelpers.createConfiguration("abstract-method", "", "Main.java");
		perform(conf);
	}

}

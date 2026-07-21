package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class InterfaceInitializationTest extends JLiSAAnalysisExecutor {

	@Test
	public void testInterfaceInit01() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("interface-init", "interface-init01", "Main.java",
				"MyInterface.java");
		perform(conf);
	}

	@Test
	public void testInterfaceInit02() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("interface-init", "interface-init02", "Main.java",
				"MyInterface.java", "Helper.java");
		perform(conf);
	}

	@Test
	public void testInterfaceInit03() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("interface-init", "interface-init03", "Main.java",
				"MyInterface.java", "Helper.java");
		perform(conf);
	}
}

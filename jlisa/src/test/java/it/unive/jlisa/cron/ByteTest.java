package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.conf.LiSAConfiguration;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ByteTest extends JLiSAAnalysisExecutor {

	@Test
	public void testByte() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("byte", "", "ByteTest.java");
		conf.analysisGraphs = LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
		perform(conf);
	}

}
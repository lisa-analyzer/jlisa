package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class CharacterTest extends JLiSAAnalysisExecutor {

	@Test
	public void testCharacter() throws IOException {
		CronConfiguration conf = TestHelpers.constantPropagation("character", "", "Main.java");
		perform(conf);
	}

}
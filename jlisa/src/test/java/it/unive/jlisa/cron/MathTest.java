package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class MathTest extends JLiSAAnalysisExecutor {
	
    @Test
    public void testMath()  throws IOException {
        CronConfiguration conf = TestHelpers.constantPropagation("math", "", "Main.java");
        perform(conf);
    }
}
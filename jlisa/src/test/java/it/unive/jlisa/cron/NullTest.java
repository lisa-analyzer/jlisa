package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class NullTest extends JLiSAAnalysisExecutor {
	
    @Test
    public void testNull01()  throws IOException {
        CronConfiguration conf = TestHelpers.constantPropagation("nulltest", "nulltest01", "Main.java");
        perform(conf);
    }
    
    @Test
    public void testNull02()  throws IOException {
        CronConfiguration conf = TestHelpers.constantPropagation("nulltest", "nulltest02", "Main.java");
        perform(conf);
    }
    
}
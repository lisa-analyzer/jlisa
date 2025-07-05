package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class StringTest extends JLiSAAnalysisExecutor {
    @Test
    public void testEmptyStringConstruction()  throws IOException {
        CronConfiguration conf = TestHelpers.createConfiguration("strings", "", "StringTest.java");
        perform(conf);
    }
}

package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SVCompTest  extends JLiSAAnalysisExecutor {

    @Test
    public void testString() throws IOException {
        CronConfiguration conf = TestHelpers.constantPropagation("sv-comp", "data-structures-3", "DataStructures3.java");
        perform(conf);
    }
}

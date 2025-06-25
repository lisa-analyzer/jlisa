package it.unive.jlisa.cron;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LiteralTest extends JLiSAAnalysisExecutor {
    @Test
    public void testByte1() throws IOException {
        CronConfiguration conf = TestHelpers.createConfiguration("literals", "byte", "ByteTest.java");
        perform(conf);
    }

    @Test
    public void testChar1() throws IOException {
        CronConfiguration conf = TestHelpers.createConfiguration("literals", "char", "CharTest.java");
        perform(conf);
    }
}

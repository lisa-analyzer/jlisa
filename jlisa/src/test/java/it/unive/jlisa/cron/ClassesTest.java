package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ClassesTest  extends JLiSAAnalysisExecutor {

    @Test
    public void test1()  throws IOException {
        CronConfiguration conf = TestHelpers.createConfiguration("classes", "test1", "Main.java", "A.java", "B.java");
        perform(conf);
    }

    @Test
    public void test2()  throws IOException {
        CronConfiguration conf = TestHelpers.createConfiguration("classes", "test2", "Main.java", "A.java", "B.java", "C.java");
        perform(conf);
    }

    @Test
    public void test3()  throws IOException {
        CronConfiguration conf = TestHelpers.createConfiguration("classes", "test3", "Main.java", "A.java", "B.java", "C.java");
        perform(conf);
    }

}

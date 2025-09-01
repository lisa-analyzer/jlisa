package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class MethodInvocationTest  extends JLiSAAnalysisExecutor {

    @Test
    public void test01()  throws IOException {
        CronConfiguration conf = TestHelpers.constantPropagation("method-invocation", "test1", "Main.java", "A.java", "B.java");
        perform(conf);
    }

    @Test
    public void test02()  throws IOException {
        CronConfiguration conf = TestHelpers.constantPropagation("method-invocation", "test2", "Main.java", "A.java", "B.java", "C.java");
        perform(conf);
    }

    @Test
    public void test03()  throws IOException {
        CronConfiguration conf = TestHelpers.constantPropagation("method-invocation", "test3", "Main.java", "A.java", "B.java", "C.java");
        perform(conf);
    }
    
    @Test
    public void superTest01()  throws IOException {
        CronConfiguration conf = TestHelpers.constantPropagation("method-invocation", "super-test1", "Main.java", "A.java", "B.java");
        perform(conf);
    }
    
    @Test
    public void superTest02()  throws IOException {
        CronConfiguration conf = TestHelpers.constantPropagation("method-invocation", "super-test2", "C.java", "Main.java", "A.java", "B.java");
        perform(conf);
    }

}

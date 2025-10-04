package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import org.junit.jupiter.api.Test;

public class EwolffMicroserviceSmokeTest {

    @Test
    void runOnEwolff() throws Exception {

        String testDir = "ewolff-microservice";
        String subDir  = "catalog";
        String file1   = "Item.java";

        CronConfiguration conf = TestHelpers.constantPropagation(testDir, subDir, file1);

        conf.forceUpdate = true;

        new JLiSAAnalysisExecutor() {
            @Override
            public void perform(CronConfiguration conf) throws java.io.IOException {
                super.perform(conf, false);
            }
        }.perform(conf);

    }
}

package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import org.junit.jupiter.api.Test;

public class MyDatasetSmokeTest {

	@Test
	void runOnMyDataset() throws Exception {

		String testDir = "piggymetrics";

		CronConfiguration conf = TestHelpers.createConfiguration(
				testDir,
				null,
				"Account.java",
				"User.java",
				"Item.java",
				"Saving.java",
				"Currency.java",
				"TimePeriod.java");

		conf.forceUpdate = true;

		new JLiSAAnalysisExecutor() {
		}.perform(conf);
	}
}

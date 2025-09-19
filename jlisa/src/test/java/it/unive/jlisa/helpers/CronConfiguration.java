package it.unive.jlisa.helpers;

import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.util.testing.TestConfiguration;

import java.util.List;

/**
 * An extended {@link TestConfiguration} that also holds test configuration
 * keys. This configuration disables optimizations
 * ({@link LiSAConfiguration#optimize}) by default.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class CronConfiguration extends TestConfiguration {

	/**
	 * The list of names of the Java source file to be searched in
	 * {@link #testDir}.
	 */
	public List<String> programFiles;

	public CronConfiguration() {
		optimize = false;
		programFile = "";
		compareWithOptimization = false;
	}
}
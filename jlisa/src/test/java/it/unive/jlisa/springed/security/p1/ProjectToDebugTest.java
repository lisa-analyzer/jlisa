package it.unive.jlisa.springed.security.p1;

import it.unive.jlisa.springed.Main;
import it.unive.jlisa.springed.SpringTestHelper;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ProjectToDebugTest {

	private static final Path ZIP = Path.of("spring-testcases/security/case-3.zip");

	@TempDir
	Path project;

	@Test
	public void debuggingTest() throws IOException {
		SpringTestHelper.unzip(ZIP, project);
		Main.main(new String[] { project.toString() });
	}
}

package it.unive.jlisa.helpers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.testing.AnalysisTestExecutor;
import it.unive.lisa.util.testing.TestConfiguration;

public abstract class JLiSAAnalysisExecutor extends AnalysisTestExecutor {

	protected static final String EXPECTED_RESULTS_DIR = "java-testcases";
	protected static final String ACTUAL_RESULTS_DIR = "java-outputs";

	public JLiSAAnalysisExecutor() {
		super(EXPECTED_RESULTS_DIR, ACTUAL_RESULTS_DIR);
	}

	@Override
	public Program readProgram(TestConfiguration conf, Path target) {
		CronConfiguration cc = (CronConfiguration) conf;
		Objects.requireNonNull(cc.programFiles);

		Path expectedPath = Paths.get(EXPECTED_RESULTS_DIR, conf.testDir);
		Path actualPath = Paths.get(ACTUAL_RESULTS_DIR, conf.testDir);

		if (conf.testSubDir != null) {
			expectedPath = Paths.get(expectedPath.toString(), conf.testSubDir);
			actualPath = Paths.get(actualPath.toString(), conf.testSubDir);
		}

		List<Path> targets = new ArrayList<Path>();
		for (String p : cc.programFiles) {
			Path path = Paths.get(expectedPath.toString(), cc.testDir);
			if (cc.testSubDir != null)
				path = Paths.get(path.toString(), cc.testSubDir);
			targets.add(Paths.get(expectedPath.toString(), p));
		}

		try {
			return new JavaFrontend().parseFromListOfFile(targets.stream()
				.map(t -> t.toString())
				.collect(Collectors.toList()));
		} catch (IOException e) {
			throw new AnalysisExecutionException("Cannot parse the input program", e);
		}
	}

}
package it.unive.jlisa.frontend;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.frontend.LiSAFrontend;
import it.unive.lisa.program.Program;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Smoke tests for {@link JavaFrontend}'s {@link LiSAFrontend} SPI conformance.
 * Verifies that the (mainFile, projectDir) constructor + {@code toLiSAProgram}
 * pair produce a valid {@link Program} for a trivial fixture, and that the
 * legacy no-arg constructor surfaces a clear error when used through the SPI.
 */
public class JavaFrontendLiSAFrontendSpiTest {

	private static final Path EMPTY_METHOD_FIXTURE = Path.of(
			"java-testcases", "empty-method", "empty-method.java")
			.toAbsolutePath();

	@Test
	public void toLiSAProgramReturnsNonEmptyProgramForSingleFile() throws IOException, AnalysisSetupException {
		LiSAFrontend frontend = new JavaFrontend(EMPTY_METHOD_FIXTURE.toString(), null);

		Program program = frontend.toLiSAProgram();

		assertNotNull(program, "toLiSAProgram() must not return null");
		assertNotNull(program.getTypes(), "Program must have a TypeSystem after parsing");
		assertTrue(
				!program.getUnits().isEmpty(),
				"Program must contain at least one unit after parsing empty-method.java");
	}

	@Test
	public void toLiSAProgramOnLegacyNoArgConstructorThrowsAnalysisSetupException() {
		JavaFrontend frontend = new JavaFrontend();

		assertThrows(AnalysisSetupException.class, frontend::toLiSAProgram,
				"toLiSAProgram() must reject frontends built without a source path");
	}
}

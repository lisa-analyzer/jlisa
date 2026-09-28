package it.unive.jlisa.springed.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedAnnotationException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.springed.Main;
import it.unive.jlisa.springed.SpringTestCases;
import it.unive.jlisa.springed.exceptions.SpringCSVExceptionWriter;
import it.unive.jlisa.springed.exceptions.UnresolvedTypeException;
import it.unive.lisa.program.SourceCodeLocation;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests of what springed writes: the CSV of the collected errors and
 * the {@link Main} entry point. See {@link Snapshots} for how to run the suite.
 */
public class SpringOutputRegressionTest {

	@Test
	public void csvHasOneRowPerErrorCategory(
			@TempDir Path workDir)
			throws IOException {
		// the writer creates the missing parent directories
		Path csv = workDir.resolve("nested").resolve("errors.csv");

		// messages are never null: master changes how those are written
		SpringCSVExceptionWriter.writeCSV(csv.toString(), List.of(
				new UnresolvedTypeException("Foo does not exist in the program (referenced at 'A.java':3:5)", "Foo"),
				new UnsupportedStatementException("Bar is not a class or interface unit"),
				new UnsupportedAnnotationException("Unsupported annotation value type: class X"),
				new IllegalStateException("boom, location: 'B.java':1:1"),
				new ParsingException("java-import", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Static imports are not supported.", new SourceCodeLocation("A.java", 2, 1))));

		// the location of non-parsing errors is where they were raised
		List<String> lines = Files.readAllLines(csv).stream()
				.map(line -> line.replaceFirst("\"SpringOutputRegressionTest\\.java:\\d+\"", "\"<here>\""))
				.toList();
		assertEquals(List.of(
				"\"Category\";\"Location\";\"Error Message\";\"Cause\"",
				"\"Unresolvable Type\";\"<here>\";\"Foo does not exist in the program (referenced at 'A.java':3:5)\";\"Foo\"",
				"\"Unsupported Statement\";\"<here>\";\"Bar is not a class or interface unit\";\"\"",
				"\"Unsupported Annotation Variation\";\"<here>\";\"Unsupported annotation value type: class X\";\"\"",
				"\"Uncategorized\";\"<here>\";\"boom\";\"\"",
				"\"java-import\";\"Static imports are not supported.\";\"UNSUPPORTED_STATEMENT\";\"'A.java':2:1\""),
				lines);
	}

	@Test
	public void mainRunsOverAllTheSampleProjects(
			@TempDir Path workDir)
			throws IOException {
		for (String caseName : SpringFixtures.CASES)
			SpringTestCases.extract(caseName, workDir);

		// Main writes into the working directory: preserve what is there
		Path output = Path.of("spring-outputs", "registry.json");
		boolean hadOutputDir = Files.isDirectory(output.getParent());
		byte[] previous = Files.isRegularFile(output) ? Files.readAllBytes(output) : null;
		try {
			Main.main(new String[] { workDir.toString() });

			assertTrue(Files.isRegularFile(output), () -> "expected output file at " + output.toAbsolutePath());
			JsonNode json = new ObjectMapper().readTree(output.toFile());
			assertTrue(json.isObject(), () -> "the output is not a JSON object: " + json);
		} finally {
			if (previous != null)
				Files.write(output, previous);
			else
				Files.deleteIfExists(output);

			if (!hadOutputDir)
				try {
					Files.deleteIfExists(output.getParent());
				} catch (DirectoryNotEmptyException keepOtherOutputs) {
					// the output directory holds other files: leave it
				}
		}
	}
}

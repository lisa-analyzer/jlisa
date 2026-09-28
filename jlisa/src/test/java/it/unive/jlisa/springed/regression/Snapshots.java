package it.unive.jlisa.springed.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Golden-file snapshots of the springed merge-regression suite. They are stored
 * under {@link #ROOT}, resolved against the Gradle project directory (as
 * {@code spring-testcases} is).
 * <p>
 * Run the suite with:
 *
 * <pre>
 * ./gradlew test --offline --rerun --tests 'it.unive.jlisa.springed.regression.*'
 * </pre>
 *
 * After an intended behaviour change, re-record every snapshot by setting the
 * environment variable {@value #UPDATE_ENV} to {@code true}:
 *
 * <pre>
 * SPRINGED_REGRESSION_UPDATE=true ./gradlew test --offline --rerun --tests 'it.unive.jlisa.springed.regression.*'
 * </pre>
 *
 * A missing snapshot is recorded and its test fails once, so that a baseline is
 * never created silently.
 */
final class Snapshots {

	static final Path ROOT = Path.of("src", "test", "resources", "springed-regression");

	static final String UPDATE_ENV = "SPRINGED_REGRESSION_UPDATE";

	private Snapshots() {
	}

	static void assertMatches(
			String name,
			String actual)
			throws IOException {
		Path file = ROOT.resolve(name);
		String current = normalizeLineEndings(actual);

		if (Boolean.parseBoolean(System.getenv(UPDATE_ENV))) {
			write(file, current);
			return;
		}

		if (!Files.isRegularFile(file)) {
			write(file, current);
			fail("recorded the missing snapshot " + file.toAbsolutePath() + ": review it and run the test again");
		}

		String expected = normalizeLineEndings(Files.readString(file));
		assertEquals(expected, current, () -> "snapshot " + name + " differs from the recorded baseline "
				+ file.toAbsolutePath() + firstDifference(expected, current)
				+ "\nre-record with " + UPDATE_ENV + "=true only if the change is intended");
	}

	private static void write(
			Path file,
			String content)
			throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

	private static String normalizeLineEndings(
			String text) {
		return text.replace("\r\n", "\n");
	}

	private static String firstDifference(
			String expected,
			String actual) {
		String[] exp = expected.split("\n", -1);
		String[] act = actual.split("\n", -1);
		for (int i = 0; i < Math.max(exp.length, act.length); i++) {
			String e = i < exp.length ? exp[i] : "<end of snapshot>";
			String a = i < act.length ? act[i] : "<end of output>";
			if (!e.equals(a))
				return "\nfirst difference at line " + (i + 1) + ":\n  expected: " + e + "\n  actual:   " + a;
		}
		return "";
	}
}

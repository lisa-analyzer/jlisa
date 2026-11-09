package it.unive.jlisa.svcomp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.jlisa.witness.validator.model.WitnessFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link WitnessFiles}: the files named by a witness are the input
 * files of the task with the same hashes, whatever their paths.
 */
public class WitnessFilesTest {

	private static String sha256(
			Path file)
			throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
	}

	@Test
	public void filesAreMatchedByHash(
			@TempDir Path dir)
			throws Exception {
		Path task = Files.createDirectories(dir.resolve("task"));
		Path main = Files.writeString(task.resolve("Main.java"), "public class Main {}\n");
		Path other = Files.writeString(task.resolve("Other.java"), "public class Other {}\n");

		// paths of another machine, and a file with another name
		String foreignMain = "/elsewhere/benchmarks/task/Main.java";
		String renamed = "/elsewhere/Renamed.java";
		WitnessFiles files = WitnessFiles.of(
				Map.of(foreignMain, sha256(main), renamed, sha256(other).toUpperCase()),
				List.of(dir.toFile()));

		assertEquals(main.toString(), files.resolve(foreignMain));
		assertEquals(other.toString(), files.resolve(renamed));
		// the same path, written differently
		assertEquals(main.toString(), files.resolve("/elsewhere/benchmarks/x/../task/Main.java"));
		// files without a hash are left as they are
		assertEquals("/unknown/A.java", files.resolve("/unknown/A.java"));
	}

	@Test
	public void filesWithTheSameContentsKeepTheirName(
			@TempDir Path dir)
			throws Exception {
		Path a = Files.writeString(Files.createDirectories(dir.resolve("a")).resolve("A.java"), "class X {}\n");
		Path b = Files.writeString(Files.createDirectories(dir.resolve("b")).resolve("B.java"), "class X {}\n");
		WitnessFiles files = WitnessFiles.of(Map.of("/w/B.java", sha256(a)), List.of(dir.toFile()));
		assertEquals(b.toString(), files.resolve("/w/B.java"));
	}
}

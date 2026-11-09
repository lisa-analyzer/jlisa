package it.unive.jlisa.svcomp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.jlisa.witness.validator.violation.SourceFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link SourceFiles}: files of the task are told apart by their
 * packages, as JDI tells the sources of the code run by the program.
 */
public class SourceFilesTest {

	@Test
	public void filesWithTheSameNameAreToldApartByTheirPackages(
			@TempDir Path dir)
			throws Exception {
		// the folders do not follow the packages
		Path first = Files.writeString(Files.createDirectories(dir.resolve("a")).resolve("Util.java"),
				"/* license */\n// a comment\npackage p1;\n\nclass Util {}\n");
		Path second = Files.writeString(Files.createDirectories(dir.resolve("b")).resolve("Util.java"),
				"package p2 . sub;\nclass Util {}\n");
		Path main = Files.writeString(dir.resolve("Main.java"), "public class Main {}\n");

		assertTrue(SourceFiles.matches(first.toString(), "p1/Util.java"));
		assertFalse(SourceFiles.matches(first.toString(), "p2/sub/Util.java"));
		assertTrue(SourceFiles.matches(second.toString(), "p2/sub/Util.java"));
		assertFalse(SourceFiles.matches(second.toString(), "p1/Util.java"));
		// the default package
		assertTrue(SourceFiles.matches(main.toString(), "Main.java"));
		assertFalse(SourceFiles.matches(main.toString(), "p1/Main.java"));
		// stack trace elements: the package of the class
		assertTrue(SourceFiles.contains(second.toString(), "p2.sub.Util$Inner", "Util.java"));
		assertFalse(SourceFiles.contains(first.toString(), "p2.sub.Util", "Util.java"));
	}

	@Test
	public void filesThatAreNotOfTheTaskAreComparedByName() {
		assertTrue(SourceFiles.matches("/elsewhere/Main.java", "Main.java"));
		assertTrue(SourceFiles.matches("/elsewhere/Util.java", "p1/Util.java"));
		assertFalse(SourceFiles.matches("/elsewhere/Util.java", "p1/Main.java"));
	}
}

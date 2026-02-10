package it.unive.jlisa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.stream.Collectors;

public class MicroSmokeTest {

	// Smoke test for the micro example (com.example.micro)
	@Test
	public void testMicroDemo() throws Exception {
		Path outDir = Paths.get("tests-output", "micro").toAbsolutePath().normalize();
		deleteOldArtifacts(outDir);
		Files.createDirectories(outDir);

		String[] args = new String[] {
				"-s", "src/test/resources/com/example/micro",
				"-o", outDir.toString(),
				"-n", "ConstantPropagation",
				"-l", "INFO"
		};

		// Run JLiSA main entry point
		Main.main(args);

		// Minimal, safe oracle
		assertTrue(Files.exists(outDir.resolve("report.json")),
				"Expected report.json to be produced in: " + outDir);
	}
	private static void deleteOldArtifacts(Path outDir) throws IOException {
		if (!Files.isDirectory(outDir))
			return;

		List<Path> toDelete;
		try (Stream<Path> s = Files.walk(outDir)) {
			toDelete = s.filter(Files::isRegularFile)
					.filter(p -> {
						String name = p.getFileName().toString();
						return name.equals("report.json") || name.endsWith(".html");
					})
					.collect(Collectors.toList());
		}

		for (Path p : toDelete)
			Files.deleteIfExists(p);
	}
}

package it.unive.jlisa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class DemoHelloSmokeTest {

	@Test
	public void testHelloDemo_graphTitleIncludesGetMapping() throws Exception {

		Path outDir = Paths.get("tests-output", "demo-hello").toAbsolutePath().normalize();
		Files.createDirectories(outDir);

		String[] args = new String[] {
				"-s", "src/test/resources/demo/com/example/demo",
				"-o", outDir.toString(),
				"-n", "ConstantPropagation",
				"-l", "INFO"
		};

		Main.main(args);

		boolean ok = htmlContainsGetMappingForHello(outDir);
		assertTrue(ok,
				"Expected HTML graph title to contain '@GetMapping' for DemoApplication::hello. Output dir: " + outDir);
	}

	private static boolean htmlContainsGetMappingForHello(Path dir) throws IOException {
		try (Stream<Path> files = Files.walk(dir)) {
			return files
					.filter(p -> p.toString().endsWith(".html"))
					.anyMatch(p -> {
						try {
							String html = Files.readString(p);
							return html.contains("Graph: @GetMapping")
									&& (html.contains("DemoApplication::hello") || html.contains("DemoApplication.hello"));
						} catch (IOException e) {
							return false;
						}
					});
		}
	}
}

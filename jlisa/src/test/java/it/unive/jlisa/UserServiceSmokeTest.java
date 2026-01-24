package it.unive.jlisa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class UserServiceSmokeTest {

    private static final String REST_CONTROLLER = "@RestController";

    @Test
    public void testUserService_graphTitlesIncludeSpringMappings() throws Exception {
        // output dir requested by the plan
        Path outDir = Paths.get("tests-output", "user-service").toAbsolutePath().normalize();
        deleteIfExists(outDir);
        Files.createDirectories(outDir);

        // IMPORTANT:
        // Analyze ONLY the controller package to avoid hitting SpringApplication.run(...)
        // (otherwise JLiSA crashes: "No class type org.springframework.boot.SpringApplication has been registered")
        String sourcePath = Paths.get(
                "examples", "user-service", "src", "main", "java",
                "com", "example", "userservice", "controller"
        ).toString();

        String[] args = new String[] {
                "-s", sourcePath,
                "-o", outDir.toString(),
                "-n", "ConstantPropagation",
                "-l", "INFO"
        };

        Main.main(args);

        // Now validate the generated HTML contains the expected titles
        assertGraphExists(outDir, "@GetMapping", "UserController", "info");
        assertGraphExists(outDir, "@PostMapping", "UserController", "create");
        assertGraphExists(outDir, "@PutMapping", "UserController", "update");
        assertGraphExists(outDir, "@DeleteMapping", "UserController", "delete");
        Path dump = outDir.resolve("field-annotations.txt");
        assertTrue(Files.exists(dump), "Missing field annotations dump: " + dump);

        String dumpContent = Files.readString(dump, StandardCharsets.UTF_8);

        assertTrue(dumpContent.contains("::dataAccessor"),
                "Missing field key for dataAccessor in dump.\n" + dumpContent);

        assertTrue(dumpContent.contains("@Autowired"),
                "Missing @Autowired in dump.\n" + dumpContent);

    }

    private static void assertGraphExists(Path outDir, String annotation, String clazz, String method) throws IOException {
        // We accept both "UserController::info" and "UserController.info"
        boolean ok = anyHtmlContains(outDir,
                "Graph:", annotation, REST_CONTROLLER, clazz, ("::" + method))
                || anyHtmlContains(outDir,
                "Graph:", annotation, REST_CONTROLLER, clazz, ("." + method));

        assertTrue(ok,
                "Expected an HTML graph title containing: Graph: "
                        + annotation + " + " + REST_CONTROLLER + " + " + clazz + " + " + method
                        + " inside " + outDir);
    }

    private static boolean anyHtmlContains(Path dir, String... needles) throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            return files
                    .filter(p -> p.toString().endsWith(".html"))
                    .anyMatch(p -> fileContainsAll(p, needles));
        }
    }

    private static boolean fileContainsAll(Path file, String... needles) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String n : needles) {
                if (!content.contains(n))
                    return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void deleteIfExists(Path path) throws IOException {
        if (!Files.exists(path))
            return;

        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // ignore
                        }
                    });
        }
    }
}

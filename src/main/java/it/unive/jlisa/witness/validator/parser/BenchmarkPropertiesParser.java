package it.unive.jlisa.witness.validator.parser;

import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.model.BenchmarkProperties;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BenchmarkPropertiesParser {
    private static final Logger LOG = LogManager.getLogger(BenchmarkPropertiesParser.class);

    public enum BenchType {
        VALID_ASSERT,
        NO_RUNTIME_EXCEPTION
    }

    private BenchmarkPropertiesParser() {
    }

    private static Map<String, Object> loadYaml(File file) {
        Yaml yaml = new Yaml(
                new SafeConstructor(new LoaderOptions())
        );

        try (FileReader reader = new FileReader(file)) {
            Object loaded = yaml.load(reader);

            if (!(loaded instanceof Map<?, ?> map)) {
                throw new RuntimeException("YAML witness root is not a mapping");
            }
            return (Map<String, Object>) map;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static Map<BenchType, Boolean> parseBenchProperties(Map<String, Object> properties) throws ValidationException {
        Map<BenchType, Boolean> map = new HashMap<>();

        if (properties.get("properties") instanceof List) {
            for (var propFile : (List<?>) properties.get("properties")) {
                if (propFile instanceof Map<?, ?>) {
                    String k = (String) ((Map<?, ?>) propFile).get("property_file");
                    Boolean v = (Boolean) ((Map<?, ?>) propFile).get("expected_verdict");

                    if (k.contains("no-runtime-exception")) {
                        System.out.println("no runtime exception: " + v);
                        map.put(BenchType.VALID_ASSERT, v);
                    } else if (k.contains("valid-assert")) {
                        System.out.println("valid assert: " + v);
                        map.put(BenchType.NO_RUNTIME_EXCEPTION, v);
                    }
                } else {
                    throw new ValidationException("Unparsable benchmark properties");
                }
            }
        }

        return map;
    }

    private static Pair<List<File>, File> parseInputFiles(File file, Map<String, Object> properties) throws FileNotFoundException {
        List<File> inputFiles = new ArrayList<>();
        if (properties.get("input_files") instanceof List) {
            inputFiles.addAll(
                    ((List<?>) properties.get("input_files")).stream()
                            .map((f) -> {
                                if (f instanceof String) {
                                    Path path = Paths.get(file.getParent(), (String) f);
                                    return path.toFile();
                                } else {
                                    return null;
                                }
                            }).toList()
            );
        }

        List<File> benchCandidates = inputFiles.stream().filter(str -> file.getName().contains(str.getName())).toList();
        if (benchCandidates.size() != 1) {
            throw new FileNotFoundException("Benchmark directory not found, " + file.getName() + " expected, " + benchCandidates.size() + " candidates found");
        }
        File benchDir = benchCandidates.getFirst();

        return Pair.of(inputFiles, benchDir);
    }

    public static BenchmarkProperties parse(File file) throws FileNotFoundException, ValidationException {
        Map<String, Object> properties = loadYaml(file);

        Map<BenchType, Boolean> benchType = parseBenchProperties(properties);
        Pair<List<File>, File> inputFiles = parseInputFiles(file, properties);

        return new BenchmarkProperties(inputFiles.getLeft(), inputFiles.getRight(),
                benchType.getOrDefault(BenchType.VALID_ASSERT, false),
                benchType.getOrDefault(BenchType.NO_RUNTIME_EXCEPTION, false)
        );
    }
}

package it.unive.jlisa.witness.validator.parser;

import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.model.*;
import it.unive.jlisa.witness.validator.model.CorrectnessInvariant.InvariantType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses format v2.0 YAML witnesses (both violation and correctness) into a {@link WitnessModel}.
 *
 * <h3>Violation witnesses</h3>
 * <p>The YAML {@code content} block contains one or more {@code segment} entries.  Each segment
 * has a {@code waypoints} list.  We map waypoints to plan elements:
 * <ul>
 *   <li>{@code assumption} — variable assignment at a line; becomes a potential
 *       {@link Interception} if the target is a {@code Verifier.nondet*} call, or a future
 *       {@code VariableAssignment} otherwise.</li>
 *   <li>{@code branching} — branch direction hint; becomes a {@link BranchDecision}.</li>
 *   <li>{@code target} — property violation location; becomes a {@link TargetPoint}.</li>
 *   <li>{@code avoid} — location that must not be reached; becomes an {@link AvoidPoint}.</li>
 *   <li>{@code function_enter} / {@code function_return} — informational, no action needed.</li>
 * </ul>
 *
 * <h3>Correctness witnesses</h3>
 * <p>The YAML {@code content} block contains {@code invariant} entries with {@code loop_invariant}
 * or {@code location_invariant} type.
 */
public final class WitnessParserV2 {

    private static final Logger LOG = LogManager.getLogger(WitnessParserV2.class);

    private WitnessParserV2() {
    }

    /**
     * Parses the given YAML file into a {@link WitnessModel}.
     *
     * @param witnessFile the {@code .yaml} / {@code .yml} witness file
     * @return the parsed model
     * @throws ValidationException if the file cannot be parsed or has an unrecognised structure
     */
    @SuppressWarnings("unchecked")
    public static WitnessModel parse(File witnessFile) throws ValidationException {
        Map<String, Object> root = loadYaml(witnessFile);

        // Metadata
        String producer = extractProducerName(root);
        String programFile = extractProgramFile(root);
        LOG.debug("[PARSE] v2 witness: producer={} programfile={}", producer, programFile);

        // Content discriminator: presence of "violation_sequence" vs "invariant" items
        Object contentObj = root.get("content");
        if (!(contentObj instanceof List<?> contentList) || contentList.isEmpty()) {
            throw new ValidationException("YAML witness has no 'content' block or it is empty");
        }

        // Inspect first content item to decide violation vs correctness
        Object firstItem = contentList.get(0);
        if (!(firstItem instanceof Map<?, ?> firstMap)) {
            throw new ValidationException("YAML witness content is not a mapping");
        }

        if (firstMap.containsKey("invariant") || firstMap.containsKey("invariant_set")) {
            return parseCorrectness((List<Object>) contentList, producer, programFile);
        } else {
            return parseViolation((List<Object>) contentList, producer, programFile);
        }
    }

    // ------------------------------------------------------------------
    // violation parsing
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static WitnessModel parseViolation(
            List<Object> content, String producer, String programFile) throws ValidationException {

        List<Interception> interceptions = new ArrayList<>();
        List<BranchDecision> branches = new ArrayList<>();
        List<AvoidPoint> avoids = new ArrayList<>();
        List<TargetPoint> targets = new ArrayList<>();

        for (Object item : content) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                continue;
            }
            // Content items may be "segment" maps or "violation_sequence" containing segments
            List<Object> waypoints = collectWaypoints((Map<String, Object>) itemMap);
            for (Object wpObj : waypoints) {
                if (!(wpObj instanceof Map<?, ?> wpMap)) {
                    continue;
                }
                processWaypoint((Map<String, Object>) wpMap, interceptions, branches, avoids, targets);
            }
        }

        LOG.debug("[PARSE] v2 violation: {} interceptions, {} branches, {} avoids, {} targets",
                interceptions.size(), branches.size(), avoids.size(), targets.size());

        logViolationPlan(interceptions, branches, avoids, targets);

        ViolationPlan plan = new ViolationPlan(interceptions, branches, avoids, targets);
        return WitnessModel.violation(WitnessModel.WitnessFormat.V2_YAML, plan, producer, programFile);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> collectWaypoints(Map<String, Object> itemMap) {
        // Structure variants:
        //   { segment: { waypoints: [...] } }
        //   { violation_sequence: [ { segment: { waypoints: [...] } }, ... ] }
        //   { waypoints: [...] }  (flattened)
        //   [ { waypoint: {...} }, ... ]  (list of single-waypoint maps)

        if (itemMap.containsKey("waypoints")) {
            Object wp = itemMap.get("waypoints");
            return wp instanceof List<?> ? (List<Object>) wp : List.of();
        }

        if (itemMap.containsKey("segment")) {
            Object seg = itemMap.get("segment");
            if (seg instanceof Map<?, ?> segMap) {
                return collectWaypoints((Map<String, Object>) segMap);
            }
            if (seg instanceof List<?> segList) {
                // Segment is itself a list of waypoint maps
                return (List<Object>) segList;
            }
        }

        if (itemMap.containsKey("violation_sequence")) {
            Object vs = itemMap.get("violation_sequence");
            List<Object> all = new ArrayList<>();
            if (vs instanceof List<?> vsList) {
                for (Object vsItem : vsList) {
                    if (vsItem instanceof Map<?, ?> vsMap) {
                        all.addAll(collectWaypoints((Map<String, Object>) vsMap));
                    }
                }
            }
            return all;
        }

        // Item itself may be a waypoint map
        if (itemMap.containsKey("type")) {
            return List.of(itemMap);
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static void processWaypoint(
            Map<String, Object> wpMap,
            List<Interception> interceptions,
            List<BranchDecision> branches,
            List<AvoidPoint> avoids,
            List<TargetPoint> targets) {

        // Single-waypoint maps: { waypoint: {...} }
        Object inner = wpMap.get("waypoint");
        if (inner instanceof Map<?, ?> innerMap) {
            wpMap = (Map<String, Object>) innerMap;
        }

        String type = asString(wpMap.get("type"));
        Map<String, Object> location = asMap(wpMap.get("location"));
        if (type == null || location == null) {
            return;
        }

        String fileName = asString(location.get("file_name"));
        int line = asInt(location.get("line"), -1);
        if (line < 0) {
            return;
        }

        switch (type) {
            case "assumption" -> {
                String value = asString(wpMap.get("value"));
                if (value != null) {
                    // Extract variable name and concrete value from "x == 42" or "x = 42"
                    String[] parts = splitAssumption(value);
                    if (parts != null) {
                        // For now, record as a named variable assumption; the EventLoop will
                        // use VariableSteerer.setValue() at the breakpoint.
                        // If the variable name suggests a nondet call we would use forceEarlyReturn,
                        // but in v2 the location is sufficient — we set a breakpoint and assign.
                        LOG.debug("[PLAN] Assumption: {}:{} → {} = {}", fileName, line, parts[0], parts[1]);
                        // Record as a zero-count interception to trigger StackFrame.setValue
                        interceptions.add(new Interception(
                                fileName, // used as file marker (not a class name)
                                parts[0], // variable name as "method name" slot
                                "", // no descriptor — VariableSteerer path
                                parts[1], // concrete value
                                1));
                    }
                }
            }
            case "branching" -> {
                String value = asString(wpMap.get("value"));
                boolean direction = "true".equalsIgnoreCase(value);
                branches.add(new BranchDecision(fileName, line, direction));
                LOG.debug("[PLAN] BranchDecision: {}:{} → {}", fileName, line, direction);
            }
            case "target" -> {
                targets.add(new TargetPoint(fileName, line));
                LOG.debug("[PLAN] Target: {}:{}", fileName, line);
            }
            case "avoid" -> {
                avoids.add(new AvoidPoint(fileName, line));
                LOG.debug("[PLAN] Avoid: {}:{}", fileName, line);
            }
            case "function_enter", "function_return" -> {
                // Informational — no action needed; the JVM enters/returns naturally.
            }
            default -> LOG.debug("[PARSE] Unknown waypoint type '{}' — skipped", type);
        }
    }

    // ------------------------------------------------------------------
    // correctness parsing
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static WitnessModel parseCorrectness(
            List<Object> content, String producer, String programFile) {

        List<CorrectnessInvariant> invariants = new ArrayList<>();

        for (Object item : content) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                continue;
            }
            // { invariant: {...} }  or  { invariant_set: [ { invariant: {...} }, ... ] }
            Object invObj = itemMap.get("invariant");
            if (invObj instanceof Map<?, ?> invMap) {
                parseInvariant((Map<String, Object>) invMap).ifPresent(invariants::add);
                continue;
            }
            Object invSetObj = itemMap.get("invariant_set");
            if (invSetObj instanceof List<?> invSetList) {
                for (Object inv : invSetList) {
                    if (inv instanceof Map<?, ?> invMap2) {
                        Object nestedInv = ((Map<?, ?>) invMap2).get("invariant");
                        if (nestedInv instanceof Map<?, ?> nestedMap) {
                            parseInvariant((Map<String, Object>) nestedMap).ifPresent(invariants::add);
                        } else {
                            parseInvariant((Map<String, Object>) invMap2).ifPresent(invariants::add);
                        }
                    }
                }
            }
        }

        LOG.debug("[PARSE] v2 correctness: {} invariants", invariants.size());
        for (int i = 0; i < invariants.size(); i++) {
            LOG.info("[PLAN] Invariant #{}: {}", i + 1, invariants.get(i));
        }

        return WitnessModel.correctness(WitnessModel.WitnessFormat.V2_YAML, invariants, producer, programFile);
    }

    @SuppressWarnings("unchecked")
    private static java.util.Optional<CorrectnessInvariant> parseInvariant(Map<String, Object> invMap) {
        String typeStr = asString(invMap.get("type"));
        Map<String, Object> location = asMap(invMap.get("location"));
        String format = asString(invMap.get("format"));
        String value = asString(invMap.get("value"));

        if (location == null || value == null) {
            LOG.warn("[PARSE] Invariant missing location or value — skipped");
            return java.util.Optional.empty();
        }

        String fileName = asString(location.get("file_name"));
        int line = asInt(location.get("line"), -1);
        String function = asString(location.get("function"));

        if (line < 0) {
            LOG.warn("[PARSE] Invariant at {} has no line — skipped", fileName);
            return java.util.Optional.empty();
        }

        InvariantType invType = "location_invariant".equalsIgnoreCase(typeStr)
                ? InvariantType.LOCATION_INVARIANT
                : InvariantType.LOOP_INVARIANT;

        return java.util.Optional.of(new CorrectnessInvariant(fileName, line, function, invType, format, value));
    }

    // ------------------------------------------------------------------
    // YAML loading & accessors
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(File file) throws ValidationException {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (FileReader reader = new FileReader(file)) {
            Object loaded = yaml.load(reader);
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new ValidationException("YAML witness root is not a mapping");
            }
            return (Map<String, Object>) map;
        } catch (IOException e) {
            throw new ValidationException("Failed to read YAML witness: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ValidationException("Failed to parse YAML witness: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractProducerName(Map<String, Object> root) {
        Object producer = root.get("producer");
        if (producer instanceof Map<?, ?> producerMap) {
            Object name = producerMap.get("name");
            return name != null ? name.toString() : null;
        }
        return producer != null ? producer.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private static String extractProgramFile(Map<String, Object> root) {
        Object task = root.get("task");
        if (task instanceof Map<?, ?> taskMap) {
            Object files = taskMap.get("input_files");
            if (files instanceof List<?> fileList && !fileList.isEmpty()) {
                return fileList.get(0).toString();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // small type-safe accessors
    // ------------------------------------------------------------------

    private static String asString(Object o) {
        return o != null ? o.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return null;
    }

    private static int asInt(Object o, int defaultVal) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }

    /**
     * Splits an assumption expression such as {@code "x == 42"} or {@code "x = 42"}
     * into {@code [varName, value]}.  Returns {@code null} if the string is not a simple
     * equality.
     */
    private static String[] splitAssumption(String expr) {
        int eqIdx = expr.indexOf("==");
        int assignIdx = eqIdx < 0 ? expr.indexOf('=') : -1;
        int splitIdx = eqIdx >= 0 ? eqIdx : assignIdx;
        int advance = eqIdx >= 0 ? 2 : 1;
        if (splitIdx < 0) {
            return null;
        }
        String varName = expr.substring(0, splitIdx).trim();
        String value = expr.substring(splitIdx + advance).trim();
        return new String[]{varName, value};
    }

    private static void logViolationPlan(
            List<Interception> interceptions,
            List<BranchDecision> branches,
            List<AvoidPoint> avoids,
            List<TargetPoint> targets) {
        for (int i = 0; i < interceptions.size(); i++) {
            LOG.info("[PLAN] Interception #{}: {}", i + 1, interceptions.get(i));
        }
        for (int i = 0; i < branches.size(); i++) {
            LOG.info("[PLAN] BranchDecision #{}: {}", i + 1, branches.get(i));
        }
        for (int i = 0; i < avoids.size(); i++) {
            LOG.info("[PLAN] AvoidPoint #{}: {}", i + 1, avoids.get(i));
        }
        for (int i = 0; i < targets.size(); i++) {
            LOG.info("[PLAN] Target #{}: {}", i + 1, targets.get(i));
        }
    }
}

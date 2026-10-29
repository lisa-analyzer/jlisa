package it.unive.jlisa.witness.validator.parser;

import it.unive.jlisa.witness.validator.RunSummary;
import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.filter.JvmScopeParser;
import it.unive.jlisa.witness.validator.filter.JvmScopeParser.ParsedScope;
import it.unive.jlisa.witness.validator.filter.ValueExtractor;
import it.unive.jlisa.witness.validator.filter.WitnessFilter;
import it.unive.jlisa.witness.validator.model.Interception;
import it.unive.jlisa.witness.validator.model.TargetPoint;
import it.unive.jlisa.witness.validator.model.ViolationPlan;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Parses format v1.0 GraphML violation witnesses into a {@link WitnessModel}.
 *
 * <p>The automaton structure is intentionally ignored — we care only about:
 * <ul>
 *   <li>Graph-level metadata: {@code witness-type}, {@code producer}, {@code programfile}</li>
 *   <li>Assumption edges: pairs of ({@code assumption}, {@code assumption.scope}) encoding
 *       return values of nondeterministic calls</li>
 *   <li>Violation nodes: nodes flagged {@code violation=true} to derive the target location</li>
 * </ul>
 *
 * <p>Correctness witnesses in GraphML format are not supported; use YAML format v2.0 instead.
 */
public final class WitnessParserV1 {

    private static final Logger LOG = LogManager.getLogger(WitnessParserV1.class);

    private WitnessParserV1() {
    }

    /**
     * Parses the given GraphML file into a {@link WitnessModel}.
     *
     * @param witnessFile the {@code .graphml} witness file
     * @return the parsed model
     * @throws ValidationException if the file cannot be parsed or is of an unsupported type
     */
    public static WitnessModel parse(File witnessFile) throws ValidationException {
        Document doc = loadDocument(witnessFile);
        Map<String, String> keyMap = buildKeyMap(doc);
        Element graph = findGraph(doc);

        String witnessType = readElementData(graph, "witness-type", keyMap);
        String producer = readElementData(graph, "producer", keyMap);
        String programFile = readElementData(graph, "programfile", keyMap);

        LOG.debug("[PARSE] witness-type={} producer={} programfile={}", witnessType, producer, programFile);

        if ("correctness_witness".equals(witnessType)) {
            throw new ValidationException(
                    "GraphML (v1) correctness witnesses are not supported. Use YAML format v2.0.");
        }

        // Collect violation-node IDs for target detection
        List<String> violationNodeIds = collectViolationNodeIds(graph, keyMap);

        // First pass: accumulate interception counts so that repeated nondet calls
        // (e.g. getBoolean() called 12 times, all returning true) get the correct count.
        // Key = className|methodName|descriptor|value
        List<Interception> interceptions = new ArrayList<>();
        //Map<String, Integer> interceptCounts = new LinkedHashMap<>();
        //Map<String, String[]> interceptParts = new LinkedHashMap<>(); // key → [className, methodName, desc, value]
        List<TargetPoint> targets = new ArrayList<>();
        int droppedEdges = 0;
        // steps with a value that cannot be used (noise edges are dropped on purpose and not counted)
        int skippedSteps = 0;

        NodeList edges = graph.getElementsByTagName("edge");
        for (int i = 0; i < edges.getLength(); i++) {
            Element edge = (Element) edges.item(i);
            if (edge.getParentNode() != graph) {
                continue;
            }

            String assumption = readElementData(edge, "assumption", keyMap);
            String scope = readElementData(edge, "assumption.scope", keyMap);
            String startLine = readElementData(edge, "startline", keyMap);
            String originFile = readElementData(edge, "originfile", keyMap);
            String targetNodeId = edge.getAttribute("target");

            // Derive target points from edges that lead to violation nodes
            if (violationNodeIds.contains(targetNodeId)
                    && startLine != null && !startLine.isBlank()) {
                String file = firstNonNull(originFile, programFile, witnessFile.getName());
                try {
                    targets.add(new TargetPoint(file, Integer.parseInt(startLine.trim())));
                    LOG.debug("[PARSE] Target derived from violation-node edge: {}:{}", file, startLine.trim());
                } catch (NumberFormatException e) {
                    LOG.warn("[PARSE] Unparseable startline '{}' on violation-target edge", startLine);
                }
            }


            if (assumption == null || assumption.isBlank()) {
                continue;
            }
            if (scope == null || scope.isBlank()) {
                continue;
            }

            if (!WitnessFilter.isInteresting(scope)) {
                droppedEdges++;
                RunSummary.count("edges.noise");
                LOG.debug("[FILTER] Noise edge dropped: scope={}", scope);
                continue;
            }

            Optional<ParsedScope> parsedScope = JvmScopeParser.parse(scope);
            if (parsedScope.isEmpty()) {
                droppedEdges++;
                skippedSteps++;
                RunSummary.count("edges.unparseable-scope");
                LOG.warn("[FILTER] Unparseable scope '{}' — edge skipped", scope);
                continue;
            }

            Optional<ValueExtractor.Extracted> extracted = ValueExtractor.extract(assumption);
            if (extracted.isEmpty()) {
                droppedEdges++;
                skippedSteps++;
                RunSummary.count("edges.no-value");
                LOG.warn("[FILTER] No value extracted from assumption '{}' — edge skipped", assumption);
                continue;
            }

            ParsedScope ps = parsedScope.get();
            String value = extracted.get().value();

            if (ps.isVerifierNondet()) {
                // Accumulate count for this (class, method, descriptor, value) tuple
                String key = ps.className() + "|" + ps.methodName() + "|" + ps.descriptor();// + "|" + value; fixme
                interceptions.add(
                        new Interception(ps.className(), ps.methodName(), ps.descriptor(), value, 1)
                );
                //interceptCounts.merge(key, 1, Integer::sum);
                //interceptParts.putIfAbsent(key, new String[]{ps.className(), ps.methodName(), ps.descriptor(), value});
                LOG.debug("[PLAN] Accumulate interception: {}.{}  → return {}", ps.className(), ps.methodName(),
                        value);
            } else {
                // Application-code scope: downstream variable assignments are consequences of
                // Verifier.nondet* interceptions and happen automatically inside the JVM.
                RunSummary.count("edges.app-assumption");
                LOG.debug("[PLAN] Application-code assumption (not steered): {}.{}  → {}",
                        ps.className(), ps.methodName(), extracted.get().varName() + "=" + value);
            }
        }

        // Build interception list with correct counts
		/*

		for (Map.Entry<String, Integer> entry : interceptCounts.entrySet()) {
			String[] parts = interceptParts.get(entry.getKey());
			int count = entry.getValue();
			interceptions.add(new Interception(parts[0], parts[1], parts[2], parts[3], count));
		}
		 */

        LOG.debug("[PARSE] Loaded v1 witness: {} interception type(s), {} total fires, {} targets, {} dropped",
                interceptions.size(),
                interceptions.stream().mapToInt(Interception::count).sum(),
                targets.size(),
                droppedEdges);

        // Log the final plan
        for (int i = 0; i < interceptions.size(); i++) {
            LOG.info("[PLAN] Interception #{}: {}", i + 1, interceptions.get(i));
        }
        for (int i = 0; i < targets.size(); i++) {
            LOG.info("[PLAN] Target #{}: {}", i + 1, targets.get(i));
        }

        ViolationPlan plan = new ViolationPlan(
                interceptions,
                List.of(), // branch decisions — not in v1 format
                List.of(), // avoid points — not in v1 format
                targets,
                skippedSteps);

        return WitnessModel.violation(WitnessModel.WitnessFormat.V1_GRAPHML, plan, producer, programFile);
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    private static Document loadDocument(File file) throws ValidationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            // Disable external entity resolution (XXE prevention)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(file);
        } catch (Exception e) {
            throw new ValidationException("Failed to parse GraphML file: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a map from key element {@code id} attributes to their semantic
     * {@code attr.name} values.  When a key's {@code id} already is the semantic name
     * (common in many tools), the mapping is identity.
     */
    private static Map<String, String> buildKeyMap(Document doc) {
        Map<String, String> map = new HashMap<>();
        NodeList keys = doc.getElementsByTagName("key");
        for (int i = 0; i < keys.getLength(); i++) {
            Element key = (Element) keys.item(i);
            String id = key.getAttribute("id");
            String name = key.getAttribute("attr.name");
            if (!id.isBlank()) {
                map.put(id, name.isBlank() ? id : name);
            }
        }
        return map;
    }

    private static Element findGraph(Document doc) throws ValidationException {
        NodeList graphs = doc.getElementsByTagName("graph");
        if (graphs.getLength() == 0) {
            throw new ValidationException("GraphML witness contains no <graph> element");
        }
        return (Element) graphs.item(0);
    }

    /**
     * Reads the text content of the first {@code <data key="...">} direct child of
     * {@code parent} whose resolved key name equals {@code attrName}.
     */
    private static String readElementData(Element parent, String attrName, Map<String, String> keyMap) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element child)) {
                continue;
            }
            if (!"data".equals(child.getTagName())) {
                continue;
            }
            String key = child.getAttribute("key");
            String resolvedName = keyMap.getOrDefault(key, key);
            if (attrName.equals(resolvedName) || attrName.equals(key)) {
                return child.getTextContent();
            }
        }
        return null;
    }

    private static List<String> collectViolationNodeIds(Element graph, Map<String, String> keyMap) {
        List<String> ids = new ArrayList<>();
        NodeList nodes = graph.getElementsByTagName("node");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if ("true".equals(readElementData(node, "violation", keyMap))) {
                ids.add(node.getAttribute("id"));
            }
        }
        return ids;
    }

    private static String firstNonNull(String... candidates) {
        for (String s : candidates) {
            if (s != null && !s.isBlank()) {
                return s;
            }
        }
        return "unknown";
    }
}

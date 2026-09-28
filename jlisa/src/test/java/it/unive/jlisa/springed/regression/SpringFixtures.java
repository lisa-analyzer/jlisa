package it.unive.jlisa.springed.regression;

import it.unive.jlisa.springed.SpringTestCases;
import it.unive.jlisa.springed.frontend.SpringFrontend;
import it.unive.jlisa.springed.p1.NewP1Impl;
import it.unive.jlisa.springed.p1.constructs.RequestMapping;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeMember;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Parsing fixtures shared by the springed merge-regression tests. Everything is
 * extracted or written under a caller-provided (temporary) directory, so the
 * working tree is never touched.
 */
final class SpringFixtures {

	static final List<String> CASES = List.of("case-1", "case-2", "case-3", "case-4");

	private SpringFixtures() {
	}

	/**
	 * The outcome of running {@link SpringFrontend} over a source root.
	 */
	record Parsed(
			Path root,
			SpringFrontend frontend,
			Unit[] units) {
	}

	/**
	 * Extracts the {@code caseName} sample project into {@code workDir} and
	 * parses its {@code src/main/java}, as {@code Main} does.
	 */
	static Parsed parseCase(
			String caseName,
			Path workDir)
			throws IOException {
		Path project = SpringTestCases.extract(caseName, workDir);
		return parse(workDir, project.resolve("src/main/java"));
	}

	/**
	 * Writes {@code sources} (path relative to the source root to content)
	 * under {@code root/src/main/java} and parses them.
	 */
	static Parsed parseSources(
			Path root,
			Map<String, String> sources)
			throws IOException {
		Path sourceRoot = root.resolve("src/main/java");
		for (Map.Entry<String, String> source : sources.entrySet()) {
			Path file = sourceRoot.resolve(source.getKey());
			Files.createDirectories(file.getParent());
			Files.writeString(file, source.getValue());
		}
		return parse(root, sourceRoot);
	}

	private static Parsed parse(
			Path root,
			Path sourceRoot)
			throws IOException {
		SpringFrontend frontend = new SpringFrontend();
		Unit[] units = frontend.parse(sourceRoot.toString());
		return new Parsed(root, frontend, units);
	}

	/**
	 * The {@link NewP1Impl} mapping of every controller method that has one,
	 * keyed by {@code SimpleClassName::method}.
	 */
	static Map<String, RequestMapping> newP1Mappings(
			Unit[] units) {
		NewP1Impl p1 = new NewP1Impl();
		Map<String, RequestMapping> mappings = new TreeMap<>();
		for (ClassUnit controller : p1.getControllers(units))
			for (CodeMember method : controller.getInstanceCodeMembers(false)) {
				RequestMapping mapping = p1.getMappingForMethod(controller, method);
				if (mapping != null)
					mappings.put(key(controller, method), mapping);
			}
		return mappings;
	}

	/**
	 * Renders the HTTP methods and paths of a mapping, e.g.
	 * {@code GET,POST /a,/b}; an empty set is rendered as {@code -}.
	 */
	static String summary(
			RequestMapping mapping) {
		return join(mapping.getMethods()) + " " + join(mapping.getPaths());
	}

	static String key(
			ClassUnit controller,
			CodeMember method) {
		String name = controller.getName();
		return name.substring(name.lastIndexOf('.') + 1) + "::" + method.getDescriptor().getName();
	}

	private static String join(
			Set<String> values) {
		return values.isEmpty() ? "-" : String.join(",", new TreeSet<>(values));
	}
}

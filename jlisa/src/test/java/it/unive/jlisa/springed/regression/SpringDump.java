package it.unive.jlisa.springed.regression;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.unive.jlisa.program.libraries.loader.extensions.CompileTimeGlobal;
import it.unive.jlisa.springed.exceptions.SpringCSVExceptionWriter;
import it.unive.jlisa.springed.frontend.SpringFrontend;
import it.unive.jlisa.springed.p1.NewP1Impl;
import it.unive.jlisa.springed.p1.P1Impl;
import it.unive.jlisa.springed.p1.constructs.Mapping;
import it.unive.jlisa.springed.p1.constructs.Registry;
import it.unive.jlisa.springed.p1.constructs.RequestMapping;
import it.unive.jlisa.springed.p1.constructs.WebAnnotation;
import it.unive.jlisa.springed.p1.output.P1Output;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.values.AnnotationValue;
import it.unive.lisa.program.annotations.values.ArrayAnnotationValue;
import it.unive.lisa.program.annotations.values.EnumAnnotationValue;
import it.unive.lisa.program.annotations.values.StringAnnotationValue;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Deterministic, human-readable renderings of what springed produces, used as
 * golden-file snapshots. Everything is sorted, because the frontend visits
 * files in filesystem order. Annotations are rendered here rather than through
 * LiSA's {@code toString()}, whose format changes between LiSA snapshots.
 */
final class SpringDump {

	/** Package prefix shared by all the sample projects. */
	static final String CASES_PACKAGE = "it.unive.jlisa.jlisa.testcases.";

	/** Package prefix of the {@code spring-libraries} stubs. */
	static final String SPRING_PACKAGE = "org.springframework.";

	/** The configuration of {@code it.unive.jlisa.springed.Main}'s mapper. */
	static final ObjectMapper MAIN_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	private SpringDump() {
	}

	/**
	 * Renders the units, annotations, globals and code members built by the
	 * frontend for the program units under {@code unitPrefix}, plus the
	 * spring-libraries stubs that got imported.
	 */
	static String frontendModel(
			String title,
			SpringFrontend frontend,
			Unit[] returned,
			String unitPrefix) {
		StringBuilder sb = new StringBuilder("# SpringFrontend model: ").append(title).append('\n');

		sb.append("\n## units returned by SpringFrontend.parse\n");
		Arrays.stream(returned)
				.map(unit -> unit == null ? "<null>" : unit.getName())
				.sorted()
				.forEach(name -> sb.append(name).append('\n'));

		sb.append("\n## program units under ").append(unitPrefix).append('\n');
		for (Unit unit : units(frontend.getProgram(), unitPrefix))
			appendUnit(sb, unit);

		sb.append("\n## spring-libraries stubs in the program\n");
		List<Unit> stubs = units(frontend.getProgram(), SPRING_PACKAGE);
		if (stubs.isEmpty())
			sb.append("-\n");
		for (Unit stub : stubs)
			appendStub(sb, stub);

		return sb.toString();
	}

	/**
	 * Renders what the P1 implementations extract from {@code units}.
	 */
	static String p1(
			String projectName,
			Unit[] units) {
		StringBuilder sb = new StringBuilder("# P1 outputs: ").append(projectName).append('\n');

		NewP1Impl newP1 = new NewP1Impl();
		List<ClassUnit> controllers = byName(newP1.getControllers(units));

		sb.append("\n## NewP1Impl.getControllers\n");
		appendNames(sb, controllers);

		sb.append("\n## NewP1Impl.getMappingForMethod\n");
		for (ClassUnit controller : controllers)
			for (CodeMember method : bySignature(controller.getInstanceCodeMembers(false)))
				sb.append(signature(method)).append("\n  ")
						.append(outcome(() -> mapping(newP1.getMappingForMethod(controller, method))))
						.append('\n');

		sb.append("\n## NewP1Impl.p1\n");
		sb.append(outcome(() -> {
			List<RequestMapping> all = newP1.p1(units);
			return all.size() + " mapping(s)\n" + all.stream()
					.map(SpringDump::mapping)
					.sorted()
					.collect(Collectors.joining("\n"));
		})).append('\n');

		P1Impl legacy = new P1Impl();
		List<ClassUnit> legacyControllers = byName(legacy.getControllerClasses(units));

		sb.append("\n## P1Impl.getControllerClasses\n");
		appendNames(sb, legacyControllers);

		sb.append("\n## P1Impl.extractAnnotation\n");
		for (ClassUnit controller : legacyControllers)
			for (CodeMember method : bySignature(controller.getInstanceCodeMembers(false)))
				sb.append(signature(method)).append("\n  ")
						.append(outcome(() -> webAnnotation(legacy.extractAnnotation(method))))
						.append('\n');

		sb.append("\n## P1Impl.produceRegistry as JSON (Main's ObjectMapper, mappings sorted)\n");
		sb.append(outcome(() -> registryJson(projectName, legacy.produceRegistry(units)))).append('\n');

		return sb.toString();
	}

	/**
	 * Serializes {@code registry} as {@code Main} does, with its mappings
	 * sorted to make the output independent of the parsing order.
	 */
	static String registryJson(
			String projectName,
			Registry registry)
			throws IOException {
		registry.getMappings().sort(Comparator.comparing(Mapping::getJsonFieldName)
				.thenComparing(Mapping::getMethodName));
		P1Output output = new P1Output();
		output.addRegistry(projectName, registry);
		return MAIN_MAPPER.writeValueAsString(output);
	}

	/**
	 * Writes {@code errors} through {@link SpringCSVExceptionWriter} into
	 * {@code csv} and returns its content: the header first, then the rows,
	 * normalized (see {@link #normalize(String, Path)}) and sorted.
	 */
	static String parseErrors(
			List<Throwable> errors,
			Path csv,
			Path root)
			throws IOException {
		SpringCSVExceptionWriter.writeCSV(csv.toString(), errors);
		List<String> lines = Files.readAllLines(csv);
		StringBuilder sb = new StringBuilder();
		if (!lines.isEmpty())
			sb.append(lines.getFirst()).append('\n');
		lines.stream()
				.skip(1)
				.map(line -> normalize(line, root))
				.sorted()
				.forEach(line -> sb.append(line).append('\n'));
		return sb.toString();
	}

	/**
	 * Replaces {@code root} (in any of its spellings) with {@code <root>} and
	 * the line numbers of stack-trace locations (e.g. {@code Foo.java:123}, as
	 * opposed to source locations such as {@code 'Foo.java':12:5}) with
	 * {@code <line>}, so that snapshots survive unrelated edits and temporary
	 * directories.
	 */
	static String normalize(
			String text,
			Path root) {
		String result = text;
		for (String spelling : spellings(root))
			result = result.replace(spelling, "<root>");
		return result.replaceAll("([A-Za-z0-9_$]+\\.java):\\d+", "$1:<line>");
	}

	static String mapping(
			RequestMapping mapping) {
		if (mapping == null)
			return "-";
		return "methods=" + sorted(mapping.getMethods())
				+ " paths=" + sorted(mapping.getPaths())
				+ " params=" + sorted(mapping.getParams())
				+ " headers=" + sorted(mapping.getHeaders())
				+ " consumes=" + sorted(mapping.getConsumes())
				+ " produces=" + sorted(mapping.getProduces())
				+ " version=" + sorted(mapping.getVersion());
	}

	static String webAnnotation(
			WebAnnotation annotation) {
		if (annotation == null)
			return "-";
		return annotation.getHttpMethod() + " " + annotation.getAddressPath()
				+ " params=" + annotation.getParams()
				+ " headers=" + annotation.getHeaders();
	}

	static String signature(
			CodeMember member) {
		String formals = Arrays.stream(member.getDescriptor().getFormals())
				.map(SpringDump::formal)
				.collect(Collectors.joining(", "));
		return (member.getDescriptor().isInstance() ? "instance " : "static ")
				+ member.getDescriptor().getFullName() + "(" + formals + ")";
	}

	/**
	 * Renders annotations as {@code @Name(id = value, ...)}, sorted; {@code -}
	 * when there are none.
	 */
	static String annotations(
			Collection<Annotation> annotations) {
		if (annotations == null || annotations.isEmpty())
			return "-";
		return annotations.stream()
				.map(SpringDump::annotation)
				.sorted()
				.collect(Collectors.joining(" "));
	}

	static String annotation(
			Annotation annotation) {
		List<AnnotationMember> members = annotation.getAnnotationMembers();
		if (members == null || members.isEmpty())
			return "@" + annotation.getAnnotationName();
		return "@" + annotation.getAnnotationName() + members.stream()
				.map(member -> member.getId() + " = " + value(member.getValue()))
				.collect(Collectors.joining(", ", "(", ")"));
	}

	/**
	 * Renders an annotation value. String values are kept as stored, so a
	 * constant folded into a literal ({@code "text/plain"}, with quotes) stays
	 * distinguishable from an unresolved, symbolic one
	 * ({@code MediaType.TEXT_PLAIN_VALUE}).
	 */
	static String value(
			AnnotationValue value) {
		return switch (value) {
		case null -> "null";
		case ArrayAnnotationValue array -> array.getArray() == null ? "{}"
				: Arrays.stream(array.getArray())
						.map(SpringDump::value)
						.collect(Collectors.joining(", ", "{", "}"));
		case EnumAnnotationValue enumValue -> "enum(" + enumValue.getName() + "." + enumValue.getField() + ")";
		case StringAnnotationValue string -> string.toString();
		default -> value.getClass().getSimpleName().replace("AnnotationValue", "") + "(" + value + ")";
		};
	}

	private static void appendUnit(
			StringBuilder sb,
			Unit unit) {
		sb.append(unit.getClass().getSimpleName()).append(' ').append(unit.getName()).append('\n');
		if (unit instanceof CompilationUnit cu) {
			sb.append("  annotations: ").append(annotations(cu.getAnnotationList())).append('\n');
			sb.append("  ancestors: ").append(ancestors(cu)).append('\n');
		}

		List<String> globals = globals(unit).stream()
				.map(global -> (global.isInstance() ? "instance " : "static ") + global.getName()
						+ " annotations: " + annotations(global.getAnnotationList()))
				.sorted()
				.toList();
		sb.append("  globals:").append(globals.isEmpty() ? " -" : "").append('\n');
		globals.forEach(global -> sb.append("    ").append(global).append('\n'));

		List<CodeMember> members = new ArrayList<>(unit.getCodeMembers());
		if (unit instanceof CompilationUnit cu)
			members.addAll(cu.getInstanceCodeMembers(false));
		sb.append("  code members:").append(members.isEmpty() ? " -" : "").append('\n');
		for (CodeMember member : bySignature(members))
			sb.append("    ").append(signature(member)).append('\n')
					.append("      annotations: ").append(annotations(member.getDescriptor().getAnnotationList()))
					.append('\n');
		sb.append('\n');
	}

	private static void appendStub(
			StringBuilder sb,
			Unit stub) {
		sb.append(stub.getClass().getSimpleName()).append(' ').append(stub.getName()).append('\n');
		if (stub instanceof CompilationUnit cu)
			sb.append("  ancestors: ").append(ancestors(cu)).append('\n');
		globals(stub).stream()
				.map(global -> "  " + (global.isInstance() ? "instance " : "static ") + global.getName()
						+ defaultValue(global))
				.sorted()
				.forEach(line -> sb.append(line).append('\n'));
	}

	private static String defaultValue(
			Global global) {
		if (!(global instanceof CompileTimeGlobal constant) || constant.getDefaultValue() == null)
			return "";
		Object value = constant.getDefaultValue().getValue();
		return " = " + (value instanceof String ? "\"" + value + "\"" : String.valueOf(value));
	}

	private static List<Global> globals(
			Unit unit) {
		List<Global> globals = new ArrayList<>(unit.getGlobals());
		if (unit instanceof CompilationUnit cu)
			globals.addAll(cu.getInstanceGlobals(false));
		return globals;
	}

	private static String ancestors(
			CompilationUnit unit) {
		List<String> names = unit.getImmediateAncestors().stream()
				.map(Unit::getName)
				.sorted()
				.toList();
		return names.isEmpty() ? "-" : String.join(", ", names);
	}

	private static String formal(
			Parameter parameter) {
		Collection<Annotation> annotations = parameter.getAnnotationList();
		String prefix = annotations == null || annotations.isEmpty() ? "" : annotations(annotations) + " ";
		return prefix + parameter.getName();
	}

	private static List<Unit> units(
			Program program,
			String prefix) {
		return program.getUnits().stream()
				.filter(unit -> unit.getName().startsWith(prefix))
				.sorted(Comparator.comparing(Unit::getName))
				.toList();
	}

	private static List<ClassUnit> byName(
			List<ClassUnit> units) {
		return units.stream()
				.sorted(Comparator.comparing(Unit::getName))
				.toList();
	}

	private static List<CodeMember> bySignature(
			Collection<CodeMember> members) {
		return members.stream()
				.sorted(Comparator.comparing(SpringDump::signature))
				.toList();
	}

	private static void appendNames(
			StringBuilder sb,
			List<ClassUnit> units) {
		if (units.isEmpty())
			sb.append("-\n");
		units.forEach(unit -> sb.append(unit.getName()).append('\n'));
	}

	private static String sorted(
			Set<String> values) {
		return values == null ? "null" : new TreeSet<>(values).toString();
	}

	private static String outcome(
			Callable<String> computation) {
		try {
			return computation.call();
		} catch (Exception e) {
			String message = e.getMessage() == null ? "" : e.getMessage();
			int colon = message.indexOf(':');
			return "THROWS " + e.getClass().getName() + (message.isEmpty() ? ""
					: ": " + (colon < 0 ? message : message.substring(0, colon)));
		}
	}

	private static Set<String> spellings(
			Path root) {
		Set<String> spellings = new LinkedHashSet<>();
		try {
			spellings.add(root.toRealPath().toString());
		} catch (IOException e) {
			// the root might not exist (anymore): its absolute path is enough
		}
		spellings.add(root.toAbsolutePath().normalize().toString());
		// longest first, since one spelling may contain another
		// (e.g. /private/var/... and /var/... on macOS)
		return spellings.stream()
				.sorted(Comparator.comparing(String::length).reversed())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}

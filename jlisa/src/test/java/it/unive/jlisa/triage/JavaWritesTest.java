package it.unive.jlisa.triage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.triage.model.HeapWrites.Written;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JavaHeapWrites} and {@link JavaGlobalWrites}.
 */
public class JavaWritesTest {

	private static final Path TASKS = Paths.get("java-testcases", "triage");

	private static final Path COMMON = Paths.get("java-testcases", "svcomp", "common");

	private static Program parse(
			String task)
			throws IOException {
		List<String> files = new ArrayList<>();
		try (Stream<Path> common = Files.walk(COMMON)) {
			files.addAll(common.filter(p -> p.toString().endsWith(".java")).map(Path::toString)
					.collect(Collectors.toList()));
		}
		files.add(TASKS.resolve(task).resolve("Main.java").toString());
		return new JavaFrontend().parseFromListOfFile(files);
	}

	// the code member with the given name of the unit with the given name
	private static CodeMember member(
			Program program,
			String unitName,
			String name) {
		for (Unit unit : program.getUnits())
			if (unit.getName().equals(unitName)) {
				for (CodeMember member : unit.getCodeMembers())
					if (member.getDescriptor().getName().equals(name))
						return member;
				if (unit instanceof CompilationUnit)
					for (CodeMember member : ((CompilationUnit) unit).getInstanceCodeMembers(true))
						if (member.getDescriptor().getName().equals(name))
							return member;
			}
		throw new AssertionError("no member " + unitName + "." + name);
	}

	/**
	 * The natives write the fields of the library classes through unqualified
	 * names (e.g., the value of an atomic integer), also when a class of the
	 * program has a field with the same name.
	 */
	@Test
	public void nativesWriteTheirFieldsAlsoWhenProgramClassesShareTheirNames() throws IOException {
		Program program = parse("heap-native-fields");
		Written written = new JavaHeapWrites()
				.writtenBy(member(program, "java.util.concurrent.atomic.AtomicInteger", "set"));
		assertNotNull(written);
		assertTrue(written.writesField("value"));
		// also the fields that library classes do not declare
		assertTrue(written.writesField("capacity"));
		assertTrue(written.writesField("message"));
		// but not the fields of the class of the program
		assertFalse(written.writesField("Box::value"));
		assertFalse(written.writesField("Box::capacity"));
	}

	/**
	 * A static initializer writing only the static fields of its class is an
	 * initializer, while one writing the static fields of another class is not.
	 */
	@Test
	public void staticInitializersWritingOtherClassesAreNotInitializers() throws IOException {
		Program program = parse("static-initializer-writes");
		JavaGlobalWrites writes = new JavaGlobalWrites();
		assertTrue(writes.isInitializer(member(program, "A", "A_clinit")));
		assertFalse(writes.isInitializer(member(program, "B", "B_clinit")));
		// a method whose name ends with the suffix is not an initializer
		assertFalse(writes.isInitializer(member(program, "Main", "touch")));
	}

	/**
	 * A static initializer is an initializer only if the code it calls writes
	 * no static field of other classes: other static initializers that are
	 * initializers (e.g., the one of the superclass), and the libraries.
	 */
	@Test
	public void staticInitializersCallingWritersAreNotInitializers() throws IOException {
		Program program = parse("static-initializer-calls");
		JavaGlobalWrites writes = new JavaGlobalWrites();
		// Helper.bump writes Counter.n
		assertFalse(writes.isInitializer(member(program, "B", "B_clinit")));
		// Math.abs is a native
		assertTrue(writes.isInitializer(member(program, "C", "C_clinit")));
		// the initializers of the superclasses run first
		assertTrue(writes.isInitializer(member(program, "D", "D_clinit")));
		assertFalse(writes.isInitializer(member(program, "E", "E_clinit")));
	}

	/**
	 * The fields that the code of the frontend accesses by unqualified names
	 * (e.g., the natives writing the {@code value} of strings) are the ones
	 * that {@link JavaHeapWrites} deems written by natives: static globals
	 * (qualified by their class) and the internal fields of the models of the
	 * library (starting with {@code $}) are excluded.
	 */
	@Test
	public void nativeFieldsAreInSyncWithTheNatives() throws IOException {
		java.util.regex.Pattern field = java.util.regex.Pattern
				.compile("new GlobalVariable\\([^,()]+,\\s*\"([^\"]+)\"\\s*,");
		java.util.Set<String> names = new java.util.TreeSet<>();
		try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files
				.walk(java.nio.file.Paths.get("src", "main", "java", "it", "unive", "jlisa"))) {
			for (java.nio.file.Path file : (Iterable<java.nio.file.Path>) files
					.filter(f -> f.toString().endsWith(".java"))::iterator) {
				java.util.regex.Matcher m = field.matcher(java.nio.file.Files.readString(file));
				while (m.find())
					if (!m.group(1).contains("::") && !m.group(1).startsWith("$"))
						names.add(m.group(1));
			}
		}
		assertFalse(names.isEmpty());
		java.util.Set<String> missing = new java.util.TreeSet<>(names);
		missing.removeAll(JavaHeapWrites.NATIVE_FIELDS);
		assertTrue(missing.isEmpty(), "fields written by natives missing from NATIVE_FIELDS: " + missing);
	}
}

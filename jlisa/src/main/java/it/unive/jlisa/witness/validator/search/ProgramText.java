package it.unive.jlisa.witness.validator.search;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The task's source lines, as the input search reads them: which lines are
 * worth watching (comparisons, assertions, string tests) and what a variable is
 * compared with on a line. Everything comes from the program being validated;
 * nothing is specific to any task.
 */
final class ProgramText {

	private static final Pattern WATCHED = Pattern.compile(
			"\\bassert\\b|==|!=|<=|>=|[^<>-]<[^<=]|[^<>-]>[^>=]"
					+ "|\\.(equals|contains|startsWith|endsWith|equalsIgnoreCase|compareTo|matches)\\(");
	private static final String LIT = "('(?:\\\\.|[^'\\\\])'|\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?[lLfFdD]?|true|false)";
	private static final Pattern LITERAL = Pattern.compile("(?<![\\w.])" + LIT);
	private static final String STRING_TESTS = "(equals|equalsIgnoreCase|contains|startsWith|endsWith|compareTo)";

	/**
	 * Lines by file name (as JDI reports it in {@code Location.sourceName()}).
	 */
	private final Map<String, List<String>> lines = new HashMap<>();
	private final Map<String, List<Integer>> watched = new HashMap<>();

	ProgramText(
			Collection<File> dirs)
			throws IOException {
		for (File d : dirs) {
			if (d == null || !d.isDirectory()) {
				continue;
			}
			try (Stream<Path> s = Files.walk(d.toPath())) {
				for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java"))::iterator) {
					if (p.endsWith(Path.of("sv_benchmarks", "Verifier.java"))) {
						continue;
					}
					List<String> text = Files.readAllLines(p, StandardCharsets.ISO_8859_1);
					String name = p.getFileName().toString();
					lines.put(name, text);
					List<Integer> w = new ArrayList<>();
					for (int i = 0; i < text.size(); i++) {
						if (WATCHED.matcher(" " + code(text.get(i)) + " ").find()) {
							w.add(i + 1);
						}
					}
					watched.put(name, w);
				}
			}
		}
	}

	List<Integer> watchedLines(
			String file) {
		return watched.getOrDefault(file, List.of());
	}

	boolean isAssert(
			String file,
			int line) {
		return code(line(file, line)).matches(".*\\bassert\\b.*");
	}

	String line(
			String file,
			int line) {
		List<String> text = lines.get(file);
		return text == null || line < 1 || line > text.size() ? "" : text.get(line - 1);
	}

	private static String code(
			String line) {
		int i = line.indexOf("//");
		return i >= 0 ? line.substring(0, i) : line;
	}

	/**
	 * Adds to {@code out} the values that variable {@code name} is compared
	 * with on this line, most specific first: {@code name == 1000} gives 1000;
	 * {@code name < 7} gives 7, 8, 6; {@code name.contains("x")} gives "x";
	 * {@code name == w} gives w's value. Then every literal of the line (and
	 * its neighbours), for comparisons the patterns above do not cover.
	 */
	static void counterparts(
			String line,
			String name,
			Map<String, Val> vars,
			LinkedHashSet<Val> out) {
		String code = code(line);
		String n = Pattern.quote(name);
		String operand = "(" + LIT + "|[A-Za-z_]\\w*)";
		Matcher m = Pattern.compile("\\b" + n + "\\b\\s*(==|!=|<=|>=|<|>)\\s*" + operand).matcher(code);
		while (m.find()) {
			add(out, m.group(1), m.group(2), vars);
		}
		m = Pattern.compile(operand + "\\s*(==|!=|<=|>=|<|>)\\s*\\b" + n + "\\b").matcher(code);
		while (m.find()) {
			add(out, m.group(3), m.group(1), vars);
		}
		m = Pattern.compile("\\b" + n + "\\s*\\.\\s*" + STRING_TESTS + "\\s*\\(\\s*" + operand).matcher(code);
		while (m.find()) {
			add(out, "==", m.group(2), vars);
		}
		m = Pattern.compile(operand + "\\s*\\.\\s*" + STRING_TESTS + "\\s*\\(\\s*\\b" + n + "\\b").matcher(code);
		while (m.find()) {
			add(out, "==", m.group(1), vars);
		}
		m = LITERAL.matcher(code);
		while (m.find()) {
			add(out, "~", m.group(1), vars);
		}
	}

	private static void add(
			LinkedHashSet<Val> out,
			String op,
			String operand,
			Map<String, Val> vars) {
		Val v;
		try {
			if (operand.matches(LIT)) {
				v = Val.literal(operand);
			} else if (vars.containsKey(operand)) {
				v = vars.get(operand);
			} else {
				return;
			}
		} catch (NumberFormatException e) {
			return;
		}
		out.add(v);
		if (v.num() != null && !op.equals("==")) {
			out.add(Val.of(v.num() + 1));
			out.add(Val.of(v.num() - 1));
		}
	}
}

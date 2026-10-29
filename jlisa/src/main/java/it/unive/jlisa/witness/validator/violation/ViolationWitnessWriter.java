package it.unive.jlisa.witness.validator.violation;

import it.unive.jlisa.svcomp.SVCOMPProperty;
import it.unive.jlisa.svcomp.SVCOMPWitnesses;
import it.unive.jlisa.witness.validator.model.Property;
import it.unive.jlisa.witness.validator.search.Trace;
import it.unive.lisa.witness.v2.Location;
import it.unive.lisa.witness.v2.ViolationWitness;
import it.unive.lisa.witness.v2.ViolationWitnessBuilder;
import it.unive.lisa.witness.v2.WitnessWriter;
import it.unive.lisa.witness.v2.metadata.Task;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * The violation witness of a run found by the bug-finding mode, written as
 * jlisa's (format 2.x): one {@code function_return} waypoint for each
 * {@code Verifier.nondet*()} call of the run, in order, with the value it
 * returned, then the target. The metadata are those of jlisa's own witnesses,
 * with the description {@value #DESCRIPTION}.
 */
public final class ViolationWitnessWriter {

	public static final String DESCRIPTION = "Found with bug-finding mode";

	/**
	 * Where to write the witness, and the task as given to the tool (the files
	 * of the witness are those of the task, not the copies the program runs
	 * from).
	 *
	 * @param output       the witness file to write
	 * @param inputs       the input files and directories of the task
	 * @param propertyFile the property file of the task
	 */
	public record Request(
			Path output,
			List<String> inputs,
			String propertyFile) {
	}

	private ViolationWitnessWriter() {
	}

	/**
	 * Writes the witness of the trace.
	 *
	 * @return null if it is written, else why there is no witness
	 */
	static String write(
			Request request,
			Trace trace,
			Property property,
			WitnessLocations locations) {
		Task task;
		try {
			task = SVCOMPWitnesses.task(request.inputs(), request.propertyFile());
		} catch (IOException | RuntimeException e) {
			return "the task cannot be loaded (" + e.getMessage() + ")";
		}
		SVCOMPProperty taskProperty = SVCOMPProperty.of(task.getSpecification());
		if (taskProperty != (property == Property.ASSERT ? SVCOMPProperty.ASSERT : SVCOMPProperty.RUNTIME_EXCEPTION)) {
			return "the property file is not the one of " + property;
		}
		try {
			ViolationWitnessBuilder builder = new ViolationWitnessBuilder(task, SVCOMPWitnesses.producer(DESCRIPTION));
			for (Trace.Call call : trace.calls()) {
				String value = constraint(call.value());
				if (value == null) {
					return "no constraint for the value " + call.value() + " of " + call.method();
				}
				Location at = location(task, locations.call(trace, call));
				if (at == null) {
					return "no location for the call of " + call.method() + " at " + call.caller().sourcePath()
							+ ":" + call.caller().line();
				}
				builder.functionReturn(at, value);
			}
			Location target = location(task, locations.target(trace, property));
			if (target == null) {
				return "no location for the target";
			}
			ViolationWitness witness = builder.target(target).build();
			if (request.output().getParent() != null) {
				Files.createDirectories(request.output().getParent());
			}
			WitnessWriter.write(witness, request.output());
		} catch (IOException | RuntimeException e) {
			// InvalidWitnessException included
			return "the witness cannot be written (" + e.getMessage() + ")";
		}
		return null;
	}

	private static Location location(
			Task task,
			WitnessLocations.Point point) {
		if (point == null) {
			return null;
		}
		String file = SVCOMPWitnesses.inputFile(task, Paths.get(point.sourcePath()).getFileName().toString());
		return file == null ? null : new Location(file, point.line(), point.column());
	}

	/**
	 * The constraint on {@code \result} of a call that returned the given
	 * value, a Java expression (e.g. {@code \result == 11L},
	 * {@code \result == 'B'}, {@code \result.equals("abc")}), or null for a
	 * value of another type.
	 */
	static String constraint(
			Object value) {
		return switch (value) {
		case Integer i -> "\\result == " + i;
		case Short s -> "\\result == " + s;
		case Byte b -> "\\result == " + b;
		case Long l -> "\\result == " + l + "L";
		case Boolean b -> "\\result == " + b;
		case Character c -> "\\result == '" + escape(c, '\'') + "'";
		case Float f when f.isNaN() -> "\\result != \\result";
		case Float f when f.isInfinite() -> "\\result == Float." + (f > 0 ? "POSITIVE" : "NEGATIVE") + "_INFINITY";
		case Float f -> "\\result == " + f + "f";
		case Double d when d.isNaN() -> "\\result != \\result";
		case Double d when d.isInfinite() -> "\\result == Double." + (d > 0 ? "POSITIVE" : "NEGATIVE") + "_INFINITY";
		case Double d -> "\\result == " + d;
		case String s -> {
			StringBuilder text = new StringBuilder();
			for (char c : s.toCharArray()) {
				text.append(escape(c, '"'));
			}
			yield "\\result.equals(\"" + text + "\")";
		}
		case null, default -> null;
		};
	}

	/**
	 * The character as written in a Java literal delimited by {@code quote}.
	 * Line breaks, quotes and backslashes use their escape sequences (a Unicode
	 * escape of them would end the literal), other characters outside printable
	 * ASCII a Unicode escape.
	 */
	private static String escape(
			char c,
			char quote) {
		return switch (c) {
		case '\b' -> "\\b";
		case '\t' -> "\\t";
		case '\n' -> "\\n";
		case '\f' -> "\\f";
		case '\r' -> "\\r";
		case '\\' -> "\\\\";
		default -> c == quote ? "\\" + c
				: c < 0x20 || c >= 0x7f ? String.format("\\u%04x", (int) c)
						: String.valueOf(c);
		};
	}
}

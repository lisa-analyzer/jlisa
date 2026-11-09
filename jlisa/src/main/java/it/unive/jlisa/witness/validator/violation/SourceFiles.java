package it.unive.jlisa.witness.validator.violation;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tells whether the code run by the program comes from a file named by a
 * witness. JDI tells the source of a location or a class by its path relative
 * to the root of the sources, that is, the folders of its package followed by
 * the name of the file (e.g., {@code svcomp/objects/A.java}). For a file of the
 * task (as resolved through the hashes of the witness, see
 * {@link it.unive.jlisa.witness.validator.model.WitnessFiles}), the same path
 * is built from the package it declares, so that files with the same name in
 * different packages are told apart, whatever the folders they are in. A path
 * that is not a file of the task is compared by its name only.
 */
public final class SourceFiles {

	/**
	 * The package declaration of a compilation unit, after the comments and
	 * blanks that might precede it.
	 */
	private static final Pattern PACKAGE = Pattern
			.compile("package\\s+([A-Za-z_$][\\w$]*(?:\\s*\\.\\s*[A-Za-z_$][\\w$]*)*)\\s*;");

	/**
	 * For each file, its path relative to the root of the sources, if it is a
	 * file that can be read.
	 */
	private static final Map<String, Optional<String>> SOURCE_PATHS = new ConcurrentHashMap<>();

	private SourceFiles() {
	}

	/**
	 * Yields whether the given location is in the given file.
	 *
	 * @param fileName the file, as named by the plan
	 * @param location the location
	 *
	 * @return whether the location is in the file
	 */
	public static boolean contains(
			String fileName,
			Location location) {
		try {
			return matches(fileName, location.sourcePath());
		} catch (AbsentInformationException e) {
			return false;
		}
	}

	/**
	 * Yields whether the given class is defined in the given file.
	 *
	 * @param fileName the file, as named by the plan
	 * @param type     the class
	 *
	 * @return whether the class is defined in the file
	 */
	public static boolean defines(
			String fileName,
			ReferenceType type) {
		try {
			List<String> paths = type.sourcePaths(null);
			for (String path : paths) {
				if (matches(fileName, path)) {
					return true;
				}
			}
			return false;
		} catch (AbsentInformationException e) {
			return false;
		}
	}

	/**
	 * Yields whether the code of the given class, from the given source file
	 * (as reported by a {@link StackTraceElement}), is in the given file.
	 *
	 * @param fileName       the file, as named by the plan
	 * @param className      the fully qualified name of the class
	 * @param sourceFileName the name of the source file of the class
	 *
	 * @return whether the code is in the file
	 */
	public static boolean contains(
			String fileName,
			String className,
			String sourceFileName) {
		int dot = className.lastIndexOf('.');
		String packagePath = dot < 0 ? "" : className.substring(0, dot).replace('.', '/') + "/";
		return matches(fileName, packagePath + sourceFileName);
	}

	/**
	 * Yields whether the given path, relative to the root of the sources (as
	 * reported by JDI), is the one of the given file.
	 *
	 * @param fileName   the file, as named by the plan
	 * @param sourcePath the path relative to the root of the sources (can be
	 *                       {@code null})
	 *
	 * @return whether the path is the one of the file
	 */
	public static boolean matches(
			String fileName,
			String sourcePath) {
		if (fileName == null || sourcePath == null) {
			return false;
		}
		String path = sourcePath.replace('\\', '/');
		Optional<String> expected = SOURCE_PATHS.computeIfAbsent(fileName, SourceFiles::sourcePathOf);
		if (expected.isPresent()) {
			return expected.get().equals(path);
		}
		return WitnessCalls.baseName(fileName).equals(WitnessCalls.baseName(path));
	}

	/**
	 * Yields the path of the given file relative to the root of the sources:
	 * the folders of the package it declares, followed by its name. Yields
	 * nothing if it is not a file that can be read.
	 */
	private static Optional<String> sourcePathOf(
			String fileName) {
		File file = new File(fileName);
		if (!file.isFile()) {
			return Optional.empty();
		}
		String text;
		try {
			text = Files.readString(file.toPath(), StandardCharsets.ISO_8859_1);
		} catch (IOException e) {
			return Optional.empty();
		}
		Matcher m = PACKAGE.matcher(text);
		int start = skipBlanksAndComments(text);
		String name = file.getName();
		if (m.find(start) && m.start() == start) {
			return Optional.of(m.group(1).replaceAll("\\s", "").replace('.', '/') + "/" + name);
		}
		// the default package
		return Optional.of(name);
	}

	/**
	 * Yields the index of the first character of the given text that is not
	 * blank nor part of a comment.
	 */
	private static int skipBlanksAndComments(
			String text) {
		int i = 0;
		while (i < text.length()) {
			if (Character.isWhitespace(text.charAt(i)) || text.charAt(i) == '﻿') {
				i++;
			} else if (text.startsWith("//", i)) {
				int end = text.indexOf('\n', i);
				i = end < 0 ? text.length() : end + 1;
			} else if (text.startsWith("/*", i)) {
				int end = text.indexOf("*/", i + 2);
				i = end < 0 ? text.length() : end + 2;
			} else {
				break;
			}
		}
		return i;
	}
}

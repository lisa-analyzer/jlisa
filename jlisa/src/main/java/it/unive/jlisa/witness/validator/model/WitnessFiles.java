package it.unive.jlisa.witness.validator.model;

import it.unive.jlisa.witness.validator.violation.ValidatorLogger;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The files of a task, as named by a witness. The locations of a witness name
 * files by the paths the producer of the witness used, that might not exist
 * where the witness is validated (e.g., on another machine, or with the task in
 * another folder). The metadata of the witness give the SHA-256 hash of each
 * input file: a path of the witness denotes the input file of the task with the
 * same hash, whatever its path.
 */
public final class WitnessFiles {

	/**
	 * For each path of the witness, the path of the input file of the task with
	 * the same hash.
	 */
	private final Map<String, String> resolved;

	private WitnessFiles(
			Map<String, String> resolved) {
		this.resolved = resolved;
	}

	/**
	 * Matches the input files of a witness with the files of a task by their
	 * hashes.
	 *
	 * @param hashes the SHA-256 hash of each input file, for each path of the
	 *                   witness (as in its metadata; can be {@code null})
	 * @param inputs the input files and directories of the task
	 *
	 * @return the files of the task, as named by the witness
	 */
	public static WitnessFiles of(
			Map<String, String> hashes,
			Collection<File> inputs) {
		Map<String, String> resolved = new LinkedHashMap<>();
		if (hashes == null || hashes.isEmpty()) {
			return new WitnessFiles(resolved);
		}
		Map<String, List<Path>> byHash = new HashMap<>();
		for (Path file : files(inputs)) {
			try {
				byHash.computeIfAbsent(sha256(file), h -> new ArrayList<>()).add(file);
			} catch (IOException e) {
				ValidatorLogger.warn("[FILES]  Cannot hash {}: {}", file, e.getMessage());
			}
		}
		for (Map.Entry<String, String> e : hashes.entrySet()) {
			List<Path> candidates = byHash.get(e.getValue().toLowerCase());
			if (candidates == null) {
				ValidatorLogger.warn("[FILES]  No input file has the hash of {}", e.getKey());
				continue;
			}
			// files with the same contents: the one with the same name, if any
			String name = Paths.get(e.getKey()).getFileName().toString();
			Path chosen = candidates.stream()
					.filter(p -> p.getFileName().toString().equals(name))
					.findFirst()
					.orElse(candidates.get(0));
			resolved.put(e.getKey(), chosen.toString());
			ValidatorLogger.plan("  File {} is {}", e.getKey(), chosen);
		}
		return new WitnessFiles(resolved);
	}

	/**
	 * Yields the path of the file of the task that the given path of the
	 * witness denotes: the input file with the hash given for the path in the
	 * metadata of the witness, or the path itself if there is none.
	 *
	 * @param fileName the path, as written in the witness (can be {@code null})
	 *
	 * @return the path of the file of the task
	 */
	public String resolve(
			String fileName) {
		if (fileName == null) {
			return null;
		}
		String file = resolved.get(fileName);
		if (file != null) {
			return file;
		}
		// the same path, written differently (e.g., with "..")
		Path normalized = Paths.get(fileName).normalize();
		for (Map.Entry<String, String> e : resolved.entrySet()) {
			if (Paths.get(e.getKey()).normalize().equals(normalized)) {
				return e.getValue();
			}
		}
		return fileName;
	}

	private static List<Path> files(
			Collection<File> inputs) {
		List<Path> files = new ArrayList<>();
		for (File input : inputs) {
			if (input == null) {
				continue;
			}
			Path path = input.toPath().toAbsolutePath().normalize();
			if (Files.isRegularFile(path)) {
				files.add(path);
			} else if (Files.isDirectory(path)) {
				try (Stream<Path> walk = Files.walk(path)) {
					walk.filter(Files::isRegularFile).forEach(files::add);
				} catch (IOException | UncheckedIOException e) {
					ValidatorLogger.warn("[FILES]  Cannot list {}: {}", path, e.getMessage());
				}
			}
		}
		return files;
	}

	/**
	 * Yields the SHA-256 hash of the contents of the given file, as 64
	 * lowercase hexadecimal digits (as in the metadata of witnesses).
	 */
	private static String sha256(
			Path file)
			throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
		StringBuilder hex = new StringBuilder();
		for (byte b : digest.digest(Files.readAllBytes(file))) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}
}

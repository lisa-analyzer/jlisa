package it.unive.jlisa.springed;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public enum SpringTestHelper {

	WEBMVC("webmvc"),
	SECURITY("security");

	private final Path cases;

	private final Path output;

	SpringTestHelper(
			String track) {
		this.cases = Path.of("spring-testcases", track);
		this.output = Path.of("spring-outputs", track, "registry.json");
	}

	public Path output() {
		return output;
	}

	public Path extract(
			String name,
			Path into)
			throws IOException {
		unzip(cases.resolve(name + ".zip"), into);
		return into.resolve(name);
	}

	public Path sourceRoot(
			String name,
			Path into)
			throws IOException {
		return extract(name, into).resolve("src/main/java");
	}

	public static void unzip(
			Path zip,
			Path into)
			throws IOException {
		if (!Files.isRegularFile(zip))
			throw new IOException("missing test case archive: " + zip.toAbsolutePath());

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().startsWith("__MACOSX/"))
					continue;

				Path out = into.resolve(entry.getName()).normalize();
				if (!out.startsWith(into))
					throw new IOException("unsafe zip entry outside " + into + ": " + entry.getName());

				if (entry.isDirectory())
					Files.createDirectories(out);
				else {
					Files.createDirectories(out.getParent());
					Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	public static void deleteOutputs()
			throws IOException {
		for (SpringTestHelper track : values()) {
			Files.deleteIfExists(track.output);
			deleteIfEmpty(track.output.getParent());
		}
		deleteIfEmpty(Path.of("spring-outputs"));
	}

	private static void deleteIfEmpty(
			Path dir)
			throws IOException {
		try {
			Files.deleteIfExists(dir);
		} catch (DirectoryNotEmptyException keepOtherOutputs) {
			// the output directory holds other files; leave it in place
		}
	}
}

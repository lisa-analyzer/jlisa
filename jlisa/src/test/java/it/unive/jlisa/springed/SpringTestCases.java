package it.unive.jlisa.springed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SpringTestCases {

	public static final Path ROOT = Path.of("spring-testcases");

	private SpringTestCases() {
	}

	public static Path extract(String name) throws IOException {
		Path zip = ROOT.resolve(name + ".zip");
		if (!Files.isRegularFile(zip))
			throw new IOException("missing test case archive: " + zip.toAbsolutePath());

		Path target = ROOT.resolve(name);
		delete(target);

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().startsWith("__MACOSX/"))
					continue;

				Path out = ROOT.resolve(entry.getName()).normalize();
				if (!out.startsWith(ROOT))
					throw new IOException("unsafe zip entry outside spring-testcases: " + entry.getName());

				if (entry.isDirectory())
					Files.createDirectories(out);
				else {
					Files.createDirectories(out.getParent());
					Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
		return target;
	}

	public static void delete(Path path) throws IOException {
		if (!Files.exists(path))
			return;

		try (Stream<Path> paths = Files.walk(path)) {
			paths.sorted(Comparator.reverseOrder())
					.forEach(p -> {
						try {
							Files.delete(p);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}
}

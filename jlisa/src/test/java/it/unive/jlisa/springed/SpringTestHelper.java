package it.unive.jlisa.springed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SpringTestHelper {

	public static final Path ROOT = Path.of("spring-testcases");

	private SpringTestHelper() {
	}

	public static Path extract(
			String name,
			Path into)
			throws IOException {
		Path zip = ROOT.resolve(name + ".zip");
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
		return into.resolve(name);
	}

	public static Path sourceRoot(
			String name,
			Path into)
			throws IOException {
		return extract(name, into).resolve("src/main/java");
	}
}

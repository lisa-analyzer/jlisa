package it.unive.jlisa.springed.frontend;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpringProjectVisitor extends SimpleFileVisitor<Path> {

	private static final Set<String> SKIP_DIRS = Set.of(
			"target", "build", "out", "bin", "node_modules", ".git", ".idea", ".gradle");

	private final Path root;
	private final List<Path> springProjects = new ArrayList<>();

	public SpringProjectVisitor(
			Path root) {
		this.root = root;
	}

	public List<Path> getProjects() {
		return springProjects;
	}

	@Override
	public FileVisitResult preVisitDirectory(
			Path dir,
			BasicFileAttributes attrs) {
		Path path = dir.getFileName();
		String name = (path == null) ? "" : path.toString();

		if (!dir.equals(root) && (SKIP_DIRS.contains(name) || name.startsWith(".")))
			return FileVisitResult.SKIP_SUBTREE;

		if (isProject(dir)) {
			springProjects.add(dir);
			return FileVisitResult.SKIP_SUBTREE;
		}

		return FileVisitResult.CONTINUE;
	}

	protected static boolean isProject(
			Path dir) {
		boolean hasBuildFile = Files.isRegularFile(dir.resolve("pom.xml"))
				|| Files.isRegularFile(dir.resolve("build.gradle"))
				|| Files.isRegularFile(dir.resolve("build.gradle.kts"));

		return hasBuildFile && Files.isDirectory(dir.resolve("src/main/java"));
	}
}

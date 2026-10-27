package it.unive.jlisa.svcomp;

import it.unive.lisa.witness.v2.Language;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.metadata.TaskLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * The parts of the SV-COMP witnesses written by jlisa that do not depend on how
 * a violation is found: the task, the producer and the input file of a source
 * file. They are shared by the witnesses of the analysis and by those of other
 * tools of jlisa (e.g., the bug-finding mode of jlisa-wit).
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class SVCOMPWitnesses {

	/**
	 * The name of jlisa in the producer of its witnesses.
	 */
	public static final String NAME = "JLiSA";

	private SVCOMPWitnesses() {
	}

	/**
	 * Returns the task made of the given input files and property file, for
	 * Java programs. The text of the property file is the specification of the
	 * task.
	 *
	 * @param inputs       the input files and directories of the task, as given
	 *                         to the verifier
	 * @param propertyFile the property file of the task
	 *
	 * @return the task
	 *
	 * @throws IOException if the files of the task cannot be read
	 */
	public static Task task(
			List<String> inputs,
			String propertyFile)
			throws IOException {
		List<Path> paths = new ArrayList<>();
		for (String input : inputs)
			paths.add(Paths.get(input));
		return TaskLoader.load(paths, Paths.get(propertyFile), Language.JAVA, null);
	}

	/**
	 * Returns jlisa as the tool producing a witness, with the version of its
	 * jar ({@code unknown} if the jar has none) and the given description.
	 *
	 * @param description how the violation was found, or {@code null} for none
	 *
	 * @return the tool
	 */
	public static Producer producer(
			String description) {
		String version = SVCOMPWitnesses.class.getPackage().getImplementationVersion();
		return new Producer(NAME, version == null ? "unknown" : version, null, null, description);
	}

	/**
	 * Returns the input file of the given task with the given name, that is,
	 * the file of the witness for a location in the source file with that name.
	 *
	 * @param task       the task
	 * @param sourceFile the name of the source file, without folders (e.g.,
	 *                       {@code Main.java})
	 *
	 * @return the input file, or {@code null} if no input file or more than one
	 *             has that name
	 */
	public static String inputFile(
			Task task,
			String sourceFile) {
		String file = null;
		for (String input : task.getInputFiles())
			if (Paths.get(input).getFileName().toString().equals(sourceFile)) {
				if (file != null)
					return null;
				file = input;
			}
		return file;
	}
}

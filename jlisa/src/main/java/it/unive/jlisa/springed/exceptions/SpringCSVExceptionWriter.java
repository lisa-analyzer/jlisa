package it.unive.jlisa.springed.exceptions;

import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedAnnotationException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class SpringCSVExceptionWriter extends CSVExceptionWriter {

	private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger(SpringCSVExceptionWriter.class);

	public static void writeCSV(
			String fileName,
			List<? extends Throwable> exceptions) {
		File file = new File(fileName);
		File parentDir = file.getParentFile();

		if (!parentDir.exists())
			parentDir.mkdirs();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

			writer.write("\"Category\";\"Location\";\"Error Message\";\"Cause\"");
			writer.newLine();

			for (Throwable exception : exceptions) {
				writer.write(SpringCSVExceptionWriter.toCSVEntry(exception, ";", "\""));
				writer.newLine();
			}
			LOG.info("CSV file written successfully: {}", fileName);

		} catch (IOException e) {
			LOG.error("Error occurred while writing a .csv error file: {}", e.getMessage());
		}
	}

	protected static String toCSVEntry(
			Throwable e,
			String separator,
			String delimiter) {
		if (e instanceof ParsingException)
			return ((ParsingException) e).toCSVEntry(separator, delimiter);

		String category;
		String location = e.getStackTrace()[0].getFileName() + ":" + e.getStackTrace()[0].getLineNumber();
		String cause = null;

		switch (e) {
		case UnsupportedStatementException ignored:
			category = "Unsupported Statement";
			break;
		case UnresolvedTypeException ut:
			category = "Unresolvable Type";
			cause = ut.getUnresolvedName();
			break;
		case UnsupportedAnnotationException ignored:
			category = "Unsupported Annotation Variation";
			break;
		default:
			category = "Uncategorized";

		}

		StringBuilder sb = new StringBuilder();
		String message = clean(e.getMessage());

		sb.append(delimiter).append(category).append(delimiter)
				.append(separator)
				.append(delimiter).append(location).append(delimiter)
				.append(separator)
				.append(delimiter).append(message).append(delimiter)
				.append(separator)
				.append(delimiter).append(cause == null ? "" : clean(cause)).append(delimiter);
		return sb.toString();
	}
}
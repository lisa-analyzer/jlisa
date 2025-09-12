package it.unive.jlisa.frontend.exceptions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVExceptionWriter {

	public static void writeCSV(
			String fileName,
			Throwable exceptions) {
		ArrayList<Throwable> exceptionsList = new ArrayList<Throwable>();
		exceptionsList.add(exceptions);
		writeCSV(fileName, exceptionsList);
	}

	// Method to write a list of records to a CSV file
	public static void writeCSV(
			String fileName,
			List<? extends Throwable> exceptions) {
		File file = new File(fileName);
		File parentDir = file.getParentFile();
		if (!parentDir.exists()) {
			parentDir.mkdirs(); // Create the directories if they don't exist
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

			writer.write("\"Name\";\"Message\";\"Type\";\"Location\"");
			writer.newLine();

			// Iterate over each record and write it as a CSV line
			for (Throwable exception : exceptions) {
				writer.write(CSVExceptionWriter.toCSVEntry(exception, ";", "\""));
				writer.newLine(); // Add a new line after each record
			}

			System.out.println("CSV file written successfully: " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String toCSVEntry(
			Throwable e,
			String separator,
			String delimiter) {
		if (e instanceof ParsingException) {
			return ((ParsingException) e).toCSVEntry(separator, delimiter);
		}
		StringBuilder sb = new StringBuilder();
		String message = clean(e.getMessage());
		sb.append(delimiter).append("SemanticAnalysis").append(delimiter)
				.append(separator)
				.append(delimiter).append(message).append(delimiter)
				.append(separator)
				.append(delimiter).append("SemanticAnalysis").append(delimiter)
				.append(separator)
				.append(delimiter)
				.append(e.getStackTrace()[0].getClassName() + ":" + e.getStackTrace()[0].getLineNumber())
				.append(delimiter);
		return sb.toString();

	}

	private static String clean(
			String message) {
		if (message.indexOf(", location:") != -1)
			return message.substring(0, message.indexOf(", location:"));
		else
			return message;
	}
}
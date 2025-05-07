package it.unive.jlisa.frontend.exceptions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVExceptionWriter {

    // Method to write a list of records to a CSV file
    public static void writeCSV(String fileName, List<ParsingException> exceptions) {
        File file  = new File(fileName);
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();  // Create the directories if they don't exist
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

            writer.write("\"Name\";\"Message\";\"Type\";\"Location\"");
            writer.newLine();

            // Iterate over each record and write it as a CSV line
            for (ParsingException exception : exceptions) {
                writer.write(exception.toCSVEntry(";", "\""));
                writer.newLine();  // Add a new line after each record
            }

            System.out.println("CSV file written successfully: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
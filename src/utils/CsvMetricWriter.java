package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * A helper class to write lists of metrics to a CSV file.
 */
public class CsvMetricWriter {

    /**
     * Writes a list of numbers to a specified CSV file.
     * Each number is written on a new line under a given header.
     *
     * @param filePath The full path for the output CSV file (e.g., "output/metrics/data.csv").
     * @param header The column header (e.g., "hop_count").
     * @param data The list of numbers (Integer, Long, Double, etc.) to write.
     */
    public static void writeListToCsv(String filePath, String header, List<? extends Number> data) {
        if (data == null || data.isEmpty()) {
            System.out.println("  Skipped writing " + filePath + " (no data)");
            return;
        }

        try {
            // Ensure the parent directory exists
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    System.err.println("Error: Could not create directory " + parentDir.getAbsolutePath());
                    return;
                }
            }

            // Use try-with-resources for automatic closing
            try (FileWriter fw = new FileWriter(file);
                 PrintWriter pw = new PrintWriter(fw)) {

                // Write the header
                pw.println(header);

                // Write each data point on a new line
                for (Number value : data) {
                    pw.println(value.toString());
                }
                
                System.out.println("  Successfully wrote " + data.size() + " data points to " + filePath);

            } // pw and fw are auto-closed here

        } catch (IOException e) {
            System.err.println("Error writing to CSV file " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
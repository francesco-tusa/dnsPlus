package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger; // Import Logger

/**
 * A helper class to write lists of metrics to a CSV file.
 */
public class CsvMetricWriter {

    private static final Logger logger = CustomLogger.getLogger(CsvMetricWriter.class.getName()); // Get logger

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
            logger.info("  Skipped writing " + filePath + " (no data)");
            return;
        }

        try {
            // Ensure the parent directory exists
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    logger.severe("Error: Could not create directory " + parentDir.getAbsolutePath());
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
                
                logger.info("  Successfully wrote " + data.size() + " data points to " + filePath);

            } // pw and fw are auto-closed here

        } catch (IOException e) {
            logger.severe("Error writing to CSV file " + filePath + ": " + e.getMessage());
        }
    }
}
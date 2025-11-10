package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A helper class to write lists of metrics to a CSV file.
 */
public class CsvMetricWriter {

    private static final Logger logger = CustomLogger.getLogger(CsvMetricWriter.class.getName());

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

    /**
     * Writes a list of Maps to a specified CSV file.
     * Each Map represents one row of data.
     *
     * @param filePath The full path for the output CSV file.
     * @param headers An array of header strings, defining the column order.
     * @param data The list of maps, where each map is a row.
     */
    public static void writeMapListToCsv(String filePath, String[] headers, List<Map<String, Object>> data) {
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

                // 1. Write the header line
                pw.println(String.join(",", headers));

                // 2. Write each data row
                for (Map<String, Object> row : data) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < headers.length; i++) {
                        // Get the value from the map using the header key
                        Object value = row.get(headers[i]);
                        
                        // Handle null values and escape strings
                        sb.append(escapeCsvValue(value));

                        if (i < headers.length - 1) {
                            sb.append(",");
                        }
                    }
                    pw.println(sb.toString());
                }
                
                logger.info("  Successfully wrote " + data.size() + " data points to " + filePath);

            } // pw and fw are auto-closed here

        } catch (IOException e) {
            logger.severe("Error writing to CSV file " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Escapes a value for inclusion in a CSV file.
     * - Wraps in double quotes if it contains a comma or a double quote.
     * - Doubles any existing double quotes.
     */
    private static String escapeCsvValue(Object value) {
        if (value == null) {
            return ""; // Represent null as an empty field
        }
        
        String str = value.toString();
        
        // If the string contains a comma, a double quote, or a newline,
        // it needs to be escaped.
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            // 1. Replace all existing double quotes with two double quotes
            String escaped = str.replace("\"", "\"\"");
            // 2. Wrap the entire string in double quotes
            return "\"" + escaped + "\"";
        } else {
            // No escaping needed
            return str;
        }
    }
}
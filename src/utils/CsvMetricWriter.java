package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CsvMetricWriter {

    private static CsvMetricWriter instance;
    private RotatingFileWriter subscriptionWriter;
    private RotatingFileWriter publicationWriter;
    private boolean initialized = false;
    
    // Map to track the running count of messages per TraceID
    private final Map<String, Integer> traceCounts = new HashMap<>();
    
    private static final long MAX_FILE_SIZE_BYTES = 90 * 1024 * 1024; // 90 MB

    private CsvMetricWriter() { }

    public static synchronized CsvMetricWriter getInstance() {
        if (instance == null) {
            instance = new CsvMetricWriter();
        }
        return instance;
    }

    public synchronized void initialize(String runId) {
        if (initialized) return;
        try {
            // Clear counters from previous runs to avoid memory leaks or incorrect counts
            traceCounts.clear();

            String baseDir = "output/" + runId;
            
            String subDir = baseDir + "/subscriptions";
            String pubDir = baseDir + "/publications";
            
            File subFolder = new File(subDir);
            if (!subFolder.exists()) subFolder.mkdirs();
            
            File pubFolder = new File(pubDir);
            if (!pubFolder.exists()) pubFolder.mkdirs();
            
            // NEW: Added "MsgCount" to the header
            subscriptionWriter = new RotatingFileWriter(
                subDir, "subscriptions", 
                "TraceID,MsgCount,Source,Receiver,Hops,Region,Result\n"
            );
            
            publicationWriter = new RotatingFileWriter(
                pubDir, "publications", 
                "TraceID,Subscriber,Hops,LocationX,LocationY\n"
            );
            
            initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void logSubscription(String traceId, String receiver, String source, int hops, String region, String result) {
        if (!initialized) return;
        try {
            // Increment and retrieve the running count for this TraceID
            int count = traceCounts.getOrDefault(traceId, 0) + 1;
            traceCounts.put(traceId, count);

            // Append the count to the CSV line
            String line = String.format("%s,%d,%s,%s,%d,\"%s\",%s\n", traceId, count, source, receiver, hops, region, result);
            subscriptionWriter.write(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void logPublicationDelivery(String traceId, String subscriber, int hops, double x, double y) {
        if (!initialized) return;
        try {
            String line = String.format("%s,%s,%d,%.4f,%.4f\n", traceId, subscriber, hops, x, y);
            publicationWriter.write(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void close() {
        try {
            if (subscriptionWriter != null) subscriptionWriter.close();
            if (publicationWriter != null) publicationWriter.close();
            traceCounts.clear();
            initialized = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inner helper class to handle file rotation based on size.
     */
    private class RotatingFileWriter {
        private final String dirPath;
        private final String baseName;
        private final String header;
        
        private BufferedWriter currentWriter;
        private int fileIndex = 1;
        private long currentBytes = 0;

        public RotatingFileWriter(String dirPath, String baseName, String header) throws IOException {
            this.dirPath = dirPath;
            this.baseName = baseName;
            this.header = header;
            rotate(); // Create the first file
        }

        private void rotate() throws IOException {
            if (currentWriter != null) {
                currentWriter.close();
            }
            
            // Create new file: name_1.csv, name_2.csv
            File file = new File(dirPath, baseName + "_" + fileIndex + ".csv");
            currentWriter = new BufferedWriter(new FileWriter(file));
            
            // Write header
            currentWriter.write(header);
            currentWriter.flush();
            
            // Reset counters (header counts towards size)
            currentBytes = header.length();
            fileIndex++;
        }

        public void write(String line) throws IOException {
            // Check if rotation is needed
            if (currentBytes + line.length() > MAX_FILE_SIZE_BYTES) {
                rotate();
            }
            
            currentWriter.write(line);
            currentWriter.flush();
            currentBytes += line.length();
        }

        public void close() throws IOException {
            if (currentWriter != null) {
                currentWriter.close();
            }
        }
    }
}
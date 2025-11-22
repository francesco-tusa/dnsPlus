package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvMetricWriter {

    private static CsvMetricWriter instance;
    private RotatingFileWriter subscriptionWriter;
    private RotatingFileWriter publicationWriter;
    private boolean initialized = false;
    
    private static final long MAX_FILE_SIZE_BYTES = 95 * 1024 * 1024; // 95 MB

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
            String dirPath = "output/metrics/" + runId;
            File metricsDir = new File(dirPath);
            if (!metricsDir.exists()) {
                metricsDir.mkdirs();
            }
            
            // Initialize Rotating Writers
            // subscriptions_1.csv, subscriptions_2.csv ...
            subscriptionWriter = new RotatingFileWriter(
                dirPath, "subscriptions", 
                "TraceID,Source,Receiver,Hops,Region\n"
            );
            
            // publications_1.csv, publications_2.csv ...
            publicationWriter = new RotatingFileWriter(
                dirPath, "publications", 
                "TraceID,Subscriber,Hops,LocationX,LocationY\n"
            );
            
            initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void logSubscription(String traceId, String receiver, String source, int hops, String region) {
        if (!initialized) return;
        try {
            String line = String.format("%s,%s,%s,%d,\"%s\"\n", traceId, source, receiver, hops, region);
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
            currentWriter.flush(); // Ensure data is safe on disk
            currentBytes += line.length();
        }

        public void close() throws IOException {
            if (currentWriter != null) {
                currentWriter.close();
            }
        }
    }
}

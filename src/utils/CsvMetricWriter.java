package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvMetricWriter {

    private static CsvMetricWriter instance;
    private BufferedWriter subscriptionWriter;
    private BufferedWriter publicationWriter;
    private boolean initialized = false;

    private CsvMetricWriter() {
        // Private constructor
    }

    public static synchronized CsvMetricWriter getInstance() {
        if (instance == null) {
            instance = new CsvMetricWriter();
        }
        return instance;
    }

    /**
     * Initializes the writers with a specific Experiment Run ID.
     * Creates a directory matching the runId.
     */
    public synchronized void initialize(String runId) {
        if (initialized) return;
        try {
            String dirPath = "output/metrics/" + runId;
            File metricsDir = new File(dirPath);
            if (!metricsDir.exists()) {
                metricsDir.mkdirs();
            }
            
            // Overwrite existing files
            subscriptionWriter = new BufferedWriter(new FileWriter(dirPath + "/subscriptions.csv"));
            // Removed Timestamp, Swapped Source/Receiver
            subscriptionWriter.write("TraceID,Source,Receiver,Hops,Region\n");
            
            publicationWriter = new BufferedWriter(new FileWriter(dirPath + "/publications.csv"));
            // Removed Timestamp
            publicationWriter.write("TraceID,Subscriber,Hops,LocationX,LocationY\n");
            
            initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Updated Signature: No Timestamp, Swapped Receiver/Source order in output
    public synchronized void logSubscription(String traceId, String receiver, String source, int hops, String region) {
        if (!initialized) return;
        try {
            // Format: TraceID, Source, Receiver, Hops, Region
            subscriptionWriter.write(String.format("%s,%s,%s,%d,\"%s\"\n", 
                traceId, source, receiver, hops, region));
            subscriptionWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Updated Signature: No Timestamp
    public synchronized void logPublicationDelivery(String traceId, String subscriber, int hops, double x, double y) {
        if (!initialized) return;
        try {
            // Format: TraceID, Subscriber, Hops, LocX, LocY
            publicationWriter.write(String.format("%s,%s,%d,%.4f,%.4f\n", 
                traceId, subscriber, hops, x, y));
            publicationWriter.flush();
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
}
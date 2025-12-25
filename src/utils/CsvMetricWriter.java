package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import simulator.core.Location;
import simulator.core.TreeNode;

public class CsvMetricWriter {

    private static CsvMetricWriter instance;
    private RotatingFileWriter subscriptionWriter;
    private RotatingFileWriter publicationWriter;
    
    private BufferedWriter subSummaryWriter;
    private BufferedWriter pubSummaryWriter;

    private boolean initialized = false;

    // Running counts (we will prune these dynamically now)
    private final Map<String, Integer> subTraceCounts = new HashMap<>();
    private final Map<String, Integer> pubTraceCounts = new HashMap<>();

    // --- Streaming State Buffers (Single Entry) ---
    private String currentSubTraceId = null;
    private SummaryEntry currentSubSummary = null;

    private String currentPubTraceId = null;
    private SummaryEntry currentPubSummary = null;

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
            subTraceCounts.clear();
            pubTraceCounts.clear();
            
            // Reset streaming state
            currentSubTraceId = null;
            currentSubSummary = null;
            currentPubTraceId = null;
            currentPubSummary = null;

            String baseDir = "output/" + runId;
            String subDir = baseDir + "/subscriptions";
            String pubDir = baseDir + "/publications";
            
            new File(subDir).mkdirs();
            new File(pubDir).mkdirs();
            
            subscriptionWriter = new RotatingFileWriter(
                subDir, "subscriptions", 
                "TraceID,MsgCount,Source,Receiver,Hops,Region,Result\n"
            );
            subSummaryWriter = initializeSummaryWriter(subDir, "subscription_summary.csv");

            publicationWriter = new RotatingFileWriter(
                pubDir, "publications", 
                "TraceID,MsgCount,Source,Receiver,Hops,Location,Result\n"
            );
            pubSummaryWriter = initializeSummaryWriter(pubDir, "publication_summary.csv");
            
            initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedWriter initializeSummaryWriter(String dir, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir, filename)));
        writer.write("id,country,long,lat,max_row_count\n");
        writer.flush();
        return writer;
    }

    // --- Subscription Logging (Updated for Streaming) ---
    public synchronized void logSubscription(String traceId, String receiver, TreeNode sourceNode, int hops, String region, String result) {
        if (!initialized) return;

        // 1. Check for Trace ID Change (Flush)
        if (currentSubTraceId != null && !traceId.equals(currentSubTraceId)) {
            flushSummary(subSummaryWriter, currentSubSummary, subTraceCounts.get(currentSubTraceId));
            // Critical: Free memory for the old trace count
            subTraceCounts.remove(currentSubTraceId);
            currentSubSummary = null;
        }
        currentSubTraceId = traceId;

        // 2. Initialize Summary if needed
        if (currentSubSummary == null) {
            currentSubSummary = new SummaryEntry();
            currentSubSummary.id = traceId;
        }

        // 3. Update Summary Metadata (if this node has location info and we haven't captured it yet)
        //    We prioritize the first node in the trace that has a valid physical location.
        if (sourceNode != null && sourceNode.getMetricLocation() != null && currentSubSummary.country == null) {
            Location loc = sourceNode.getMetricLocation();
            currentSubSummary.longitude = loc.getX();
            currentSubSummary.latitude = loc.getY();
            currentSubSummary.country = extractCountry(sourceNode);
        }

        // 4. Log the standard event
        logEvent(traceId, receiver, sourceNode, hops, region, result, subscriptionWriter, subTraceCounts);
    }

    // --- Publication Logging (Updated for Streaming) ---
    public synchronized void logPublication(String traceId, String receiver, TreeNode sourceNode, int hops, String location, String result) {
        if (!initialized) return;

        // 1. Check for Trace ID Change (Flush)
        if (currentPubTraceId != null && !traceId.equals(currentPubTraceId)) {
            flushSummary(pubSummaryWriter, currentPubSummary, pubTraceCounts.get(currentPubTraceId));
            pubTraceCounts.remove(currentPubTraceId);
            currentPubSummary = null;
        }
        currentPubTraceId = traceId;

        // 2. Initialize Summary
        if (currentPubSummary == null) {
            currentPubSummary = new SummaryEntry();
            currentPubSummary.id = traceId;
        }

        // 3. Update Summary Metadata
        if (sourceNode != null && sourceNode.getMetricLocation() != null && currentPubSummary.country == null) {
            Location loc = sourceNode.getMetricLocation();
            currentPubSummary.longitude = loc.getX();
            currentPubSummary.latitude = loc.getY();
            currentPubSummary.country = extractCountry(sourceNode);
        }

        // 4. Log the standard event
        logEvent(traceId, receiver, sourceNode, hops, location, result, publicationWriter, pubTraceCounts);
    }
    
    // --- Helper to write the summary line immediately ---
    private void flushSummary(BufferedWriter writer, SummaryEntry summary, Integer maxCount) {
        if (writer == null || summary == null) return;
        try {
            int count = (maxCount != null) ? maxCount : 0;
            // Handle cases where no location was ever found in the trace (e.g. abstract nodes only)
            String countryStr = (summary.country != null) ? summary.country : "Unknown";
            
            writer.write(String.format("%s,%s,%.4f,%.4f,%d\n", 
                summary.id, countryStr, summary.longitude, summary.latitude, count));
            writer.flush(); // Ensure it hits disk
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void logEvent(
            String traceId, String receiver, TreeNode sourceNode, int hops, 
            String payload, String result,
            RotatingFileWriter writer, 
            Map<String, Integer> counts) {
        
        try {
            int count = counts.getOrDefault(traceId, 0) + 1;
            counts.put(traceId, count);

            String sourceName = (sourceNode != null) ? sourceNode.getName() : "null";
            writer.write(String.format("%s,%d,%s,%s,%d,\"%s\",%s\n", 
                traceId, count, sourceName, receiver, hops, payload, result));
                
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    public void close() {
        try {
            // Flush any currently active traces
            if (currentSubTraceId != null) {
                flushSummary(subSummaryWriter, currentSubSummary, subTraceCounts.get(currentSubTraceId));
            }
            if (currentPubTraceId != null) {
                flushSummary(pubSummaryWriter, currentPubSummary, pubTraceCounts.get(currentPubTraceId));
            }

            if (subscriptionWriter != null) subscriptionWriter.close();
            if (publicationWriter != null) publicationWriter.close();
            if (subSummaryWriter != null) subSummaryWriter.close();
            if (pubSummaryWriter != null) pubSummaryWriter.close();

            subTraceCounts.clear();
            pubTraceCounts.clear();
            initialized = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String extractCountry(TreeNode node) {
        List<TreeNode> path = new ArrayList<>();
        TreeNode current = node;
        while (current != null) {
            path.add(current);
            current = current.getParent();
        }
        if (path.size() >= 3) {
             return path.get(path.size() - 3).getName();
        }
        return "Unknown";
    }

    private static class SummaryEntry {
        String id;
        String country;
        double longitude;
        double latitude;
    }

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
            rotate();
        }
        private void rotate() throws IOException {
            if (currentWriter != null) currentWriter.close();
            File file = new File(dirPath, baseName + "_" + fileIndex + ".csv");
            currentWriter = new BufferedWriter(new FileWriter(file));
            currentWriter.write(header);
            currentWriter.flush();
            currentBytes = header.length();
            fileIndex++;
        }
        public void write(String line) throws IOException {
            if (currentBytes + line.length() > MAX_FILE_SIZE_BYTES) rotate();
            currentWriter.write(line);
            currentWriter.flush();
            currentBytes += line.length();
        }
        public void close() throws IOException { if (currentWriter != null) currentWriter.close(); }
    }
}
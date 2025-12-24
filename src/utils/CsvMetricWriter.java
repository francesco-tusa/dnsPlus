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
    
    // Maps to track running counts per TraceID
    private final Map<String, Integer> subTraceCounts = new HashMap<>();
    private final Map<String, Integer> pubTraceCounts = new HashMap<>(); 

    // Maps to store summary data
    private final Map<String, SummaryEntry> subSummaryMap = new HashMap<>();
    private final Map<String, SummaryEntry> pubSummaryMap = new HashMap<>(); 
    
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
            subSummaryMap.clear();
            pubSummaryMap.clear();

            String baseDir = "output/" + runId;
            String subDir = baseDir + "/subscriptions";
            String pubDir = baseDir + "/publications";
            
            new File(subDir).mkdirs();
            new File(pubDir).mkdirs();
            
            // --- Subscription Writers ---
            subscriptionWriter = new RotatingFileWriter(
                subDir, "subscriptions", 
                "TraceID,MsgCount,Source,Receiver,Hops,Region,Result\n"
            );
            subSummaryWriter = initializeSummaryWriter(subDir, "subscription_summary.csv");

            // --- Publication Writers ---
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

    // --- Subscription Logging ---
    public synchronized void logSubscription(String traceId, String receiver, TreeNode sourceNode, int hops, String region, String result) {
        logEvent(traceId, receiver, sourceNode, hops, region, result, subscriptionWriter, subTraceCounts, subSummaryMap);
    }

    // --- Publication Logging ---
   public synchronized void logPublication(String traceId, String receiver, TreeNode sourceNode, int hops, String location, String result) {
        logEvent(traceId, receiver, sourceNode, hops, location, result, publicationWriter, pubTraceCounts, pubSummaryMap);
    }
    
    // --- Generic Logging Helper ---
    private synchronized void logEvent(
            String traceId, String receiver, TreeNode sourceNode, int hops, 
            String payload, String result,
            RotatingFileWriter writer, 
            Map<String, Integer> traceCounts,
            Map<String, SummaryEntry> summaryMap) {
        
        if (!initialized) return;
        try {
            int count = traceCounts.getOrDefault(traceId, 0) + 1;
            traceCounts.put(traceId, count);

            String sourceName = (sourceNode != null) ? sourceNode.getName() : "null";
            // Format: TraceID,MsgCount,Source,Receiver,Hops,Payload,Result
            writer.write(String.format("%s,%d,%s,%s,%d,\"%s\",%s\n", 
                traceId, count, sourceName, receiver, hops, payload, result));

            if (sourceNode != null && sourceNode.getMetricLocation() != null) {
                summaryMap.computeIfAbsent(traceId, k -> {
                    SummaryEntry entry = new SummaryEntry();
                    entry.id = k;
                    Location loc = sourceNode.getMetricLocation();
                    entry.longitude = loc.getX();
                    entry.latitude = loc.getY();
                    entry.country = extractCountry(sourceNode);
                    return entry;
                });
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    public void close() {
        try {
            if (subscriptionWriter != null) subscriptionWriter.close();
            if (publicationWriter != null) publicationWriter.close();
            
            // Write Subscription Summary
            if (subSummaryWriter != null) {
                writeSummary(subSummaryWriter, subSummaryMap, subTraceCounts);
            }
            
            // Write Publication Summary
            if (pubSummaryWriter != null) {
                writeSummary(pubSummaryWriter, pubSummaryMap, pubTraceCounts);
            }

            subTraceCounts.clear();
            pubTraceCounts.clear();
            subSummaryMap.clear();
            pubSummaryMap.clear();
            initialized = false;
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void writeSummary(BufferedWriter writer, Map<String, SummaryEntry> map, Map<String, Integer> counts) throws IOException {
        for (Map.Entry<String, SummaryEntry> entry : map.entrySet()) {
            SummaryEntry s = entry.getValue();
            int maxCount = counts.getOrDefault(s.id, 0);
            writer.write(String.format("%s,%s,%.4f,%.4f,%d\n", s.id, s.country, s.longitude, s.latitude, maxCount));
        }
        writer.flush();
        writer.close();
    }

    /**
     * Generalized to work for both Subscribers and Publishers (any TreeNode).
     * Extract Country at Level 2 (Root=L0, Continent=L1, Country=L2).
     */
    private String extractCountry(TreeNode node) {
        List<TreeNode> path = new ArrayList<>();
        TreeNode current = node;
        while (current != null) {
            path.add(current);
            current = current.getParent();
        }
        
        // Path is [LeafEntity, LeafBroker, ..., Country, Continent, Root]
        // Root is at index: size - 1
        // Level 1 is at:    size - 2
        // Level 2 is at:    size - 3
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
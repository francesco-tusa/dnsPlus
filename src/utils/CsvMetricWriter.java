package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;

public class CsvMetricWriter {

    private static CsvMetricWriter instance;
    private RotatingFileWriter subscriptionWriter;
    private RotatingFileWriter publicationWriter;
    private BufferedWriter summaryWriter; 
    
    private boolean initialized = false;
    
    // Map to track the running count of messages per TraceID
    private final Map<String, Integer> traceCounts = new HashMap<>();

    // Map to store summary data for each trace
    private final Map<String, SummaryEntry> summaryMap = new HashMap<>();
    
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
            traceCounts.clear();
            summaryMap.clear();

            String baseDir = "output/" + runId;
            
            String subDir = baseDir + "/subscriptions";
            String pubDir = baseDir + "/publications";
            
            File subFolder = new File(subDir);
            if (!subFolder.exists()) subFolder.mkdirs();
            
            File pubFolder = new File(pubDir);
            if (!pubFolder.exists()) pubFolder.mkdirs();
            
            subscriptionWriter = new RotatingFileWriter(
                subDir, "subscriptions", 
                "TraceID,MsgCount,Source,Receiver,Hops,Region,Result\n"
            );
            
            publicationWriter = new RotatingFileWriter(
                pubDir, "publications", 
                "TraceID,Subscriber,Hops,LocationX,LocationY\n"
            );

            File summaryFile = new File(subDir, "subscription_summary.csv");
            summaryWriter = new BufferedWriter(new FileWriter(summaryFile));
            summaryWriter.write("id,country,long,lat,max_row_count\n");
            summaryWriter.flush();
            
            initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void logSubscription(String traceId, String receiver, TreeNode sourceNode, int hops, String region, String result) {
        if (!initialized) return;
        try {
            int count = traceCounts.getOrDefault(traceId, 0) + 1;
            traceCounts.put(traceId, count);

            String sourceName = (sourceNode != null) ? sourceNode.getName() : "null";

            // Append the count to the CSV line
            String line = String.format("%s,%d,%s,%s,%d,\"%s\",%s\n", traceId, count, sourceName, receiver, hops, region, result);
            subscriptionWriter.write(line);

            // NEW: Capture summary data if this is the start of the trace (from Subscriber)
            if (sourceNode instanceof SubscriberWithLocation sub) {
                summaryMap.computeIfAbsent(traceId, k -> {
                    SummaryEntry entry = new SummaryEntry();
                    entry.id = k;
                    entry.longitude = sub.getLocation().getX();
                    entry.latitude = sub.getLocation().getY();
                    entry.country = extractCountry(sub);
                    return entry;
                });
            }

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
            
            if (summaryWriter != null) {
                for (Map.Entry<String, SummaryEntry> entry : summaryMap.entrySet()) {
                    SummaryEntry s = entry.getValue();
                    // Get the final max count from traceCounts
                    int maxCount = traceCounts.getOrDefault(s.id, 0);
                    
                    String line = String.format("%s,%s,%.4f,%.4f,%d\n", 
                        s.id, s.country, s.longitude, s.latitude, maxCount);
                    summaryWriter.write(line);
                }
                summaryWriter.close();
            }

            traceCounts.clear();
            summaryMap.clear();
            initialized = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts the Country by traversing the topology tree up to Level 2.
     * Hierarchy: Root (Level 0) -> Continent (Level 1) -> Country (Level 2).
     */
    private String extractCountry(SubscriberWithLocation sub) {
        List<TreeNode> path = new ArrayList<>();
        TreeNode current = sub;
        
        // 1. Traverse up to the root to build the full path
        while (current != null) {
            path.add(current);
            current = current.getParent();
        }
        
        // Path is now Bottom-Up: [Subscriber, LeafBroker, ..., Level 2, Level 1, Root]
        // We need to access Level 2 (Country).
        
        // Calculate index of Level 2 from the end of the list
        // Root is at index: size - 1
        // Level 1 is at:    size - 2
        // Level 2 is at:    size - 3
        
        if (path.size() >= 3) {
            TreeNode countryNode = path.get(path.size() - 3);
            return countryNode.getName();
        }
        
        return "Unknown";
    }

    private static class SummaryEntry {
        String id;
        String country;
        double longitude;
        double latitude;
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
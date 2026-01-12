package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import simulator.config.SimConfiguration;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.events.SimulationPublication;
import simulator.events.metrics.EventMetrics;

public class CsvMetricWriter {

    private static CsvMetricWriter instance;
    private RotatingFileWriter subscriptionWriter;
    private RotatingFileWriter publicationWriter;
    private BufferedWriter subSummaryWriter;
    private BufferedWriter pubSummaryWriter;

    private boolean initialized = false;
    
    // Master switch for all event tracing
    private boolean eventTracingEnabled = true;

    private final Map<Long, Integer> subTraceCounts = new HashMap<>();
    private final Map<Long, Integer> pubTraceCounts = new HashMap<>();

    private Long currentSubTraceId = null;
    private SummaryEntry currentSubSummary = null;

    private Long currentPubTraceId = null;
    private SummaryEntry currentPubSummary = null;

    private static final long MAX_FILE_SIZE_BYTES = 90 * 1024 * 1024; 

    private CsvMetricWriter() { }

    public static synchronized CsvMetricWriter getInstance() {
        if (instance == null) instance = new CsvMetricWriter();
        return instance;
    }

    public synchronized void initialize(String runId) { initialize(runId, true); }

    public synchronized void initialize(String runId, boolean enableTracing) {
        if (initialized) return;
        try {
            this.eventTracingEnabled = enableTracing;
            subTraceCounts.clear(); pubTraceCounts.clear();
            currentSubTraceId = null; currentSubSummary = null;
            currentPubTraceId = null; currentPubSummary = null;

            // 1. Resolve Output Directory (Respecting Config)
            String outputRoot = "output/";
            try {
                // Try to get from config, fallback to default if config not ready (e.g. unit tests)
                outputRoot = SimConfiguration.get().paths.outputDir;
                if (!outputRoot.endsWith("/")) outputRoot += "/";
            } catch (Exception e) {
                // Fallback used if SimConfiguration is not initialized
            }
            
            String baseDir = outputRoot + runId;

            // 2. ALWAYS create the base directory.
            // This ensures that the general simulation.log (created by CustomLogger/SimulationRunner)
            // has a valid destination, even if we decide not to write CSV traces.
            new File(baseDir).mkdirs();

            // 3. Check Flag to decide on CSV Writers
            if (!this.eventTracingEnabled) {
                initialized = true;
                return;
            }

            String subDir = baseDir + "/subscriptions";
            String pubDir = baseDir + "/publications";
            
            // Create Subscription Writers
            new File(subDir).mkdirs();
            subscriptionWriter = new RotatingFileWriter(subDir, "subscriptions", "TraceID,MsgCount,Source,Receiver,Hops,Region,Result\n");
            subSummaryWriter = initializeSummaryWriter(subDir, "subscription_summary.csv");

            // Create Publication Writers
            new File(pubDir).mkdirs();
            publicationWriter = new RotatingFileWriter(pubDir, "publications", "TraceID,MsgCount,Source,Receiver,Hops,Location,Result\n");
            pubSummaryWriter = initializeSummaryWriter(pubDir, "publication_summary.csv");
            
            initialized = true;
        } catch (IOException e) { e.printStackTrace(); }
    }

    private BufferedWriter initializeSummaryWriter(String dir, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir, filename)));
        writer.write("trace_id,source,country,long,lat,max_row_count\n");
        writer.flush();
        return writer;
    }

    private String formatTraceId(long traceId) {
        long high = traceId >>> 32;       
        long low = traceId & 0xFFFFFFFFL; 
        return high + ":" + low;
    }

    public void logSubscription(SimulationSubscription s, String receiver, String logDetail, String result) {
        if (!eventTracingEnabled) return;
        if (s.getMetrics() == null) return;
        logSubscription(s.getMetrics().getTraceId(), receiver, s.getSource(), s.getHops(), logDetail, result, s.getMetrics());
    }
    
    public void logPublication(SimulationPublication p, String receiver, String location, String result) {
         if (!initialized || !eventTracingEnabled) return;
         if (p.getMetrics() == null) return;
         logPublication(p.getMetrics().getTraceId(), receiver, p.getSource(), p.getHops(), location, result, p.getMetrics());
    }

    public synchronized void logSubscription(long traceId, String receiver, TreeNode sourceNode, int hops, String region, String result, EventMetrics metrics) {
        if (!initialized || !eventTracingEnabled) return;

        if (currentSubTraceId != null && traceId != currentSubTraceId) {
            flushSummary(subSummaryWriter, currentSubSummary, subTraceCounts.get(currentSubTraceId));
            subTraceCounts.remove(currentSubTraceId);
            currentSubSummary = null;
        }
        currentSubTraceId = traceId;

        if (currentSubSummary == null) {
            currentSubSummary = new SummaryEntry();
            currentSubSummary.id = traceId;
        }

        if (metrics != null) {
            if (metrics.getOriginalSourceName() != null) currentSubSummary.sourceName = metrics.getOriginalSourceName();
            if (metrics.getOriginalCountry() != null) currentSubSummary.country = metrics.getOriginalCountry();
            if (metrics.getOriginalLongitude() != null) currentSubSummary.longitude = metrics.getOriginalLongitude();
            if (metrics.getOriginalLatitude() != null) currentSubSummary.latitude = metrics.getOriginalLatitude();
        }
        
        if (currentSubSummary.sourceName == null && sourceNode != null) {
             currentSubSummary.sourceName = sourceNode.getName();
        }

        logEvent(traceId, receiver, sourceNode, hops, region, result, subscriptionWriter, subTraceCounts);
    }

    public synchronized void logPublication(long traceId, String receiver, TreeNode sourceNode, int hops, String location, String result, EventMetrics metrics) {
        if (!initialized || !eventTracingEnabled) return;

        if (currentPubTraceId != null && traceId != currentPubTraceId) {
            flushSummary(pubSummaryWriter, currentPubSummary, pubTraceCounts.get(currentPubTraceId));
            pubTraceCounts.remove(currentPubTraceId);
            currentPubSummary = null;
        }
        currentPubTraceId = traceId;

        if (currentPubSummary == null) {
            currentPubSummary = new SummaryEntry();
            currentPubSummary.id = traceId;
        }

        if (metrics != null) {
            if (metrics.getOriginalSourceName() != null) currentPubSummary.sourceName = metrics.getOriginalSourceName();
            if (metrics.getOriginalCountry() != null) currentPubSummary.country = metrics.getOriginalCountry();
            if (metrics.getOriginalLongitude() != null) currentPubSummary.longitude = metrics.getOriginalLongitude();
            if (metrics.getOriginalLatitude() != null) currentPubSummary.latitude = metrics.getOriginalLatitude();
        }

        if (currentPubSummary.sourceName == null && sourceNode != null) {
            currentPubSummary.sourceName = sourceNode.getName();
        }

        logEvent(traceId, receiver, sourceNode, hops, location, result, publicationWriter, pubTraceCounts);
    }
    
    private void flushSummary(BufferedWriter writer, SummaryEntry summary, Integer maxCount) {
        if (writer == null || summary == null) return;
        try {
            int count = (maxCount != null) ? maxCount : 0;
            String countryStr = (summary.country != null) ? summary.country : "Unknown";
            String sourceStr = (summary.sourceName != null) ? summary.sourceName : "Unknown";
            
            String idStr = formatTraceId(summary.id);

            writer.write(String.format("%s,\"%s\",\"%s\",%.4f,%.4f,%d\n", idStr, sourceStr, countryStr, summary.longitude, summary.latitude, count));
            writer.flush(); 
        } catch (IOException e) { e.printStackTrace(); }
    }

    private synchronized void logEvent(long traceId, String receiver, TreeNode sourceNode, int hops, String payload, String result, RotatingFileWriter writer, Map<Long, Integer> counts) {
        try {
            int count = counts.getOrDefault(traceId, 0) + 1;
            counts.put(traceId, count);
            String sourceName = (sourceNode != null) ? sourceNode.getName() : "null";
            String idStr = formatTraceId(traceId);
            writer.write(String.format("%s,%d,\"%s\",\"%s\",%d,\"%s\",%s\n", idStr, count, sourceName, receiver, hops, payload, result));
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    public void close() {
        try {
            if (eventTracingEnabled) {
                if (currentSubTraceId != null) flushSummary(subSummaryWriter, currentSubSummary, subTraceCounts.get(currentSubTraceId));
                if (subscriptionWriter != null) subscriptionWriter.close();
                if (subSummaryWriter != null) subSummaryWriter.close();
                
                if (currentPubTraceId != null) flushSummary(pubSummaryWriter, currentPubSummary, pubTraceCounts.get(currentPubTraceId));
                if (publicationWriter != null) publicationWriter.close();
                if (pubSummaryWriter != null) pubSummaryWriter.close();
            }
            
            subTraceCounts.clear(); pubTraceCounts.clear(); initialized = false;
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static class SummaryEntry { long id; String sourceName; String country; double longitude; double latitude; }
    
    private class RotatingFileWriter {
        private final String dirPath; private final String baseName; private final String header;
        private BufferedWriter currentWriter; private int fileIndex = 1; private long currentBytes = 0;
        public RotatingFileWriter(String dirPath, String baseName, String header) throws IOException {
            this.dirPath = dirPath; this.baseName = baseName; this.header = header; rotate();
        }
        private void rotate() throws IOException {
            if (currentWriter != null) currentWriter.close();
            File file = new File(dirPath, baseName + "_" + fileIndex + ".csv");
            currentWriter = new BufferedWriter(new FileWriter(file));
            currentWriter.write(header); currentWriter.flush(); currentBytes = header.length(); fileIndex++;
        }
        public void write(String line) throws IOException {
            if (currentBytes + line.length() > MAX_FILE_SIZE_BYTES) rotate();
            currentWriter.write(line); currentWriter.flush(); currentBytes += line.length();
        }
        public void close() throws IOException { if (currentWriter != null) currentWriter.close(); }
    }
}
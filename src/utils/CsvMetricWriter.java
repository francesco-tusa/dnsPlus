package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.metrics.EventMetrics;
import simulator.regions.SpatialRegion;

public class CsvMetricWriter {

    public interface TraceMetricStrategy {
        String getSubscriptionHeader();
        String formatBrokerEvent(Object... args);
        String formatSubscriberEvent(double x, double y);
    }

    // Strategy 1: Regional / Spatial
    public static class RegionTraceStrategy implements TraceMetricStrategy {
        @Override
        public String getSubscriptionHeader() {
            return "TraceID,Seq,From,Node,Hops,IncomingRegion,BrokerRegion,Details,Op\n";
        }

        @Override
        public String formatBrokerEvent(Object... args) {
            String incoming = (args.length > 0 && args[0] instanceof SpatialRegion r) ? r.toLogString() : "";
            String broker   = (args.length > 1 && args[1] instanceof SpatialRegion r) ? r.toLogString() : "";
            String detail   = (args.length > 2) ? String.valueOf(args[2]) : "";
            return String.format("\"%s\",\"%s\",\"%s\"", incoming, broker, detail);
        }

        @Override
        public String formatSubscriberEvent(double x, double y) {
            return String.format("\"(%.4f, %.4f)\",\"\",\"\"", x, y);
        }
    }

    // Strategy 2: Proximity / Location
    public static class ProximityTraceStrategy implements TraceMetricStrategy {
        @Override
        public String getSubscriptionHeader() {
            return "TraceID,Seq,From,Node,Hops,Location,Decision,Op\n";
        }

        @Override
        public String formatBrokerEvent(Object... args) {
            String locStr = "";
            if (args.length > 0 && args[0] instanceof Location l) {
                locStr = String.format("(%.4f, %.4f)", l.getX(), l.getY());
            }
            String decision = (args.length > 1) ? String.valueOf(args[1]) : "";
            
            return String.format("\"%s\",\"%s\"", locStr, decision);
        }

        @Override
        public String formatSubscriberEvent(double x, double y) {
            return String.format("\"(%.4f, %.4f)\",\"\"", x, y);
        }
    }

    private static CsvMetricWriter instance;
    private TraceMetricStrategy traceStrategy; 

    private RotatingFileWriter subscriptionWriter;
    private RotatingFileWriter publicationWriter;
    private BufferedWriter subSummaryWriter;
    private BufferedWriter pubSummaryWriter;

    private boolean initialized = false;
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

    public synchronized void initialize(String runId, boolean enableTracing, TraceMetricStrategy strategy) {
        if (initialized) return;
        try {
            this.eventTracingEnabled = enableTracing;
            this.traceStrategy = (strategy != null) ? strategy : new RegionTraceStrategy();

            subTraceCounts.clear(); pubTraceCounts.clear();
            currentSubTraceId = null; currentSubSummary = null;
            currentPubTraceId = null; currentPubSummary = null;

            String outputRoot = SimConfiguration.get().paths.outputDir;
            if (!outputRoot.endsWith("/")) outputRoot += "/";
            String baseDir = outputRoot + runId;
            new File(baseDir).mkdirs();

            if (!this.eventTracingEnabled) {
                initialized = true;
                return;
            }

            String subDir = baseDir + "/subscriptions";
            String pubDir = baseDir + "/publications";
            
            new File(subDir).mkdirs();
            new File(pubDir).mkdirs();
            
            subscriptionWriter = new RotatingFileWriter(subDir, "subscriptions", this.traceStrategy.getSubscriptionHeader());
            subSummaryWriter = initializeSummaryWriter(subDir, "subscription_summary.csv");

            publicationWriter = new RotatingFileWriter(pubDir, "publications", "TraceID,MsgCount,Source,Receiver,Hops,Location,Result\n");
            pubSummaryWriter = initializeSummaryWriter(pubDir, "publication_summary.csv");
            
            initialized = true;
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void initialize(String runId, boolean enableTracing) {
        initialize(runId, enableTracing, new RegionTraceStrategy());
    }

    public void logSubscription(SimulationSubscription s, String receiver, String result, Object... args) {
        if (!eventTracingEnabled || s.getMetrics() == null) return;
        String formattedData = traceStrategy.formatBrokerEvent(args);
        writeSubscriptionLine(s, receiver, formattedData, result);
    }

    public void logSubscriberEvent(SimulationSubscription s, String receiver, double x, double y, String result) {
        if (!eventTracingEnabled || s.getMetrics() == null) return;
        String formattedData = traceStrategy.formatSubscriberEvent(x, y);
        writeSubscriptionLine(s, receiver, formattedData, result);
    }

    private synchronized void writeSubscriptionLine(SimulationSubscription s, String receiver, String formattedData, String result) {
        EventMetrics metrics = s.getMetrics();
        long traceId = metrics.getTraceId();

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

        try {
            int count = subTraceCounts.getOrDefault(traceId, 0) + 1;
            subTraceCounts.put(traceId, count);
            String sourceName = (s.getSource() != null) ? s.getSource().getName() : "null";
            String idStr = formatTraceId(traceId);
            
            subscriptionWriter.write(String.format("%s,%d,\"%s\",\"%s\",%d,%s,%s\n", 
                idStr, count, sourceName, receiver, s.getHops(), formattedData, result));
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

    public void logPublication(SimulationPublication p, String receiver, String location, String result) {
         if (!initialized || !eventTracingEnabled) return;
         if (p.getMetrics() == null) return;
         logEvent(p.getMetrics().getTraceId(), receiver, p.getSource(), p.getHops(), location, result, publicationWriter, pubTraceCounts);
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
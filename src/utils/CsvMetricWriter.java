package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.metrics.EventMetrics;
import simulator.regions.SpatialRegion;

public class CsvMetricWriter {

    public interface TraceMetricStrategy {
        String getSubscriptionHeader();

        String formatBrokerSubscriptionEvent(Object... args);

        String formatSubscriberSubscriptionEvent(double x, double y);

        String getPublicationHeader();

        String formatBrokerPublicationEvent(Object... args);

        String formatSubscriberPublicationEvent(SimulationPublication p, double subX, double subY, double distSq);
    }

    // --- Strategy 1: Regional / Spatial ---
    public static class RegionTraceStrategy implements TraceMetricStrategy {
        @Override
        public String getSubscriptionHeader() {
            // Renamed headers for clarity
            return "TraceID,Seq,ReceivedFrom,ProcessingNode,Hops,IncomingRegion,BrokerRegion,Details,Op\n";
        }

        @Override
        public String formatBrokerSubscriptionEvent(Object... args) {
            String incoming = (args.length > 0 && args[0] instanceof SpatialRegion r) ? r.toLogString() : "";
            String broker = (args.length > 1 && args[1] instanceof SpatialRegion r) ? r.toLogString() : "";
            String detail = (args.length > 2) ? String.valueOf(args[2]) : "";
            return String.format("\"%s\",\"%s\",\"%s\"", incoming, broker, detail);
        }

        @Override
        public String formatSubscriberSubscriptionEvent(double x, double y) {
            return String.format("\"(%.4f, %.4f)\",\"\",\"\"", x, y);
        }

        @Override
        public String getPublicationHeader() {
            // Renamed Source -> ReceivedFrom, Receiver -> ProcessingNode
            return "TraceID,MsgCount,ReceivedFrom,ProcessingNode,Hops,RegionOrPub,SubLocation,Result\n";
        }

        @Override
        public String formatBrokerPublicationEvent(Object... args) {
            String reg = (args.length > 0 && args[0] instanceof SpatialRegion r) ? r.toLogString() : "";
            return String.format("\"%s\",\"\"", reg);
        }

        @Override
        public String formatSubscriberPublicationEvent(SimulationPublication p, double subX, double subY,
                double distSq) {
            String pLoc = "";
            if (p instanceof PublicationWithLocation pwl) {
                Location l = pwl.getLocation();
                pLoc = String.format("(%.4f, %.4f)", l.getX(), l.getY());
            }
            String sLoc = String.format("(%.4f, %.4f)", subX, subY);
            return String.format("\"Pub:%s\",\"Sub:%s\"", pLoc, sLoc);
        }
    }

    // --- Strategy 2: Proximity / Location ---
    public static class ProximityTraceStrategy implements TraceMetricStrategy {
        @Override
        public String getSubscriptionHeader() {
            // Renamed headers for clarity
            return "TraceID,Seq,ReceivedFrom,ProcessingNode,Hops,Location,Decision,Op\n";
        }

        @Override
        public String formatBrokerSubscriptionEvent(Object... args) {
            String locStr = "";
            if (args.length > 0 && args[0] instanceof Location l) {
                locStr = String.format("(%.4f, %.4f)", l.getX(), l.getY());
            }
            String decision = (args.length > 1) ? String.valueOf(args[1]) : "";
            return String.format("\"%s\",\"%s\"", locStr, decision);
        }

        @Override
        public String formatSubscriberSubscriptionEvent(double x, double y) {
            return String.format("\"(%.4f, %.4f)\",\"\"", x, y);
        }

        @Override
        public String getPublicationHeader() {
            // Renamed Source -> ReceivedFrom, Receiver -> ProcessingNode
            return "TraceID,MsgCount,ReceivedFrom,ProcessingNode,Hops,Distance,UpdateStats,Result\n";
        }

        @Override
        public String formatBrokerPublicationEvent(Object... args) {
            double distSq = (args.length > 0 && args[0] instanceof Double d) ? d : Double.MAX_VALUE;
            int actual = (args.length > 1 && args[1] instanceof Integer i) ? i : 0;
            int potential = (args.length > 2 && args[2] instanceof Integer i) ? i : 0;

            String distStr = (distSq < Double.MAX_VALUE) ? String.format("MinDist:%.0fkm", Math.sqrt(distSq) * 111.1)
                    : "";
            String statsStr = (potential > 0) ? String.format("Upd:%d/%d", actual, potential) : "";

            return String.format("\"%s\",\"%s\"", distStr, statsStr);
        }

        @Override
        public String formatSubscriberPublicationEvent(SimulationPublication p, double subX, double subY,
                double distSq) {
            String distStr = (distSq < Double.MAX_VALUE) ? String.format("%.0fkm", Math.sqrt(distSq) * 111.1) : "N/A";
            return String.format("\"%s\",\"\"", distStr);
        }
    }

    private static CsvMetricWriter instance;
    private TraceMetricStrategy traceStrategy;

    // --- Defaults ensure safety in tests ---
    private boolean initialized = false;
    private boolean eventTracingEnabled = false;

    private RotatingFileWriter subscriptionWriter;
    private RotatingFileWriter publicationWriter;
    private BufferedWriter subSummaryWriter;
    private BufferedWriter pubSummaryWriter;

    private final Map<Long, Integer> subTraceCounts = new HashMap<>();
    private final Map<Long, Integer> pubTraceCounts = new HashMap<>();

    private Long currentSubTraceId = null;
    private SummaryEntry currentSubSummary = null;

    private Long currentPubTraceId = null;
    private SummaryEntry currentPubSummary = null;

    private static final long MAX_FILE_SIZE_BYTES = 90 * 1024 * 1024;

    private CsvMetricWriter() {
    }

    public static synchronized CsvMetricWriter getInstance() {
        if (instance == null)
            instance = new CsvMetricWriter();
        return instance;
    }

    public synchronized void initialize(String runId, boolean enableTracing, TraceMetricStrategy strategy) {
        // Reset state
        close();

        this.eventTracingEnabled = enableTracing;

        // --- If disabled, mark initialized and EXIT. ---
        if (!this.eventTracingEnabled) {
            this.initialized = true;
            return;
        }

        // Only create files if actually enabled
        try {
            this.traceStrategy = (strategy != null) ? strategy : new RegionTraceStrategy();

            subTraceCounts.clear();
            pubTraceCounts.clear();
            currentSubTraceId = null;
            currentSubSummary = null;
            currentPubTraceId = null;
            currentPubSummary = null;

            String outputRoot = SimConfiguration.get().paths.outputDir;
            if (outputRoot == null)
                outputRoot = "experiment-results"; // Safety fallback
            if (!outputRoot.endsWith("/"))
                outputRoot += "/";
            String baseDir = outputRoot + runId;
            new File(baseDir).mkdirs();

            String subDir = baseDir + "/subscriptions";
            String pubDir = baseDir + "/publications";

            new File(subDir).mkdirs();
            new File(pubDir).mkdirs();

            subscriptionWriter = new RotatingFileWriter(subDir, "subscriptions",
                    this.traceStrategy.getSubscriptionHeader());
            subSummaryWriter = initializeSummaryWriter(subDir, "subscription_summary.csv");

            publicationWriter = new RotatingFileWriter(pubDir, "publications",
                    this.traceStrategy.getPublicationHeader());
            pubSummaryWriter = initializeSummaryWriter(pubDir, "publication_summary.csv");

            initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
            // If IO fails, disable tracing to prevent subsequent crashes
            this.eventTracingEnabled = false;
        }
    }

    public void initialize(String runId, boolean enableTracing) {
        initialize(runId, enableTracing, new RegionTraceStrategy());
    }

    // --- LOGGING METHODS (Now Guarded) ---

    public void logSubscription(SimulationSubscription s, String receiver, String result, Object... args) {
        // Guard clause: If disabled, uninitialized, or strategy missing -> Do nothing
        if (!initialized || !eventTracingEnabled || traceStrategy == null || s.getMetrics() == null)
            return;

        String formattedData = traceStrategy.formatBrokerSubscriptionEvent(args);
        writeSubscriptionLine(s, receiver, formattedData, result);
    }

    public void logSubscriberEvent(SimulationSubscription s, String receiver, double x, double y, String result) {
        if (!initialized || !eventTracingEnabled || traceStrategy == null || s.getMetrics() == null)
            return;

        String formattedData = traceStrategy.formatSubscriberSubscriptionEvent(x, y);
        writeSubscriptionLine(s, receiver, formattedData, result);
    }

    public void logPublication(SimulationPublication p, String receiver, String result, Object... args) {
        if (!initialized || !eventTracingEnabled || traceStrategy == null || p.getMetrics() == null)
            return;

        String formattedData = traceStrategy.formatBrokerPublicationEvent(args);
        writePublicationLine(p, receiver, formattedData, result);
    }

    public void logSubscriberPublicationEvent(SimulationPublication p, String receiver, String result, double subX,
            double subY, double distSq) {
        if (!initialized || !eventTracingEnabled || traceStrategy == null || p.getMetrics() == null)
            return;

        String formattedData = traceStrategy.formatSubscriberPublicationEvent(p, subX, subY, distSq);
        writePublicationLine(p, receiver, formattedData, result);
    }

    // --- Writers ---

    private synchronized void writeSubscriptionLine(SimulationSubscription s, String receiver, String formattedData,
            String result) {
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

        if (metrics.getOriginalSourceName() != null)
            currentSubSummary.sourceName = metrics.getOriginalSourceName();
        if (metrics.getOriginalCountry() != null)
            currentSubSummary.country = metrics.getOriginalCountry();
        if (metrics.getOriginalLongitude() != null)
            currentSubSummary.longitude = metrics.getOriginalLongitude();
        if (metrics.getOriginalLatitude() != null)
            currentSubSummary.latitude = metrics.getOriginalLatitude();

        try {
            int count = subTraceCounts.getOrDefault(traceId, 0) + 1;
            subTraceCounts.put(traceId, count);
            // Source logic here is for the "ReceivedFrom" column
            String sourceName = (s.getSource() != null) ? s.getSource().getName() : "null";
            String idStr = formatTraceId(traceId);

            subscriptionWriter.write(String.format("%s,%d,\"%s\",\"%s\",%d,%s,%s\n",
                    idStr, count, sourceName, receiver, s.getHops(), formattedData, result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void writePublicationLine(SimulationPublication p, String receiver, String formattedData,
            String result) {
        EventMetrics metrics = p.getMetrics();
        long traceId = metrics.getTraceId();

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

        if (metrics.getOriginalSourceName() != null)
            currentPubSummary.sourceName = metrics.getOriginalSourceName();
        if (metrics.getOriginalCountry() != null)
            currentPubSummary.country = metrics.getOriginalCountry();
        if (metrics.getOriginalLongitude() != null)
            currentPubSummary.longitude = metrics.getOriginalLongitude();
        if (metrics.getOriginalLatitude() != null)
            currentPubSummary.latitude = metrics.getOriginalLatitude();

        if (currentPubSummary.sourceName == null && p.getSource() != null) {
            currentPubSummary.sourceName = p.getSource().getName();
        }

        try {
            int count = pubTraceCounts.getOrDefault(traceId, 0) + 1;
            pubTraceCounts.put(traceId, count);
            // Source logic here is for the "ReceivedFrom" column
            String sourceName = (p.getSource() != null) ? p.getSource().getName() : "null";
            String idStr = formatTraceId(traceId);

            publicationWriter.write(String.format("%s,%d,\"%s\",\"%s\",%d,%s,%s\n",
                    idStr, count, sourceName, receiver, p.getHops(), formattedData, result));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        if (writer == null || summary == null)
            return;
        try {
            int count = (maxCount != null) ? maxCount : 0;
            String countryStr = (summary.country != null) ? summary.country : "Unknown";
            String sourceStr = (summary.sourceName != null) ? summary.sourceName : "Unknown";
            String idStr = formatTraceId(summary.id);
            writer.write(String.format("%s,\"%s\",\"%s\",%.4f,%.4f,%d\n", idStr, sourceStr, countryStr,
                    summary.longitude, summary.latitude, count));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (eventTracingEnabled) {
                if (currentSubTraceId != null)
                    flushSummary(subSummaryWriter, currentSubSummary, subTraceCounts.get(currentSubTraceId));
                if (subscriptionWriter != null)
                    subscriptionWriter.close();
                if (subSummaryWriter != null)
                    subSummaryWriter.close();
                if (currentPubTraceId != null)
                    flushSummary(pubSummaryWriter, currentPubSummary, pubTraceCounts.get(currentPubTraceId));
                if (publicationWriter != null)
                    publicationWriter.close();
                if (pubSummaryWriter != null)
                    pubSummaryWriter.close();
            }
            subTraceCounts.clear();
            pubTraceCounts.clear();
            initialized = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class SummaryEntry {
        long id;
        String sourceName;
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
            if (currentWriter != null)
                currentWriter.close();
            File file = new File(dirPath, baseName + "_" + fileIndex + ".csv");
            currentWriter = new BufferedWriter(new FileWriter(file));
            currentWriter.write(header);
            currentWriter.flush();
            currentBytes = header.length();
            fileIndex++;
        }

        public void write(String line) throws IOException {
            if (currentBytes + line.length() > MAX_FILE_SIZE_BYTES)
                rotate();
            currentWriter.write(line);
            currentWriter.flush();
            currentBytes += line.length();
        }

        public void close() throws IOException {
            if (currentWriter != null)
                currentWriter.close();
        }
    }
}
package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;
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
        String formatBrokerSubscriptionEvent(SimulationSubscription sub, Object... args);
        String formatSubscriberSubscriptionEvent(SimulationSubscription sub, double x, double y);

        String getPublicationHeader();
        String formatBrokerPublicationEvent(SimulationPublication pub, Object... args);
        String formatSubscriberPublicationEvent(SimulationPublication p, double subX, double subY, double distSq);
    }

    // --- Strategy 1: Regional / Spatial ---
    public static class RegionTraceStrategy implements TraceMetricStrategy {
        @Override
        public String getSubscriptionHeader() {
            return "TraceID,Seq,ReceivedFrom,ProcessingNode,Hops,IncomingRegion,BrokerRegion,Stage,Result\n";
        }
        @Override
        public String formatBrokerSubscriptionEvent(SimulationSubscription sub, Object... args) {
            String incoming = (args.length > 0 && args[0] instanceof SpatialRegion r) ? r.toLogString() : "";
            String broker = (args.length > 1 && args[1] instanceof SpatialRegion r) ? r.toLogString() : "";
            String stage = (args.length > 2) ? String.valueOf(args[2]) : "";
            return "\"" + incoming + "\",\"" + broker + "\",\"" + stage + "\"";
        }
        @Override
        public String getPublicationHeader() {
            return "TraceID,MsgCount,ReceivedFrom,ProcessingNode,Hops,RegionOrPub,Result\n";
        }
        @Override
        public String formatBrokerPublicationEvent(SimulationPublication pub, Object... args) {
            String reg = (args.length > 0 && args[0] instanceof SpatialRegion r) ? r.toLogString() : "";
            return "\"" + reg + "\"";
        }
        @Override
        public String formatSubscriberPublicationEvent(SimulationPublication p, double subX, double subY, double distSq) {
            String pLoc = "";
            if (p instanceof PublicationWithLocation pwl) {
                Location l = pwl.getLocation();
                pLoc = "(" + String.format("%.4f", l.getX()) + ", " + String.format("%.4f", l.getY()) + ")";
            }
            String sLoc = "(" + String.format("%.4f", subX) + ", " + String.format("%.4f", subY) + ")";
            return "\"Pub:" + pLoc + "\",\"Sub:" + sLoc + "\"";
        }
        @Override
        public String formatSubscriberSubscriptionEvent(SimulationSubscription sub, double x, double y) {
            return "\"( " + String.format("%.4f", x) + ", " + String.format("%.4f", y) + ")\",\"\",\"\"";
        }
    }

    // --- Strategy 2: Proximity ---
    public static class ProximityTraceStrategy implements TraceMetricStrategy {
        @Override
        public String getSubscriptionHeader() {
            return "TraceID,Seq,ReceivedFrom,ProcessingNode,Hops,Location,Stage,Result\n";
        }
        @Override
        public String formatBrokerSubscriptionEvent(SimulationSubscription sub, Object... args) {
            String locStr = "";
            if (args.length > 0 && args[0] instanceof Location l) {
                locStr = String.format("(%.4f, %.4f)", l.getX(), l.getY());
            }
            String stage = (args.length > 1) ? String.valueOf(args[1]) : "";
            return "\"" + locStr + "\",\"" + stage + "\"";
        }
        @Override
        public String getPublicationHeader() {
            return "TraceID,MsgCount,ReceivedFrom,ProcessingNode,Hops,Distance,UpdateStats,Result\n";
        }
        @Override
        public String formatBrokerPublicationEvent(SimulationPublication pub, Object... args) {
            double distSq = (args.length > 0 && args[0] instanceof Double d) ? d : Double.MAX_VALUE;
            int actual = (args.length > 1 && args[1] instanceof Integer i) ? i : 0;
            int potential = (args.length > 2 && args[2] instanceof Integer i) ? i : 0;
            String distStr = (distSq < Double.MAX_VALUE) ? String.format("MinDist:%.0fkm", Math.sqrt(distSq) * 111.1) : "";
            String statsStr = (potential > 0) ? "Upd:" + actual + "/" + potential : "";
            return "\"" + distStr + "\",\"" + statsStr + "\"";
        }
        @Override
        public String formatSubscriberPublicationEvent(SimulationPublication p, double subX, double subY, double distSq) {
            String distStr = (distSq < Double.MAX_VALUE) ? String.format("%.0fkm", Math.sqrt(distSq) * 111.1) : "N/A";
            return "\"" + distStr + "\",\"\"";
        }
        @Override
        public String formatSubscriberSubscriptionEvent(SimulationSubscription sub, double x, double y) {
            return "\"(" + String.format("%.4f", x) + ", " + String.format("%.4f", y) + ")\",\"\"";
        }
    }

    public static class MarketplaceTraceStrategy implements TraceMetricStrategy {
        @Override
        public String getSubscriptionHeader() {
            return "TraceID,Seq,ReceivedFrom,ProcessingNode,Hops,ServiceID,IncomingState,ExistingState,ResultingState,Result\n";
        }

        @Override
        public String formatBrokerSubscriptionEvent(SimulationSubscription sub, Object... args) {
            String serviceId = "N/A";
            if (sub instanceof ServiceOffer offer) {
                serviceId = String.valueOf(offer.getOracleServiceId());
            }

            // Extract the 3 chronological states passed from the Broker
            String incoming = (args.length > 0 && args[0] != null && args[0] instanceof SpatialRegion r) ? r.toLogString().replace(",", ";") : "None";
            String existing = (args.length > 3 && args[3] != null && args[3] instanceof SpatialRegion r) ? r.toLogString().replace(",", ";") : "None";
            String resulting = (args.length > 4 && args[4] != null && args[4] instanceof SpatialRegion r) ? r.toLogString().replace(",", ";") : "None";

            return "\"" + serviceId + "\",\"" + incoming + "\",\"" + existing + "\",\"" + resulting + "\"";
        }

        @Override
        public String formatSubscriberSubscriptionEvent(SimulationSubscription sub, double x, double y) {
            String serviceId = "N/A";
            String incomingState = "[]";
            
            // For the initial hop, IncomingState shows pure hardware capabilities and GPS point
            if (sub instanceof marketplace.events.ServiceOffer offer) {
                serviceId = String.valueOf(offer.getOracleServiceId());
                incomingState = offer.toDisplayString().replace(",", ";") + " (" + String.format("%.4f", x) + "; " + String.format("%.4f", y) + ")";
            }
            
            // Existing and Resulting are inherently blank for the physical node's origin event
            return "\"" + serviceId + "\",\"" + incomingState + "\",\"None\",\"None\"";
        }

        @Override
        public String getPublicationHeader() {
            return "TraceID,MsgCount,ReceivedFrom,ProcessingNode,Hops,ServiceID,DecisionDetails,Result\n";
        }

        @Override
        public String formatBrokerPublicationEvent(SimulationPublication pub, Object... args) {
            String serviceId = "N/A";
            if (pub instanceof ServiceRequest req) {
                serviceId = String.valueOf(req.getOracleServiceId());
            }

            String details = (args.length > 0) ? String.valueOf(args[0]).replace(",", ";") : "";

            return "\"" + serviceId + "\",\"" + details + "\"";
        }

        @Override
        public String formatSubscriberPublicationEvent(SimulationPublication p, double subX, double subY, double distSq) {
            String serviceId = (p instanceof ServiceRequest req) ? String.valueOf(req.getOracleServiceId()) : "N/A";
            return "\"" + serviceId + "\",\"DELIVERED TO SUB at (" + String.format("%.4f", subX) + ";" + String.format("%.4f", subY) + ")\"";
        }
    }

    // --- Singleton & Writer Logic ---
    private static CsvMetricWriter instance;
    private TraceMetricStrategy traceStrategy;
    private boolean initialized = false;
    private boolean eventTracingEnabled = false;
    
    private RotatingFileWriter subscriptionWriter;
    private RotatingFileWriter publicationWriter;
    private BufferedWriter subSummaryWriter;
    private BufferedWriter pubSummaryWriter;
    private BufferedWriter groundTruthWriter;
    
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
        close();
        this.eventTracingEnabled = enableTracing;
        if (!this.eventTracingEnabled) {
            this.initialized = true;
            return;
        }
        try {
            this.traceStrategy = (strategy != null) ? strategy : new RegionTraceStrategy();
            
            subTraceCounts.clear(); pubTraceCounts.clear();
            currentSubTraceId = null; currentSubSummary = null;
            currentPubTraceId = null; currentPubSummary = null;

            String outputRoot = SimConfiguration.get().paths.outputDir;
            if (outputRoot == null) outputRoot = "output";
            if (!outputRoot.endsWith("/")) outputRoot += "/";
            String baseDir = outputRoot + runId;
            new File(baseDir).mkdirs();
            new File(baseDir + "/subscriptions").mkdirs();
            new File(baseDir + "/publications").mkdirs();

            subscriptionWriter = new RotatingFileWriter(baseDir + "/subscriptions", "subscriptions", this.traceStrategy.getSubscriptionHeader());
            subSummaryWriter = initializeSummaryWriter(baseDir + "/subscriptions", "subscription_summary.csv");

            publicationWriter = new RotatingFileWriter(baseDir + "/publications", "publications", this.traceStrategy.getPublicationHeader());
            pubSummaryWriter = initializeSummaryWriter(baseDir + "/publications", "publication_summary.csv");

            // [UPDATED] Added Distance Column
            groundTruthWriter = new BufferedWriter(new FileWriter(new File(baseDir, "ground_truth.csv")));
            groundTruthWriter.write("RequestID,ServiceID,OptimalProvider,Score,ExactDistance\n");
            
            initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
            this.eventTracingEnabled = false;
        }
    }
    
    public void initialize(String runId, boolean enableTracing) {
        initialize(runId, enableTracing, new RegionTraceStrategy());
    }
    
    // [UPDATED] Method to log ground truth events with Distance
    public synchronized void logGroundTruth(long requestId, long serviceId, String providerName, double score, double distance) {
        if (!initialized || groundTruthWriter == null) return;
        try {
            groundTruthWriter.write(String.format("%d,%d,\"%s\",%.6f,%.6f%n", requestId, serviceId, providerName, score, distance));
            groundTruthWriter.flush(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logSubscription(SimulationSubscription s, String receiver, String result, Object... args) {
        if (!initialized || !eventTracingEnabled || traceStrategy == null || s.getMetrics() == null) return;
        
        String finalResult = result;
        // The telemetry string is passed as args[2] from SpatialMatchBroker
        if (args.length > 2 && args[2] != null) {
            String additionalInfo = String.valueOf(args[2]);
            if (additionalInfo.startsWith("ADDED [") || additionalInfo.startsWith("EXPANDED") || additionalInfo.startsWith("Tier Isolation") || additionalInfo.startsWith("Filtered")) {
                finalResult = additionalInfo; 
            }
        }
        
        String formattedData = traceStrategy.formatBrokerSubscriptionEvent(s, args);
        writeSubscriptionLine(s, receiver, formattedData, finalResult);
    }

    public void logPublication(SimulationPublication p, String receiver, String result, Object... args) {
        if (!initialized || !eventTracingEnabled || traceStrategy == null || p.getMetrics() == null) return;
        String formattedData = traceStrategy.formatBrokerPublicationEvent(p, args);
        writePublicationLine(p, receiver, formattedData, result);
    }
    
    public void logSubscriberEvent(SimulationSubscription s, String receiver, double x, double y, String result) {
        if (!initialized || !eventTracingEnabled || traceStrategy == null || s.getMetrics() == null) return;
        String formattedData = traceStrategy.formatSubscriberSubscriptionEvent(s, x, y);
        writeSubscriptionLine(s, receiver, formattedData, result);
    }

    public void logSubscriberPublicationEvent(SimulationPublication p, String receiver, String result, double subX, double subY, double distSq) {
        if (!initialized || !eventTracingEnabled || traceStrategy == null || p.getMetrics() == null) return;
        String formattedData = traceStrategy.formatSubscriberPublicationEvent(p, subX, subY, distSq);
        writePublicationLine(p, receiver, formattedData, result);
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
            if (s instanceof marketplace.events.ServiceOffer offer) {
                currentSubSummary.serviceId = String.valueOf(offer.getOracleServiceId());
            }
        }

        if (metrics.getOriginalSourceName() != null) currentSubSummary.sourceName = metrics.getOriginalSourceName();
        if (metrics.getOriginalCountry() != null) currentSubSummary.country = metrics.getOriginalCountry();
        if (metrics.getOriginalLongitude() != null) currentSubSummary.longitude = metrics.getOriginalLongitude();
        if (metrics.getOriginalLatitude() != null) currentSubSummary.latitude = metrics.getOriginalLatitude();

        try {
            int count = subTraceCounts.getOrDefault(traceId, 0) + 1;
            subTraceCounts.put(traceId, count);
            String sourceName = (s.getSource() != null) ? s.getSource().getName() : "null";
            StringBuilder sb = new StringBuilder(128);
            sb.append(formatTraceId(traceId)).append(',')
              .append(count).append(',')
              .append('"').append(sourceName).append("\",")
              .append('"').append(receiver).append("\",")
              .append(s.getHops()).append(',')
              .append(formattedData).append(',')
              .append('"').append(result).append("\"\n");
            subscriptionWriter.write(sb.toString());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private synchronized void writePublicationLine(SimulationPublication p, String receiver, String formattedData, String result) {
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
            if (p instanceof marketplace.events.ServiceRequest req) {
                currentPubSummary.serviceId = String.valueOf(req.getOracleServiceId());
            }
        }

        if (metrics.getOriginalSourceName() != null) currentPubSummary.sourceName = metrics.getOriginalSourceName();
        if (metrics.getOriginalCountry() != null) currentPubSummary.country = metrics.getOriginalCountry();
        if (metrics.getOriginalLongitude() != null) currentPubSummary.longitude = metrics.getOriginalLongitude();
        if (metrics.getOriginalLatitude() != null) currentPubSummary.latitude = metrics.getOriginalLatitude();
        if (currentPubSummary.sourceName == null && p.getSource() != null) currentPubSummary.sourceName = p.getSource().getName();

        try {
            int count = pubTraceCounts.getOrDefault(traceId, 0) + 1;
            pubTraceCounts.put(traceId, count);
            String sourceName = (p.getSource() != null) ? p.getSource().getName() : "null";
            StringBuilder sb = new StringBuilder(128);
            sb.append(formatTraceId(traceId)).append(',')
              .append(count).append(',')
              .append('"').append(sourceName).append("\",")
              .append('"').append(receiver).append("\",")
              .append(p.getHops()).append(',')
              .append(formattedData).append(',')
              .append('"').append(result).append("\"\n");
            publicationWriter.write(sb.toString());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private BufferedWriter initializeSummaryWriter(String dir, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir, filename)));
        writer.write("trace_id,service_id,source,country,long,lat,max_row_count\n");
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
            String serviceIdStr = (summary.serviceId != null) ? summary.serviceId : "N/A";
            String countryStr = (summary.country != null) ? summary.country : "Unknown";
            String sourceStr = (summary.sourceName != null) ? summary.sourceName : "Unknown";
            StringBuilder sb = new StringBuilder(128);
            sb.append(formatTraceId(summary.id)).append(',')
              .append('"').append(serviceIdStr).append("\",")
              .append('"').append(sourceStr).append("\",")
              .append('"').append(countryStr).append("\",")
              .append(String.format("%.4f", summary.longitude)).append(',')
              .append(String.format("%.4f", summary.latitude)).append(',')
              .append(count).append('\n');
            writer.write(sb.toString());
            writer.flush();
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
                
                // [UPDATED] Close Ground Truth Writer
                if (groundTruthWriter != null) groundTruthWriter.close();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
    private static class SummaryEntry { long id; String serviceId; String sourceName; String country; Double longitude; Double latitude; }
    private static class RotatingFileWriter {
        private final String baseDir; private final String baseFilename; private final String header;
        private BufferedWriter writer; private File currentFile; private int fileIndex = 0;
        public RotatingFileWriter(String baseDir, String baseFilename, String header) throws IOException {
            this.baseDir = baseDir; this.baseFilename = baseFilename; this.header = header;
            rotate();
        }
        public void write(String content) throws IOException {
            if (currentFile.length() >= MAX_FILE_SIZE_BYTES) rotate();
            writer.write(content);
        }
        private void rotate() throws IOException {
            if (writer != null) writer.close();
            fileIndex++;
            currentFile = new File(baseDir, baseFilename + "_" + fileIndex + ".csv");
            writer = new BufferedWriter(new FileWriter(currentFile));
            writer.write(header);
        }
        public void close() throws IOException { if (writer != null) writer.close(); }
    }
}
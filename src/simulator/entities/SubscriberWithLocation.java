package simulator.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.metrics.EventMetrics;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public class SubscriberWithLocation extends TreeNode {

    private static final Logger logger = CustomLogger.getLogger(SubscriberWithLocation.class.getName());
    private static final AtomicInteger SUBSCRIBER_ID_GENERATOR = new AtomicInteger(1);

    private final int mySubscriberId; 
    private float[] regionCoords = null; 
    private int activeRegionCount = 0;
    
    private final float myLat;
    private final float myLon;
    
    // Cached origin country
    private String myCountry = null;
    
    // Polymorphic Strategy for Logging (initialized lazily)
    private SubscriberTraceStrategy traceStrategy;

    // Metrics
    private int falsePositiveDeliveries = 0;
    private int nSubscriptions = 0; 
    private int nPublications = 0;
    private long hopSum = 0;
    private int hopCount = 0;
    private int hopMin = Integer.MAX_VALUE;
    private int hopMax = Integer.MIN_VALUE;

    public SubscriberWithLocation(String name, Location location) {
        this(SUBSCRIBER_ID_GENERATOR.getAndIncrement(), name, location);
    }

    public SubscriberWithLocation(Location location) {
        this(SUBSCRIBER_ID_GENERATOR.getAndIncrement(), null, location);
    }

    private SubscriberWithLocation(int id, String explicitName, Location location) {
        super(resolveName(id, explicitName));
        this.mySubscriberId = id;
        this.myLon = (float) location.getX();
        this.myLat = (float) location.getY();
    }
    
    private static String resolveName(int id, String explicitName) {
        if (explicitName != null) return explicitName;
        if (SimConfiguration.get().paths.enableEventTracing) return "Sub-" + id;
        return null; 
    }
    
    @Override
    public String getName() {
        String storedName = super.getName();
        if (storedName != null) return storedName;
        // Fallback for debug/logging if needed, but not stored permanently in TreeNode
        return "Sub-" + mySubscriberId;
    }

    public String getFormattedLocation() {
        return String.format("(%.4f, %.4f)", this.myLon, this.myLat);
    }

    public void receive(SimulationPublication p) {
        nPublications++;

        if (p instanceof PublicationWithLocation pub) {
            boolean matchesInterest = checkRegionInterest(pub);
            if (!matchesInterest) falsePositiveDeliveries++;

            if (SimConfiguration.get().paths.enableEventTracing && p.getMetrics() != null) {
                if (traceStrategy == null) {
                    resolveTraceStrategy();
                }
                traceStrategy.trace(this, pub, matchesInterest);
            }

            int hops = p.getHops();
            synchronized(this) {
                hopSum += hops;
                hopCount++;
                if (hops < hopMin) hopMin = hops;
                if (hops > hopMax) hopMax = hops;
            }
        }
    }

    private void resolveTraceStrategy() {
        SimulationBroker broker = getBroker();
        if (broker != null && broker.getClass().getSimpleName().contains("Proximity")) {
            this.traceStrategy = new ProximityTraceStrategy();
        } else {
            this.traceStrategy = RegionTraceStrategy.INSTANCE;
        }
    }

    private boolean checkRegionInterest(PublicationWithLocation pub) {
        if (activeRegionCount == 0) return true; 
        if (regionCoords == null) return false;

        float pubLon = (float) pub.getLocation().getX();
        float pubLat = (float) pub.getLocation().getY();

        for (int i = 0; i < activeRegionCount; i++) {
            int offset = i * 4;
            if (Region.fastContains(
                    regionCoords[offset], regionCoords[offset+1], 
                    regionCoords[offset+2], regionCoords[offset+3], 
                    pubLon, pubLat)) {
                return true;
            }
        }
        return false;
    }

    public void send(SimulationSubscription s) {
        SimulationBroker broker = getBroker();
        if (broker == null) {
            logger.severe(getName() + ": topology error, no broker");
            return;
        }

        s.setSource(this);
        
        if (SimConfiguration.get().paths.enableEventTracing) {
            long seqId = this.nSubscriptions + 1;
            long structuredTraceId = ((long) this.mySubscriberId << 32) | (seqId & 0xFFFFFFFFL);
            
            EventMetrics metrics = new EventMetrics(structuredTraceId);
            
            if (myCountry == null) {
                myCountry = resolveCountry();
            }
            
            metrics.setOriginalSourceInfo(getName(), myCountry, (double)myLon, (double)myLat);
            s.setMetrics(metrics);
            
            CsvMetricWriter.getInstance().logSubscription(
                s,
                broker.getName(),
                "Created at " + getFormattedLocation(),
                "SENT"
            );
        }

        if (s instanceof SubscriptionWithRegion swr) {
            ensureRegionCapacity();
            int offset = activeRegionCount * 4;
            regionCoords[offset]   = swr.getMinLon();
            regionCoords[offset+1] = swr.getMaxLon();
            regionCoords[offset+2] = swr.getMinLat();
            regionCoords[offset+3] = swr.getMaxLat();
            activeRegionCount++;
        }

        broker.processSubscription(s);
        nSubscriptions++;
    }

    private String resolveCountry() {
        List<TreeNode> path = new ArrayList<>();
        TreeNode current = this;
        while (current != null) { 
            path.add(current); 
            current = current.getParent(); 
        }
        if (path.size() >= 3) {
            return path.get(path.size() - 3).getName();
        }
        if (path.size() >= 2) {
            return path.get(path.size() - 2).getName();
        }
        return "Unknown";
    }

    private void ensureRegionCapacity() {
        if (regionCoords == null) {
            regionCoords = new float[4];
        } else if (activeRegionCount * 4 >= regionCoords.length) {
            float[] newArr = new float[regionCoords.length + 16];
            System.arraycopy(regionCoords, 0, newArr, 0, regionCoords.length);
            regionCoords = newArr;
        }
    }

    public int getnPublications() { return nPublications; }
    public int getnSubscriptions() { return nSubscriptions; }
    public int getFalsePositiveDeliveries() { return falsePositiveDeliveries; }
    public int getHopCount() { return hopCount; }
    public long getHopSum() { return hopSum; }
    public int getHopMin() { return hopMin; }
    public int getHopMax() { return hopMax; }
    @Override public Location getMetricLocation() { return new Location(myLon, myLat, -1); }
    public Location getLocation() { return getMetricLocation(); }
    public SimulationBroker getBroker() { return (getParent() instanceof SimulationBroker) ? (SimulationBroker) getParent() : null; }

    // ==========================================================
    // STRATEGY PATTERN IMPLEMENTATION
    // ==========================================================

    private interface SubscriberTraceStrategy {
        void trace(SubscriberWithLocation sub, PublicationWithLocation pub, boolean matchesInterest);
    }

    private static class RegionTraceStrategy implements SubscriberTraceStrategy {
        static final RegionTraceStrategy INSTANCE = new RegionTraceStrategy();

        @Override
        public void trace(SubscriberWithLocation sub, PublicationWithLocation pub, boolean matchesInterest) {
            String logLocation = String.format("Pub:%s -> Sub:%s", 
                pub.getLocation().toString(), 
                sub.getFormattedLocation()
            );
            
            String result = matchesInterest ? "Delivered" : "FalsePositive";
            
            CsvMetricWriter.getInstance().logPublication(
                pub,
                sub.getName(),
                logLocation,
                result
            );
        }
    }

    private static class ProximityTraceStrategy implements SubscriberTraceStrategy {
        private double bestDistanceSqSoFar = Double.MAX_VALUE;

        @Override
        public void trace(SubscriberWithLocation sub, PublicationWithLocation pub, boolean matchesInterest) {
            double currentDistSq = pub.getCachedDistanceSquared();
            if (currentDistSq < 0) {
                currentDistSq = sub.getLocation().distanceSquared(pub.getLocation());
            }

            String status;
            if (!matchesInterest) {
                status = "UNWANTED";
            } else if (bestDistanceSqSoFar == Double.MAX_VALUE) {
                status = "NEW";
                bestDistanceSqSoFar = currentDistSq;
            } else if (currentDistSq < bestDistanceSqSoFar) {
                status = "UPDATE";
                bestDistanceSqSoFar = currentDistSq;
            } else {
                status = "NO_UPDATE";
            }

            double approxKm = Math.sqrt(currentDistSq) * 111.1;
            String distancePayload = String.format("%.0fkm", approxKm);

            CsvMetricWriter.getInstance().logPublication(
                pub,
                sub.getName(),
                distancePayload,
                status
            );
        }
    }
}
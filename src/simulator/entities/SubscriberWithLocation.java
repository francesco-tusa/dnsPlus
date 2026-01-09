package simulator.entities;

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

    // --- INSTANCE FIELDS ---
    private final int mySubscriberId; 
    
    private float[] regionCoords = null; 
    private int activeRegionCount = 0;
    
    private final float myLat;
    private final float myLon;
    
    private float lastPubLon = Float.NaN;
    private float lastPubLat = Float.NaN;
    private Location lastReceivedPubLocation = null;

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
        if (SimConfiguration.get().paths.enableSubscriptionTracing) return "Sub-" + id;
        return null;
    }
    
    @Override
    public String getName() {
        String storedName = super.getName();
        if (storedName != null) {
            return storedName;
        }
        return "Sub-" + mySubscriberId;
    }

    public String getFormattedLocation() {
        return String.format("(%.4f, %.4f)", this.myLon, this.myLat);
    }

    public void receive(SimulationPublication p) {
        nPublications++;

        if (p instanceof PublicationWithLocation pub) {
            float pubLon = (float) pub.getLocation().getX();
            float pubLat = (float) pub.getLocation().getY();

            boolean matchesInterest = false;
            if (regionCoords != null) {
                for (int i = 0; i < activeRegionCount; i++) {
                    int offset = i * 4;
                    if (Region.fastContains(
                            regionCoords[offset], regionCoords[offset+1], 
                            regionCoords[offset+2], regionCoords[offset+3], 
                            pubLon, pubLat)) {
                        matchesInterest = true;
                        break;
                    }
                }
                if (!matchesInterest) falsePositiveDeliveries++;
            }

            this.lastReceivedPubLocation = pub.getLocation();
            this.lastPubLon = pubLon;
            this.lastPubLat = pubLat;

            if (p.getMetrics() != null) {
                String pubLocStr = String.format("(%.4f, %.4f)", pub.getLocation().getX(), pub.getLocation().getY());
                String payload = "Pub:" + pubLocStr + " -> Sub:" + getFormattedLocation();

                String status = matchesInterest ? "Delivered" : "Delivered (Unwanted)";
                CsvMetricWriter.getInstance().logPublication(
                        p,
                        getName(),
                        payload,
                        status);
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

    public void send(SimulationSubscription s) {
        SimulationBroker broker = getBroker();
        if (broker == null) {
            logger.severe(getName() + ": topology error, no broker");
            return;
        }

        s.setSource(this);
        
        if (SimConfiguration.get().paths.enableSubscriptionTracing) {
            long seqId = this.nSubscriptions + 1;
            long structuredTraceId = ((long) this.mySubscriberId << 32) | (seqId & 0xFFFFFFFFL);
            s.setMetrics(new EventMetrics(structuredTraceId));
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

    private void ensureRegionCapacity() {
        if (regionCoords == null) {
            regionCoords = new float[4];
        } else if (activeRegionCount * 4 >= regionCoords.length) {
            float[] newArr = new float[regionCoords.length + 16];
            System.arraycopy(regionCoords, 0, newArr, 0, regionCoords.length);
            regionCoords = newArr;
        }
    }

    // --- Accessors ---
    public int getnPublications() { return nPublications; }
    public int getnSubscriptions() { return nSubscriptions; }
    public int getFalsePositiveDeliveries() { return falsePositiveDeliveries; }
    public int getHopCount() { return hopCount; }
    public long getHopSum() { return hopSum; }
    public int getHopMin() { return hopMin; }
    public int getHopMax() { return hopMax; }
    public Location getLastReceivedPubLocation() { return Float.isNaN(lastPubLon) ? null : new Location(lastPubLon, lastPubLat, -1); }
    @Override public Location getMetricLocation() { return new Location(myLon, myLat, -1); }
    public Location getLocation() { return getMetricLocation(); }
    public SimulationBroker getBroker() { return (getParent() instanceof SimulationBroker) ? (SimulationBroker) getParent() : null; }
}
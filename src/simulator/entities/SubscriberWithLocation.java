package simulator.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.context.RegionalEvaluationContext;
import simulator.entities.context.SubscriberEvaluationContext;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.metrics.EventMetrics;
import utils.CustomLogger;

public class SubscriberWithLocation extends TreeNode {

    private static final Logger logger = CustomLogger.getLogger(SubscriberWithLocation.class.getName());
    private static final AtomicInteger SUBSCRIBER_ID_GENERATOR = new AtomicInteger(1);
    
    private final int mySubscriberId; 
    private final float myLat;
    private final float myLon;
    private String myCountry = null; // Lazy loaded for traces
    
    // --- Metric State ---
    private int falsePositiveDeliveries = 0;
    private int nSubscriptions = 0; 
    private int nPublications = 0;
    private long hopSum = 0;
    private int hopCount = 0;
    private int hopMin = Integer.MAX_VALUE;
    private int hopMax = Integer.MIN_VALUE;

    // --- STRATEGY: Evaluation Context ---
    private SubscriberEvaluationContext evalContext = new RegionalEvaluationContext();

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
        return "Sub-" + mySubscriberId;
    }

    public void setEvaluationContext(SubscriberEvaluationContext ctx) {
        this.evalContext = ctx;
    }

    public SubscriberEvaluationContext getContext() {
        return this.evalContext;
    }

    // --- MAIN LOGIC ---

    public void receive(SimulationPublication p) {
        nPublications++;
        if (p instanceof PublicationWithLocation pub) {
            
            // 1. Delegate Logic & Tracing to Context
            if (evalContext != null) {
                evalContext.onPublicationReceived(this, pub);
            }

            // 2. Update Stats
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
        
        // 1. Prepare Metrics if Tracing is enabled (Common Logic)
        if (SimConfiguration.get().paths.enableEventTracing) {
             ensureTraceMetrics(s);
        }

        // 2. Delegate Logic & Tracing to Context
        if (evalContext != null) {
            evalContext.onSubscriptionSent(s);
            
            if (SimConfiguration.get().paths.enableEventTracing) {
                evalContext.traceSubscription(this, s, broker);
            }
        }

        broker.processSubscription(s);
        nSubscriptions++;
    }
    
    // --- Helper Methods ---

    public void incrementFalsePositiveDeliveries() {
        this.falsePositiveDeliveries++;
    }

    private void ensureTraceMetrics(SimulationSubscription s) {
        long seqId = this.nSubscriptions + 1;
        long structuredTraceId = ((long) this.mySubscriberId << 32) | (seqId & 0xFFFFFFFFL);
        
        EventMetrics metrics = new EventMetrics(structuredTraceId);
        
        if (myCountry == null) {
            myCountry = resolveCountry();
        }
        
        metrics.setOriginalSourceInfo(getName(), myCountry, (double)myLon, (double)myLat);
        s.setMetrics(metrics);
    }

    private String resolveCountry() {
        List<TreeNode> path = new ArrayList<>();
        TreeNode current = this;
        while (current != null) { path.add(current); current = current.getParent(); }
        // Attempt to find country in hierarchy (usually grand-parent in GeoNames)
        if (path.size() >= 3) return path.get(path.size() - 3).getName();
        if (path.size() >= 2) return path.get(path.size() - 2).getName();
        return "Unknown";
    }

    public SimulationBroker getBroker() { 
        return (getParent() instanceof SimulationBroker) ? (SimulationBroker) getParent() : null; 
    }

    public Location getLocation() { 
        return new Location(myLon, myLat, 0); 
    }
    
    @Override 
    public Location getMetricLocation() { 
        return getLocation(); 
    }

    // Standard Getters
    public int getnPublications() { return nPublications; }
    public int getnSubscriptions() { return nSubscriptions; }
    public int getFalsePositiveDeliveries() { return falsePositiveDeliveries; }
    public int getHopCount() { return hopCount; }
    public long getHopSum() { return hopSum; }
    public int getHopMin() { return hopMin; }
    public int getHopMax() { return hopMax; }
    public float getLat() { return myLat; }
    public float getLon() { return myLon; }
}
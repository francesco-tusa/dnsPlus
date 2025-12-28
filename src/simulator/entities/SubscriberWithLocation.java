package simulator.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.metrics.EventMetrics;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.visualisation.TopologyVisualiser;
import utils.CsvMetricWriter;
import utils.CustomLogger;
import utils.TraceIdGenerator;

public class SubscriberWithLocation extends TreeNode {

    private static final Logger logger = CustomLogger.getLogger(SubscriberWithLocation.class.getName());

    private List<Region> activeRegions = null; 
    private int falsePositiveDeliveries = 0;

    private final Location location;
    private int nSubscriptions = 0;
    private int nPublications = 0;

    private Location lastReceivedPubLocation = null;

    private long hopSum = 0;
    private int hopCount = 0;
    private int hopMin = Integer.MAX_VALUE;
    private int hopMax = Integer.MIN_VALUE;

    public SubscriberWithLocation(String name, Location location) {
        super(name);
        this.location = location;
    }

    public void receive(SimulationPublication p) {
        if (logger.isLoggable(java.util.logging.Level.FINE)) {
            logger.fine(getName() + ": received publication " + p);
        }
        nPublications++;

        if (p instanceof PublicationWithLocation pub) {
            // 1. False Positive Validation
            boolean matchesInterest = false;
            if (activeRegions != null) {
                for (Region r : activeRegions) {
                    if (r.contains(pub.getLocation())) {
                        matchesInterest = true;
                        break;
                    }
                }
                if (!matchesInterest) {
                    falsePositiveDeliveries++;
                }
            }

            // 2. Update State: Store LOCATION ONLY
            this.lastReceivedPubLocation = pub.getLocation();

            // 3. Metrics (Using Primitives)
            if (p.getMetrics() != null) {
                int hops = p.getMetrics().getHops();
                synchronized(this) {
                    hopSum += hops;
                    hopCount++;
                    if (hops < hopMin) hopMin = hops;
                    if (hops > hopMax) hopMax = hops;
                }
                
                // Optional: Sample logging to save I/O if needed
                CsvMetricWriter.getInstance().logPublication(
                    p.getMetrics().getTraceId(),
                    this.getName(),
                    p.getSource(), 
                    hops,
                    this.getLocation().toShortString(),
                    "Delivered"
                );
            }
        }
        
        // 4. Visualisation
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null && p.getSource() != null) {
            visualizer.updatePublicationEdge(p.getSource().getName(), getName(), false);
        }
    }
        
    public void send(SimulationSubscription s) {
        SimulationBroker broker = getBroker();
        s.setSource(this);
        s.setMetrics(new EventMetrics(TraceIdGenerator.nextId()));

        if (broker != null) {
            if (logger.isLoggable(java.util.logging.Level.FINE)) {
                 String subInfo = (s instanceof simulator.events.SubscriptionWithLocation sl) 
                ? " for location " + sl.getLocation() 
                : " for region " + ((SubscriptionWithRegion)s).getRegion().toShortString();
                logger.fine("\n" + getName() + ": sending subscription" + subInfo);
            }

            if (s instanceof SubscriptionWithRegion swr) {
                if (this.activeRegions == null) {
                    this.activeRegions = new ArrayList<>(1);
                }
                this.activeRegions.add(swr.getRegion());
            }

            broker.processSubscription(s);
            nSubscriptions++;

            TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
            if (visualizer != null) {
                visualizer.setNodeActive(getName());
                visualizer.updateSubscriberLabel(this, s);
            }
        } else {
            logger.severe(getName() + ": topology error, no broker");
        }
    }
    
    public Location getLastReceivedPubLocation() { 
        return lastReceivedPubLocation; 
    }

    // Accessors for Primitive Metrics
    public long getHopSum() { return hopSum; }
    public int getHopCount() { return hopCount; }
    public int getHopMin() { return hopMin; }
    public int getHopMax() { return hopMax; }

    public Location getLocation() { return location; }
    public int getnSubscriptions() { return nSubscriptions; }
    public int getnPublications() { return nPublications; }
    public int getFalsePositiveDeliveries() { return falsePositiveDeliveries; }
    
    public SimulationBroker getBroker() { 
        if (getParent() instanceof SimulationBroker) {
            return (SimulationBroker) getParent();
        }
        return null;
    }
    
    @Override
    public Location getMetricLocation() { return this.location; }
}
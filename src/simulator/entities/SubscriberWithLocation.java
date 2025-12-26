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

    // Track the specific regions this subscriber is interested in
    private final List<Region> activeRegions = new ArrayList<>();
    
    // Counter for messages received that fall OUTSIDE the specific region
    private int falsePositiveDeliveries = 0;

    private final Location location;
    private int nSubscriptions;
    private int nPublications;
    private PublicationWithLocation lastReceivedPublication;
    
    // Store hop counts for statistical analysis
    private final List<Integer> receivedHopsList = new ArrayList<>();

    public SubscriberWithLocation(String name, Location location) {
        super(name);
        this.location = location;
        this.nSubscriptions = 0;
        this.nPublications = 0;
        this.lastReceivedPublication = null;
    }

    public void receive(SimulationPublication p) {
        logger.fine(getName() + ": received publication " + p);
        nPublications++;

        if (p instanceof PublicationWithLocation pub) {
            
            // --- 1. False Positive Delivery Validation ---
            boolean matchesInterest = false;
            
            // Check if this publication actually falls inside ANY of our requested regions
            for (Region r : activeRegions) {
                if (r.contains(pub.getLocation())) {
                    matchesInterest = true;
                    break;
                }
            }
            
            // If it arrived but we didn't want it (geometrically), it's a False Positive Delivery.
            // We only count this if we have active regions (to distinguish from non-spatial logic).
            if (!matchesInterest && !activeRegions.isEmpty()) {
                falsePositiveDeliveries++;
            }

            // --- 2. Update Subscriber State ---
            this.lastReceivedPublication = pub;

            // --- 3. Metrics Logging ---
            if (p.getMetrics() != null) {
                int hops = p.getMetrics().getHops();
                receivedHopsList.add(hops);

                // Log the delivery. 
                // Note: We use 'this.getLocation()' to visualize where the SUBSCRIBER is, 
                // rather than where the EVENT happened.
                CsvMetricWriter.getInstance().logPublication(
                    p.getMetrics().getTraceId(),
                    this.getName(),                 // Receiver's Name
                    p.getSource(),                  // Source (Last Broker)
                    hops,
                    this.getLocation().toShortString(),
                    "Delivered"
                );
            }
        }

        // --- 4. Visualisation (Generic) ---
        TopologyVisualiser visualizer = TopologyVisualiser.getInstance();
        if (visualizer != null && p.getSource() != null) {
            boolean isUpward = false; 
            visualizer.updatePublicationEdge(p.getSource().getName(), getName(), isUpward);
        }
    }
        
    public void send(SimulationSubscription s) {
        SimulationBroker broker = getBroker();
        s.setSource(this);

        long traceId = TraceIdGenerator.nextId();
        s.setMetrics(new EventMetrics(traceId));

        if (broker != null) {
            String subInfo = (s instanceof simulator.events.SubscriptionWithLocation sl) 
                ? " for location " + sl.getLocation() 
                : " for region " + ((SubscriptionWithRegion)s).getRegion().toShortString();
            logger.fine("\n" + getName() + ": sending subscription" + subInfo);

            if (s instanceof SubscriptionWithRegion swr) {
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
            logger.severe(getName() + ": topology error, there is no broker to send the subscription to");
        }
    }
    
    public Location getLocation() { return location; }
    public int getnSubscriptions() { return nSubscriptions; }
    public int getnPublications() { return nPublications; }
    public int getFalsePositiveDeliveries() { return falsePositiveDeliveries; }
    
    public List<Integer> getReceivedHopsList() {
        return receivedHopsList;
    }
    
    public SimulationBroker getBroker() { 
        if (getParent() instanceof SimulationBroker) {
            return (SimulationBroker) getParent();
        }
        return null;
    }
    
    public PublicationWithLocation getLastReceivedPublication() { return lastReceivedPublication; }


    @Override
    public Location getMetricLocation() {
        return this.location;
    }
}
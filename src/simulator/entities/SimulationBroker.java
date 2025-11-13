package simulator.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.TrackableEvent;
import simulator.regions.BrokerWithRegion;
import utils.CustomLogger;

public abstract class SimulationBroker extends TreeNode {

    private static final Logger logger = CustomLogger.getLogger(SimulationBroker.class.getName());

    private final Map<TreeNode, SimulationSubscription> subscriptionsTable = new HashMap<>();
    
    // A simple, efficient counter for all processing events
    protected long totalSubscriptionProcessingEvents = 0;
    
    private final List<Long> publicationProcessingCosts = new ArrayList<>();

    public SimulationBroker(String name) {
        super(name);
    }


    public abstract void addSubscription(SimulationSubscription s);

    public abstract SimulationSubscription matchPublication(SimulationPublication p);

    protected abstract void propagateSubscription(SimulationSubscription s);


    public BrokerWithRegion getParentBroker() {
        TreeNode parent = getParent();
        if (parent instanceof BrokerWithRegion) {
            return (BrokerWithRegion) parent;
        }
        return null;
    }
    
    /**
     * Processes a subscription or publication.
     * This common helper now uses the TrackableEvent interface.
     */
    private void processEvent(TrackableEvent event, String brokerName) {
        event.incrementHops();
        event.addBrokerToPath(brokerName);

        // Let the subclass decide how to record the region
        captureRegionMetric(event);
    }

    protected void captureRegionMetric(TrackableEvent event) {
        // Default for generic brokers that don't have regions
        event.addBrokerRegionToPath("N/A");
    }

    public void processSubscription(SimulationSubscription s) {
        processEvent(s, this.getName());
        this.totalSubscriptionProcessingEvents++;
        
        this.propagateSubscription(s);
    }

    public void processPublication(SimulationPublication p) {
        processEvent(p, this.getName()); // Use the common helper
        
        long startTime = System.nanoTime();
        this.matchPublication(p);
        long endTime = System.nanoTime();
        
        this.publicationProcessingCosts.add(endTime - startTime);
    }

    public Map<TreeNode, SimulationSubscription> getSubscriptionsTable() {
        return subscriptionsTable;
    }

    
    public long getTotalSubscriptionProcessingEvents() {
        return this.totalSubscriptionProcessingEvents;
    }


    public List<Long> getPublicationProcessingCosts() {
        return publicationProcessingCosts;
    }
}
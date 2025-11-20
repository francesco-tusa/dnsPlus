package simulator.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.BrokerWithRegion;

/**
 * Abstract base class for all broker entities in the simulation.
 */
public abstract class SimulationBroker extends TreeNode {

    /**
     * The main subscription table.
     */
    private final Map<TreeNode, SimulationSubscription> subscriptionsTable = new HashMap<>();
    
    // Metrics (Performance Counters)
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
     * Entry point for processing a subscription.
     */
    public void processSubscription(SimulationSubscription s) {
        // Metric tracking (hops/paths) is now handled by the concrete Broker implementation
        // via EventMetrics deep-copying. We just count the event locally here.
        this.totalSubscriptionProcessingEvents++;
        
        this.propagateSubscription(s);
    }

    /**
     * Entry point for processing a publication.
     */
    public void processPublication(SimulationPublication p) {
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
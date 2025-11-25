package simulator.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;

/**
 * Abstract base class for all broker entities in the simulation.
 */
public abstract class SimulationBroker extends TreeNode {
    
    // Metrics (Performance Counters)
    protected long totalSubscriptionProcessingEvents = 0;
    private final List<Long> publicationProcessingCosts = new ArrayList<>();

    public SimulationBroker(String name) {
        super(name);
    }

    public abstract void addSubscription(SimulationSubscription s);

    public abstract SimulationSubscription matchPublication(SimulationPublication p);

    protected abstract void propagateSubscription(SimulationSubscription s);

    public abstract int getSubscriptionCount();

    /**
     * Returns the Input Store state (Subscriptions received by this broker).
     * Uniform accessor for all broker types.
     */
    public abstract Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions();

    /**
     * Returns the Output Store state (Subscriptions propagated TO neighbors).
     * Uniform accessor for all broker types.
     */
    public abstract Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions();
    


    public BoundedBroker getParentBroker() {
        TreeNode parent = getParent();
        if (parent instanceof BoundedBroker) {
            return (BoundedBroker) parent;
        }
        return null;
    }
    
    /**
     * Entry point for processing a subscription.
     */
    public void processSubscription(SimulationSubscription s) {
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

    public long getTotalSubscriptionProcessingEvents() {
        return this.totalSubscriptionProcessingEvents;
    }

    public List<Long> getPublicationProcessingCosts() {
        return publicationProcessingCosts;
    }
}
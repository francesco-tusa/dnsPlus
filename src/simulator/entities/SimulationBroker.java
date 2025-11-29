package simulator.entities;

import java.util.List;
import java.util.Map;
import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;

public abstract class SimulationBroker extends TreeNode {

    // Metrics
    protected long totalSubscriptionProcessingEvents = 0;
    protected long totalPublicationProcessingEvents = 0; 
    protected long totalMatchingComputations = 0;        
    
    // Tracks events where a message was processed but forwarded to no one (Dead End)
    protected long totalFalsePositiveEvents = 0; 

    public SimulationBroker(String name) {
        super(name);
    }

    public abstract void addSubscription(SimulationSubscription s);
    public abstract SimulationSubscription matchPublication(SimulationPublication p);
    protected abstract void propagateSubscription(SimulationSubscription s);
    
    // Metric Accessors
    public abstract int getInputSubscriptionCount();
    public abstract int getOutputSubscriptionCount();

    public abstract Map<TreeNode, List<SimulationSubscription>> getInputSubscriptions();
    public abstract Map<TreeNode, List<SimulationSubscription>> getPropagatedSubscriptions();

    public BoundedBroker getParentBroker() {
        TreeNode parent = getParent();
        if (parent instanceof BoundedBroker) {
            return (BoundedBroker) parent;
        }
        return null;
    }
    
    public void processSubscription(SimulationSubscription s) {
        this.totalSubscriptionProcessingEvents++;
        this.propagateSubscription(s);
    }

    public void processPublication(SimulationPublication p) {
        // Track that a message arrived and was processed (Traffic Load)
        this.totalPublicationProcessingEvents++;
        
        // The subclass matchPublication is responsible for incrementing 
        // totalMatchingComputations (Computational Load)
        this.matchPublication(p);
    }

    public long getTotalSubscriptionProcessingEvents() {
        return this.totalSubscriptionProcessingEvents;
    }
    
    public long getTotalPublicationProcessingEvents() {
        return this.totalPublicationProcessingEvents;
    }

    public long getTotalMatchingComputations() {
        return totalMatchingComputations;
    }

    public long getTotalFalsePositiveEvents() {
        return totalFalsePositiveEvents;
    }
}
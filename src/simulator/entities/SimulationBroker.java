package simulator.entities;

import java.util.List;
import java.util.Map;

import broker.GenericBroker;
import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;

public abstract class SimulationBroker extends TreeNode 
    implements GenericBroker<SimulationSubscription, SimulationPublication> {

    // Metrics
    protected long totalSubscriptionProcessingEvents = 0;
    protected long totalPublicationProcessingEvents = 0; 
    protected long totalMatchingComputations = 0;        
    
    // Tracks events where a message was processed but forwarded to no one (Dead End)
    protected long totalFalsePositiveEvents = 0; 

    public SimulationBroker(String name) {
        super(name);
    }

    // --- SUBSCRIPTION PIPELINE ---

    /**
     * Public Entry Point (e.g., from Clients/Tests).
     * Delegates strictly to the central processing pipeline.
     */
    @Override
    public void addSubscription(SimulationSubscription s) {
        this.processSubscription(s);
    }

    /**
     * The Single Source of Truth for subscription logic.
     * Subclasses cannot bypass metrics or the pipeline structure.
     */
    @Override
    public final void processSubscription(SimulationSubscription s) {
        this.totalSubscriptionProcessingEvents++;
        
        // Delegate to the concrete implementation hook
        handleSubscriptionProcessing(s);
    }

    /**
     * Concrete implementation logic.
     * Responsible for:
     * 1. Storage (inputStore)
     * 2. State Updates (Routing Tables)
     * 3. Propagation (Forwarding to parents/children)
     */
    protected abstract void handleSubscriptionProcessing(SimulationSubscription s);

    // --- PUBLICATION PIPELINE ---

    @Override
    public void processPublication(SimulationPublication p) {
        // Track that a message arrived and was processed (Traffic Load)
        this.totalPublicationProcessingEvents++;
        
        // The subclass matchPublication is responsible for incrementing 
        // totalMatchingComputations (Computational Load)
        this.matchPublication(p);
    }

    // Abstract matching logic required by GenericBroker
    @Override
    public abstract SimulationSubscription matchPublication(SimulationPublication p);

    // --- METRICS & GETTERS ---

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
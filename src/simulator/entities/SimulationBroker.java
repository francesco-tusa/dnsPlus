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

/**
 * Abstract base class for all broker entities in the simulation.
 * <p>This class provides the foundational data structures and methods for:</p>
 * <ul>
 * <li>Maintaining a subscription table (neighbors' interests).</li>
 * <li>Tracking basic metrics (processing events, costs).</li>
 * <li>Defining the contract for subscription and publication processing.</li>
 * </ul>
 * <p>Subclasses (like {@link BrokerWithRegion}) must implement the specific
 * routing logic (e.g., region-based, topic-based).</p>
 */
public abstract class SimulationBroker extends TreeNode {

    /**
     * The main subscription table.
     * Maps a neighbor node (Source) to the subscription they have registered with us.
     */
    private final Map<TreeNode, SimulationSubscription> subscriptionsTable = new HashMap<>();
    
    // Metrics
    protected long totalSubscriptionProcessingEvents = 0;
    private final List<Long> publicationProcessingCosts = new ArrayList<>();

    public SimulationBroker(String name) {
        super(name);
    }

    /**
     * Adds or updates a subscription in the main {@code subscriptionsTable}.
     * <p><b>Subclass Responsibility:</b> Subclasses should override this to implement
     * specific aggregation logic (e.g., merging regions) before storing.</p>
     * @param s The subscription to add.
     */
    public abstract void addSubscription(SimulationSubscription s);

    /**
     * Matches a publication against the {@code subscriptionsTable} and forwards it.
     * <p><b>Subclass Responsibility:</b> Implement the routing logic to determine
     * which neighbors should receive the publication.</p>
     * @param p The publication to match.
     * @return The matching subscription (optional/debug), or null.
     */
    public abstract SimulationSubscription matchPublication(SimulationPublication p);

    /**
     * Core logic for handling a received subscription event.
     * <p><b>Subclass Responsibility:</b> Implement the propagation strategy.
     * typically: Update Input View -> Check Output View -> Propagate if changed.</p>
     * @param s The received subscription.
     */
    protected abstract void propagateSubscription(SimulationSubscription s);


    /**
     * Helper to safely cast the parent TreeNode to a BrokerWithRegion.
     */
    public BrokerWithRegion getParentBroker() {
        TreeNode parent = getParent();
        if (parent instanceof BrokerWithRegion) {
            return (BrokerWithRegion) parent;
        }
        return null;
    }
    
    /**
     * Common helper to track metrics for any event (Sub or Pub).
     */
    private void processEvent(TrackableEvent event, String brokerName) {
        event.incrementHops();
        event.addBrokerToPath(brokerName);
        captureRegionMetric(event);
    }

    /**
     * Hook for subclasses to add region-specific info to event traces.
     */
    protected void captureRegionMetric(TrackableEvent event) {
        // Default implementation: do nothing or add N/A
        event.addBrokerRegionToPath("N/A");
    }

    /**
     * Entry point for processing a subscription.
     * Tracks metrics and delegates to the abstract {@link #propagateSubscription(SimulationSubscription)}.
     */
    public void processSubscription(SimulationSubscription s) {
        processEvent(s, this.getName());
        this.totalSubscriptionProcessingEvents++;
        
        this.propagateSubscription(s);
    }

    /**
     * Entry point for processing a publication.
     * Tracks metrics and timing, then delegates to {@link #matchPublication(SimulationPublication)}.
     */
    public void processPublication(SimulationPublication p) {
        processEvent(p, this.getName()); 
        
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
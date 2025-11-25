package simulator.regions.store;

import java.util.List;
import java.util.Map;

import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;

/**
 * Common interface for all subscription storage strategies.
 * Ensures that metrics can be collected polymorphically.
 */
public interface SubscriptionStore {
    
    /**
     * Returns the number of active subscriptions in the store.
     */
    int size();

    /**
     * Checks if the store is empty.
     */
    boolean isEmpty();

    /**
     * Uniform accessor for all store types.
     * Returns a list of subscriptions per neighbor. 
     * (For Basic stores, the list will contain a single element).
     */
    Map<TreeNode, List<SimulationSubscription>> getAllSubscriptions();
}
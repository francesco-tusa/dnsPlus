package simulator.regions.store;

import java.util.List;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;

public interface RegionSubscriptionStore extends SubscriptionStore{
    /**
     * Adds or updates a subscription from a specific source.
     * Returns a StoreUpdate containing the result status and the specific region that changed.
     */
    StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub);

    /**
     * Finds all neighbors whose stored subscriptions cover the given location.
     * Returns the number of geometric comparisons performed (CPU Cost)
     */
    int findMatches(Location loc, List<TreeNode> resultsBuffer);

    /**
     * Retrieves the current subscription state for a specific target (Output View).
     */
    List<SimulationSubscription> getOutputFor(TreeNode target);
}
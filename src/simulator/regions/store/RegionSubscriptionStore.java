package simulator.regions.store;

import java.util.List;
import java.util.Map;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;

public interface RegionSubscriptionStore extends SubscriptionStore{
    /**
     * Adds or updates a subscription from a specific source.
     * @return true if the internal state changed.
     */
    boolean addOrUpdate(TreeNode source, SubscriptionWithRegion sub);

    /**
     * Finds all neighbors whose stored subscriptions cover the given location.
     */
    List<TreeNode> findMatches(Location loc);
    
    /**
     * Retrieves the current subscription state for a specific target (Output View).
     */
    List<SimulationSubscription> getOutputFor(TreeNode target);
}
package simulator.regions.store;

import java.util.List;
import java.util.Map;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;

public interface RegionSubscriptionStore extends SubscriptionStore{
    StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub);

    int findMatches(Location loc, Map<TreeNode, List<SimulationSubscription>> resultsBuffer);

    List<SimulationSubscription> getOutputFor(TreeNode target);
}
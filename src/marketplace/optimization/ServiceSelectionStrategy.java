package marketplace.optimization;

import java.util.List;
import simulator.core.TreeNode;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.events.ServiceRequest;

public interface ServiceSelectionStrategy {

    /**
     * Composite record holding the routing decision AND the trace diagnosis 
     * to completely eliminate redundant score calculations.
     */
    record SelectionResult(TreeNode bestNode, double bestScore, double distance, String diagnosis) {}

    SelectionResult selectBestProvider(ServiceRequest request, List<TreeNode> candidates, RegionSubscriptionStore store);
}
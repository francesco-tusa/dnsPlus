package marketplace.optimization;

import java.util.List;
import simulator.core.TreeNode;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.events.ServiceRequest;

/**
 * Interface defining how a Broker selects the best next hop 
 * for a specific Service Request among a list of candidates.
 */
public interface ServiceSelectionStrategy {

    /**
     * Selects the optimal node to forward the request to.
     *
     * @param request    The incoming Service Request.
     * @param candidates The list of neighbor nodes that physically cover the request location.
     * @param store      The broker's subscription store (to inspect the actual offers/metrics).
     * @return The best TreeNode to forward to, or null if no candidate satisfies the logic.
     */
    TreeNode selectBestProvider(ServiceRequest request, List<TreeNode> candidates, RegionSubscriptionStore store);
}
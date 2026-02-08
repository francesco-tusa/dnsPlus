package marketplace.optimization;

import java.util.List;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;

/**
 * The original strategy:
 * 1. Checks strict containment (Hard Constraints).
 * 2. Optimizes purely based on the first metric dimension (Latency).
 * 3. Ignores weights and other costs.
 */
public class LatencyFirstStrategy implements ServiceSelectionStrategy {

    @Override
    public TreeNode selectBestProvider(ServiceRequest req, List<TreeNode> candidates, RegionSubscriptionStore store) {
        
        TreeNode bestNode = null;
        double bestScore = Double.MAX_VALUE;

        for (TreeNode candidate : candidates) {
            if (candidate == req.getSource()) continue;

            List<SimulationSubscription> subs = store.getAllSubscriptions().get(candidate);
            if (subs == null) continue;

            for (SimulationSubscription sub : subs) {
                MetricHyperCube cap = extractMetricHyperCube(sub, req.getServiceId());
                
                if (cap != null) {
                    // 1. Hard Constraint Check
                    if (cap.contains(req.getLocation())) {
                        
                        // 2. Greedy Score: Just take Dimension 0 (Latency)
                        // Assumes index 0 is always the optimization target.
                        double currentScore = cap.getMinValues()[0];

                        if (currentScore < bestScore) {
                            bestScore = currentScore;
                            bestNode = candidate;
                        }
                    }
                }
            }
        }
        return bestNode;
    }

    /**
     * Helper to extract MetricHyperCube from a subscription.
     */
    private MetricHyperCube extractMetricHyperCube(SimulationSubscription sub, long targetServiceId) {
        if (sub instanceof ServiceOffer offer) {
            if (offer.getServiceId() == targetServiceId && offer.getRegion() instanceof MetricHyperCube mhc) {
                return mhc;
            }
        } 
        else if (sub instanceof SubscriptionWithRegion swr && swr.getRegion() instanceof MetricHyperCube mhc) {
            if (swr instanceof ServiceOffer so) {
                if(so.getServiceId() == targetServiceId) return mhc;
            } else {
                return mhc; 
            }
        }
        return null;
    }
}
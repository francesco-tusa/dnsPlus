package marketplace.optimization;

import java.util.List;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.common.MetricHyperCube;
import marketplace.common.MetricLocation;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;

/**
 * The new strategy:
 * 1. Checks strict containment (Hard Constraints).
 * 2. Optimizes based on a Normalized Weighted Utility Score.
 * Score = Sum( (Actual / Max) * Weight )
 */
public class WeightedUtilityStrategy implements ServiceSelectionStrategy {

    @Override
    public TreeNode selectBestProvider(ServiceRequest req, List<TreeNode> candidates, RegionSubscriptionStore store) {
        
        TreeNode bestNode = null;
        double bestScore = Double.MAX_VALUE;

        // Extract Request Context safely
        double[] maxConstraints;
        if (req.getLocation() instanceof MetricLocation ml) {
             maxConstraints = ml.getCoordinates();
        } else {
             return null; 
        }
        double[] weights = req.getWeights();

        for (TreeNode candidate : candidates) {
            if (candidate == req.getSource()) continue;

            List<SimulationSubscription> subs = store.getAllSubscriptions().get(candidate);
            if (subs == null) continue;

            for (SimulationSubscription sub : subs) {
                MetricHyperCube cap = extractMetricHyperCube(sub, req.getServiceId());
                
                if (cap != null) {
                    if (cap.contains(req.getLocation())) {
                        
                        // Calculate Utility Score
                        double currentScore = calculateScore(cap.getMinValues(), maxConstraints, weights);

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

    private double calculateScore(double[] actuals, double[] maxes, double[] weights) {
        double score = 0.0;
        for(int i = 0; i < actuals.length; i++) {
            double denominator = (maxes[i] > 0) ? maxes[i] : 1.0;
            double normalized = actuals[i] / denominator;
            score += normalized * weights[i];
        }
        return score;
    }
}
package marketplace.optimization;

import java.util.List;
import simulator.core.TreeNode;
import simulator.core.LoggableEntity; 
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.common.MetricHyperCube;
import marketplace.common.MetricLocation;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;

public class WeightedUtilityStrategy implements ServiceSelectionStrategy {

    @Override
    public TreeNode selectBestProvider(ServiceRequest req, List<TreeNode> candidates, RegionSubscriptionStore store) {
        
        TreeNode bestNode = null;
        double bestScore = Double.MAX_VALUE;

        // 1. Get Metric Location (The Constraints)
        MetricLocation constraintsLoc = req.getQoSConstraintsLocation();
        double[] constraintBoundaries = constraintsLoc.getMetricValues(); // Use getMetricValues()
        double[] weights = req.getWeights();
        boolean[] flags = req.getMinimizeFlags();

        for (TreeNode candidate : candidates) {
            if (candidate == req.getSource()) continue;

            // 2. Calculate Network Latency (Physical Distance)
            double networkLatency = 0.0;
            if (candidate instanceof LoggableEntity entity) {
                simulator.core.Location candidateLoc = entity.getMetricLocation();
                if (candidateLoc != null) {
                    double distSq = candidateLoc.distanceSquared(constraintsLoc);
                    double distance = Math.sqrt(distSq);
                    networkLatency = distance * MarketplaceMetricSchema.DISTANCE_TO_TIME_FACTOR;
                }
            }

            List<SimulationSubscription> subs = store.getAllSubscriptions().get(candidate);
            if (subs == null) continue;

            for (SimulationSubscription sub : subs) {
                MetricHyperCube cap = extractMetricHyperCube(sub, req.getServiceId());
                
                if (cap != null) {
                    // FIX: Do NOT use cap.contains(loc) for QoS Constraints.
                    // Use semantic check: Does the Provider's Range satisfy the Client's Limit?
                    if (satisfiesConstraints(cap, constraintBoundaries, flags, networkLatency)) {
                        
                        // 3. Calculate Score
                        double currentScore = calculateGenericScore(cap, constraintBoundaries, weights, flags, networkLatency);

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

    private boolean satisfiesConstraints(MetricHyperCube cap, double[] constraints, boolean[] flags, double networkLatency) {
        double[] minVals = cap.getMinValues();
        double[] maxVals = cap.getMaxValues();
        int latIdx = MarketplaceMetricSchema.IDX_LATENCY;

        for (int i = 0; i < constraints.length; i++) {
            // Get Provider's Best Promise
            // For Min (Lat): Promise is Lower Bound (minVals)
            // For Max (Rel): Promise is Upper Bound (maxVals)
            double bestPromise = flags[i] ? minVals[i] : maxVals[i];

            // Inject Latency Penalty if this dimension is Latency
            if (i == latIdx) {
                bestPromise += networkLatency;
            }

            // CHECK: Is the Promise worse than the Constraint?
            if (flags[i]) {
                // Direction: MINIMIZE (Lower is better)
                // Constraint is MAX ALLOWED.
                // Fail if Promise > Constraint
                if (bestPromise > constraints[i]) return false;
            } else {
                // Direction: MAXIMIZE (Higher is better)
                // Constraint is MIN REQUIRED.
                // Fail if Promise < Constraint
                if (bestPromise < constraints[i]) return false;
            }
        }
        return true;
    }

    private MetricHyperCube extractMetricHyperCube(SimulationSubscription sub, long targetServiceId) {
        if (sub instanceof ServiceOffer offer) {
            if (offer.getServiceId() == targetServiceId && offer.getRegion() instanceof MetricHyperCube mhc) {
                return mhc;
            }
        } 
        else if (sub instanceof SubscriptionWithRegion swr && swr.getRegion() instanceof MetricHyperCube mhc) {
            return mhc; 
        }
        return null;
    }

    private double calculateGenericScore(MetricHyperCube cap, double[] constraints, double[] weights, boolean[] flags, double networkLatencyAddition) {
        double score = 0.0;
        double[] bestMinValues = cap.getMinValues().clone(); 
        double[] bestMaxValues = cap.getMaxValues(); 

        int latIdx = MarketplaceMetricSchema.IDX_LATENCY;
        if (latIdx >= 0 && latIdx < bestMinValues.length) {
            bestMinValues[latIdx] += networkLatencyAddition;
        }

        for(int i = 0; i < constraints.length; i++) {
            if (weights[i] == 0.0) continue;

            double termScore;
            
            if (flags[i]) {
                // MINIMIZE
                double actual = (bestMinValues[i] > 0) ? bestMinValues[i] : 0.001;
                double maxAllowed = (constraints[i] > 0) ? constraints[i] : 1.0;
                termScore = actual / maxAllowed;
            } else {
                // MAXIMIZE
                double actual = (bestMaxValues[i] > 0) ? bestMaxValues[i] : 0.001;
                double minRequired = (constraints[i] > 0) ? constraints[i] : 0.001;
                termScore = minRequired / actual;
            }
            score += termScore * weights[i];
        }
        return score;
    }
}
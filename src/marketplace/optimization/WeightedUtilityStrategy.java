package marketplace.optimization;

import java.util.List;
import simulator.core.TreeNode;
import simulator.core.Location;
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.common.MetricHyperCube;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;

public class WeightedUtilityStrategy implements ServiceSelectionStrategy {

    public static final double PENALTY_SCORE = 999.0;

    public record EvaluationResult(boolean isFeasible, double score, String reason) {
        public static EvaluationResult fail(String reason) {
            return new EvaluationResult(false, PENALTY_SCORE, reason);
        }
        public static EvaluationResult success(double score) {
            return new EvaluationResult(true, score, "MATCH");
        }
    }

    @Override
    public TreeNode selectBestProvider(ServiceRequest req, List<TreeNode> candidates, RegionSubscriptionStore store) {
        TreeNode bestNode = null;
        double bestScore = PENALTY_SCORE;

        for (TreeNode candidate : candidates) {
            if (candidate == req.getSource()) continue;
            List<SimulationSubscription> subs = store.getAllSubscriptions().get(candidate);
            if (subs == null) continue;

            for (SimulationSubscription sub : subs) {
                MetricHyperCube cap = extractMetricHyperCube(sub, req.getServiceId());
                if (cap == null) continue;
                Location providerLoc = resolveProviderLocation(sub);
                EvaluationResult result = inspectLogic(cap, req, providerLoc);

                if (result.isFeasible() && result.score() < bestScore) {
                    bestScore = result.score();
                    bestNode = candidate;
                }
            }
        }
        return bestNode;
    }

    public EvaluationResult inspect(ServiceOffer offer, ServiceRequest req) {
        MetricHyperCube cap = (offer.getRegion() instanceof MetricHyperCube mhc) ? mhc : null;
        if (cap == null) return EvaluationResult.fail("Invalid Offer Region");
        return inspectLogic(cap, req, offer.getLocation());
    }

    private EvaluationResult inspectLogic(MetricHyperCube cap, ServiceRequest req, Location providerLoc) {
        double networkLatency = 0.0;
        if (req.getQoSConstraintsLocation() != null) {
             double distSq;
             if (providerLoc != null) {
                 // Point-to-Point Exact Distance (Local Edge Provider)
                 distSq = req.getQoSConstraintsLocation().distanceSquared(providerLoc);
             } else {
                 // EXPECTED DISTANCE to Region Centroid (Remote Fog/Cloud Branch)
                 // This prevents the "MINDIST = 0.0" trap for clients inside the bounding box
                 Location center = cap.getCenter();
                 distSq = center.distanceSquared(req.getQoSConstraintsLocation());
             }
             networkLatency = Math.sqrt(distSq) * MarketplaceMetricSchema.DISTANCE_TO_TIME_FACTOR;
        }

        String rejectionReason = checkConstraints(cap, req, networkLatency);
        if (rejectionReason != null) {
            return EvaluationResult.fail(rejectionReason);
        }

        // Add the network latency addition explicitly to the generic score calculation
        double score = calculateGenericScore(cap, req, networkLatency);
        return EvaluationResult.success(score);
    }

    private String checkConstraints(MetricHyperCube cap, ServiceRequest req, double networkLatency) {
        double[] constraints = req.getQoSConstraintsLocation().getMetricValues();
        boolean[] flags = req.getMinimizeFlags();
        double[] minVals = cap.getMinValues();
        int latIdx = MarketplaceMetricSchema.IDX_LATENCY;
        
        // Epsilon handles the Centroid Approximation error (approx. 10 meters of distance)
        double epsilon = 1e-4; 

        for (int i = 0; i < constraints.length; i++) {
            if (i >= minVals.length) break;

            double bestPromise = flags[i] ? minVals[i] : cap.getMaxValues()[i];
            double intrinsic = bestPromise;

            if (i == latIdx) {
                bestPromise += networkLatency;
            }

            if (flags[i]) {
                if (bestPromise > constraints[i] + epsilon) {
                    String metricName = (i == latIdx) ? "Latency" : "Metric " + i;
                    return String.format("%s Violation: %.3f (In:%.2f + Net:%.2f) > %.2f", 
                        metricName, bestPromise, intrinsic, (i == latIdx ? networkLatency : 0.0), constraints[i]);
                }
            } else {
                if (bestPromise < constraints[i] - epsilon) {
                     return String.format("Metric %d Violation: %.2f < %.2f", i, bestPromise, constraints[i]);
                }
            }
        }
        return null;
    }

    private double calculateGenericScore(MetricHyperCube cap, ServiceRequest req, double networkLatencyAddition) {
        double[] constraints = req.getQoSConstraintsLocation().getMetricValues();
        double[] weights = req.getWeights();
        boolean[] flags = req.getMinimizeFlags();
        double[] bestMinValues = cap.getMinValues(); 

        double score = 0.0;
        int latIdx = MarketplaceMetricSchema.IDX_LATENCY;
        
        for(int i = 0; i < constraints.length; i++) {
            if (weights[i] == 0.0) continue;
            if (i >= bestMinValues.length) break;

            double valToCheck = flags[i] ? bestMinValues[i] : cap.getMaxValues()[i];
            if (flags[i] && i == latIdx) valToCheck += networkLatencyAddition; 
            
            double actual = (valToCheck > 0) ? valToCheck : 0.001;
            double maxAllowed = (constraints[i] > 0) ? constraints[i] : 1.0;
            score += (flags[i] ? (actual / maxAllowed) : (maxAllowed / actual)) * weights[i];
        }
        return score;
    }

    private Location resolveProviderLocation(SimulationSubscription sub) { 
        if (sub instanceof ServiceOffer offer) return offer.getLocation(); return null; 
    }

    private MetricHyperCube extractMetricHyperCube(SimulationSubscription sub, long targetServiceId) { 
        if (sub instanceof ServiceOffer offer) {
            if (offer.getServiceId() == targetServiceId && offer.getRegion() instanceof MetricHyperCube mhc) return mhc;
        } else if (sub instanceof SubscriptionWithRegion swr && swr.getRegion() instanceof MetricHyperCube mhc) return mhc;
        return null;
    }
}
package marketplace.optimization;

import java.util.List;
import java.util.Map;

import simulator.core.TreeNode;
import simulator.core.Location;
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;
import marketplace.common.MetricHyperCube;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;

public class WeightedUtilityStrategy implements ServiceSelectionStrategy {

    public static final double PENALTY_SCORE = 999.0;

    public record EvaluationResult(boolean isFeasible, double score, String reason, double distance) {
        public static EvaluationResult fail(String reason, double distance) {
            return new EvaluationResult(false, PENALTY_SCORE, reason, distance);
        }
        public static EvaluationResult success(double score, double distance) {
            return new EvaluationResult(true, score, "MATCH", distance);
        }
    }

   @Override
    public SelectionResult selectBestProvider(ServiceRequest req, Map<TreeNode, List<SimulationSubscription>> candidates) {
        TreeNode bestNode = null;
        double bestScore = PENALTY_SCORE;
        double bestDistance = -1.0;

        String bestAttempt = "No Candidates";
        double minDist = Double.MAX_VALUE;

        // Iterate strictly over the pre-filtered matches
        for (Map.Entry<TreeNode, List<SimulationSubscription>> entry : candidates.entrySet()) {
            TreeNode candidate = entry.getKey();
            if (candidate == req.getSource()) continue;

            for (SimulationSubscription sub : entry.getValue()) {
                MetricHyperCube cap = extractMetricHyperCube(sub, req.getServiceId());
                if (cap == null) continue;

                // The logic natively handles whether this is a leaf or a branch
                EvaluationResult result = inspectLogic(cap, req);

                if (result.isFeasible() && result.score() < bestScore) {
                    bestScore = result.score();
                    bestNode = candidate;
                    bestDistance = result.distance();
                }
                
                double currentDist = result.distance() >= 0 ? result.distance() : Double.MAX_VALUE;
                if (currentDist < minDist) {
                    minDist = currentDist;
                    String scoreStr = (result.score() >= PENALTY_SCORE) ? "INF" : String.format("%.3f", result.score());
                    bestAttempt = String.format("BestAttempt=[%s] Reason=[%s] Score=[%s] Dist=[%.2f]",
                            candidate.getName(), result.reason(), scoreStr, result.distance());
                }
            }
        }
        return new SelectionResult(bestNode, bestScore, bestDistance, bestAttempt);
    }

    public EvaluationResult inspect(ServiceOffer offer, ServiceRequest req) {
        MetricHyperCube cap = (offer.getRegion() instanceof MetricHyperCube mhc) ? mhc : null;
        if (cap == null) return EvaluationResult.fail("Invalid Offer Region", -1.0);
        return inspectLogic(cap, req);
    }

    private EvaluationResult inspectLogic(MetricHyperCube cap, ServiceRequest req) {
        double optimisticNetworkLatency = 0.0;
        double barycenterNetworkLatency = 0.0;
        double optimisticDistance = -1.0;

        if (req.getQoSConstraintsLocation() != null) {
            // 1. SLA Pruning Distance (Optimistic Edge Distance)
            // MetricHyperCube IS a Region, so we call distanceSquared() natively.
            // Returns 0.0 if the client is physically inside the region
            optimisticDistance = Math.sqrt(cap.distanceSquared(req.getLocation()));
            optimisticNetworkLatency = optimisticDistance * MarketplaceMetricSchema.DISTANCE_TO_TIME_FACTOR;
            
            // 2. Utility Scoring Distance (Density Barycenter)
            // Evaluates FaaS cluster's actual center of mass for accurate marketplace competition
            double barycenterDist = Math.sqrt(req.getLocation().distanceSquared(cap.getDensityCentroid()));
            barycenterNetworkLatency = barycenterDist * MarketplaceMetricSchema.DISTANCE_TO_TIME_FACTOR;
        }

        // STRICT GATE: Use the Optimistic latency strictly for SLA pruning
        String rejectionReason = checkConstraints(cap, req, optimisticNetworkLatency); 
        if (rejectionReason != null) {
            return EvaluationResult.fail(rejectionReason, optimisticDistance);
        }

        // UTILITY MATCHING: Use the Barycenter latency strictly for Utility Scoring
        double score = calculateGenericScore(cap, req, barycenterNetworkLatency);
        return EvaluationResult.success(score, optimisticDistance);
    }

    private String checkConstraints(MetricHyperCube cap, ServiceRequest req, double networkLatency) {
        double[] constraints = req.getQoSConstraintsLocation().getMetricValues();
        boolean[] flags = req.getMinimizeFlags();
        double[] minVals = cap.getMinValues();
        int latIdx = MarketplaceMetricSchema.IDX_LATENCY;
        
        // Epsilon handles float precision and minor spatial approximations
        double epsilon = 1e-4; 

        for (int i = 0; i < constraints.length; i++) {
            if (i >= minVals.length) break;

            // Bypass unconstrained dimensions to prevent false SLA violations
            if (req.getWeights()[i] == 0.0) continue;

            double bestPromise = flags[i] ? minVals[i] : cap.getMaxValues()[i];
            double intrinsic = bestPromise;

            // Apply the mathematically safe Optimistic Network Latency
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
        double score = 0.0;
        
        // Extract the decoupled statistical center for utility evaluation
        double[] expectedYields = cap.getQosCenterOfMass(); 

        double[] constraints = req.getQoSConstraintsLocation().getMetricValues();
        double[] weights = req.getWeights();
        boolean[] flags = req.getMinimizeFlags();

        int latIdx = MarketplaceMetricSchema.IDX_LATENCY;
        
        for(int i = 0; i < constraints.length; i++) {
            if (weights[i] == 0.0) continue;
            if (i >= expectedYields.length) break;

            double termScore;
            
            // Score strictly against the Expected Yield, not the absolute boundaries
            double valToCheck = expectedYields[i];

            if (flags[i]) {
                // MINIMIZE
                if (i == latIdx) valToCheck += networkLatencyAddition; 
                
                double actual = (valToCheck > 0) ? valToCheck : 0.001;
                double maxAllowed = (constraints[i] < Double.MAX_VALUE && constraints[i] > 0) ? constraints[i] : getSystemMaximumForMetric(i);
                
                termScore = actual / maxAllowed;
            } else {
                // MAXIMIZE
                double actual = (valToCheck > 0) ? valToCheck : 0.001;
                double minRequired = (constraints[i] > 0) ? constraints[i] : getSystemMaximumForMetric(i);
                
                termScore = minRequired / actual;
            }
            
            score += termScore * weights[i];
        }
        
        return score;
    }

    private double getSystemMaximumForMetric(int index) {
        return switch (index) {
            case MarketplaceMetricSchema.IDX_LATENCY -> MarketplaceMetricSchema.SYSTEM_MAX_LATENCY;
            case 1 /* COST */ -> MarketplaceMetricSchema.SYSTEM_MAX_COST;
            case 2 /* RELIABILITY */ -> MarketplaceMetricSchema.SYSTEM_MAX_RELIABILITY;
            case 3 /* BANDWIDTH */ -> MarketplaceMetricSchema.SYSTEM_MAX_BANDWIDTH;
            default -> 100.0; // Safe fallback
        };
    }

    private MetricHyperCube extractMetricHyperCube(SimulationSubscription sub, long targetServiceId) { 
        if (sub instanceof ServiceOffer offer) {
            if (offer.getServiceId() == targetServiceId && offer.getRegion() instanceof MetricHyperCube mhc) return mhc;
        } else if (sub instanceof SubscriptionWithRegion swr && swr.getRegion() instanceof MetricHyperCube mhc) return mhc;
        return null;
    }
}
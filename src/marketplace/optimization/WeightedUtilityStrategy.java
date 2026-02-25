package marketplace.optimization;

import java.util.List;
import java.util.Map;

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

                Location providerLoc = resolveProviderLocation(sub);
                EvaluationResult result = inspectLogic(cap, req, providerLoc);

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
        return inspectLogic(cap, req, offer.getLocation());
    }

    private EvaluationResult inspectLogic(MetricHyperCube cap, ServiceRequest req, Location providerLoc) {
        double networkLatency = 0.0;
        double distance = -1.0;

        if (req.getQoSConstraintsLocation() != null) {
            double distSq;
            if (providerLoc != null) {
                // Physical Request Location vs Physical Provider Location
                distSq = req.getLocation().distanceSquared(providerLoc);
            } else {
                // Fallback if provider location is hidden
                // Assuming we route towards the broker's spatial region center
                distSq = req.getLocation().distanceSquared(cap.getCenter());
            }
            distance = Math.sqrt(distSq);
            networkLatency = distance * MarketplaceMetricSchema.DISTANCE_TO_TIME_FACTOR;
        }

        String rejectionReason = checkConstraints(cap, req, networkLatency);
        if (rejectionReason != null) {
            return EvaluationResult.fail(rejectionReason, distance); // Pass distance on fail
        }

        double score = calculateGenericScore(cap, req, networkLatency);
        return EvaluationResult.success(score, distance); // Pass distance on success
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
        double score = 0.0;
        
        // 1. Unpack the SLA arrays from the Hypercube
        double[] bestMinValues = cap.getMinValues(); 
        double[] bestMaxValues = cap.getMaxValues(); 

        // 2. Unpack the client preferences from the ServiceRequest
        double[] constraints = req.getQoSConstraintsLocation().getMetricValues();
        double[] weights = req.getWeights();
        boolean[] flags = req.getMinimizeFlags();

        int latIdx = MarketplaceMetricSchema.IDX_LATENCY;
        
        for(int i = 0; i < constraints.length; i++) {
            // Skip metrics the client doesn't care about
            if (weights[i] == 0.0) continue;
            
            // Safety bound for array lengths
            if (i >= bestMinValues.length) break;

            double termScore;
            double valToCheck;

            if (flags[i]) {
                // ==========================================
                // MINIMIZE (Lower actual = lower score = better)
                // ==========================================
                valToCheck = bestMinValues[i];
                if (i == latIdx) valToCheck += networkLatencyAddition; 
                
                double actual = (valToCheck > 0) ? valToCheck : 0.001;
                
                // FIX: Decouple normalization from hard constraints.
                double maxAllowed;
                if (constraints[i] < Double.MAX_VALUE && constraints[i] > 0) {
                    maxAllowed = constraints[i];
                } else {
                    maxAllowed = getSystemMaximumForMetric(i);
                }
                
                termScore = actual / maxAllowed;

            } else {
                // ==========================================
                // MAXIMIZE (Higher actual = lower score = better)
                // ==========================================
                valToCheck = bestMaxValues[i];
                double actual = (valToCheck > 0) ? valToCheck : 0.001;
                
                // FIX: Decouple normalization from hard constraints.
                double minRequired;
                if (constraints[i] > 0) {
                    minRequired = constraints[i];
                } else {
                    minRequired = getSystemMaximumForMetric(i);
                }
                
                termScore = minRequired / actual;
            }
            
            // Apply the client's weight to this metric's penalty
            score += termScore * weights[i];
        }
        
        return score;
    }

    /**
     * Helper method to map array indices to the System Maximums defined in the schema.
     * Prevents the utility mathematics from collapsing if a client doesn't provide a constraint.
     */
    private double getSystemMaximumForMetric(int index) {
        return switch (index) {
            case MarketplaceMetricSchema.IDX_LATENCY -> MarketplaceMetricSchema.SYSTEM_MAX_LATENCY;
            case 1 /* COST */ -> MarketplaceMetricSchema.SYSTEM_MAX_COST;
            case 2 /* RELIABILITY */ -> MarketplaceMetricSchema.SYSTEM_MAX_RELIABILITY;
            case 3 /* BANDWIDTH */ -> MarketplaceMetricSchema.SYSTEM_MAX_BANDWIDTH;
            case 4 /* ENERGY */ -> MarketplaceMetricSchema.SYSTEM_MAX_ENERGY;
            default -> 100.0; // Safe fallback
        };
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
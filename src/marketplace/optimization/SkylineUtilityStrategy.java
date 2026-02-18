package marketplace.optimization;

import java.util.List;
import simulator.core.TreeNode;
import simulator.core.LoggableEntity;
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.common.SkylineHyperCube;
import marketplace.common.MetricLocation;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;

public class SkylineUtilityStrategy implements ServiceSelectionStrategy {

    public static final double PENALTY_SCORE = 999.0;

    @Override
    public SelectionResult selectBestProvider(ServiceRequest req, List<TreeNode> candidates, RegionSubscriptionStore store) {
        TreeNode bestNode = null;
        double bestOverallScore = PENALTY_SCORE;
        double bestDistance = -1.0;

        String bestAttempt = "No Candidates";
        double minDist = Double.MAX_VALUE;

        MetricLocation constraintsLoc = req.getQoSConstraintsLocation();
        double[] constraintBoundaries = constraintsLoc.getMetricValues();
        double[] weights = req.getWeights();
        boolean[] flags = req.getMinimizeFlags();

        for (TreeNode candidate : candidates) {
            if (candidate == req.getSource()) continue;

            List<SimulationSubscription> subs = store.getAllSubscriptions().get(candidate);
            if (subs == null) continue;

            for (SimulationSubscription sub : subs) {
                SkylineHyperCube skyline = extractSkyline(sub, req.getServiceId());
                if (skyline == null) continue;

                double distance = -1.0;
                double networkLatency = 0.0;

                if (candidate instanceof LoggableEntity le && le.getMetricLocation() != null) {
                    distance = Math.sqrt(constraintsLoc.distanceSquared(le.getMetricLocation()));
                    networkLatency = distance * MarketplaceMetricSchema.DISTANCE_TO_TIME_FACTOR;
                }

                double bestSkylineScore = PENALTY_SCORE;
                boolean foundValid = false;

                // Evaluate ALL points in the Pareto Frontier
                for (double[] point : skyline.getParetoFrontier()) {
                    if (satisfiesConstraints(point, constraintBoundaries, flags, networkLatency)) {
                        foundValid = true;
                        double currentScore = calculatePointScore(point, constraintBoundaries, weights, flags, networkLatency);
                        if (currentScore < bestSkylineScore) {
                            bestSkylineScore = currentScore;
                        }
                    }
                }

                if (foundValid && bestSkylineScore < bestOverallScore) {
                    bestOverallScore = bestSkylineScore;
                    bestNode = candidate;
                    bestDistance = distance;
                }

                double currentDist = distance >= 0 ? distance : Double.MAX_VALUE;
                if (!foundValid && currentDist < minDist) {
                    minDist = currentDist;
                    bestAttempt = String.format("BestAttempt=[%s] Reason=[SLA Exceeded via Skyline] Dist=[%.2f]",
                            candidate.getName(), distance);
                }
            }
        }
        return new SelectionResult(bestNode, bestOverallScore, bestDistance, bestAttempt);
    }

    private boolean satisfiesConstraints(double[] pt, double[] constraints, boolean[] flags, double latencyAdd) {
        int latIdx = MarketplaceMetricSchema.IDX_LATENCY;
        for (int i = 0; i < constraints.length; i++) {
            if (i >= pt.length) break;
            
            double effectivePromise = pt[i];
            if (i == latIdx) effectivePromise += latencyAdd;

            if (flags[i]) { 
                if (effectivePromise > constraints[i]) return false;
            } else { 
                if (effectivePromise < constraints[i]) return false;
            }
        }
        return true;
    }

    private double calculatePointScore(double[] pt, double[] constraints, double[] weights, boolean[] flags, double latencyAdd) {
        double score = 0.0;
        int latIdx = MarketplaceMetricSchema.IDX_LATENCY;
        for (int i = 0; i < constraints.length; i++) {
            if (weights[i] == 0.0) continue;
            
            double effectiveValue = pt[i];
            if (i == latIdx) effectiveValue += latencyAdd;

            if (flags[i]) { 
                double actual = Math.max(effectiveValue, 0.001);
                double maxAllowed = Math.max(constraints[i], 1.0);
                score += (actual / maxAllowed) * weights[i];
            } else { 
                double actual = Math.max(effectiveValue, 0.001);
                double minRequired = Math.max(constraints[i], 0.001);
                score += (minRequired / actual) * weights[i];
            }
        }
        return score;
    }

    private SkylineHyperCube extractSkyline(SimulationSubscription sub, long targetServiceId) {
        if (sub instanceof ServiceOffer offer && offer.getServiceId() == targetServiceId && offer.getRegion() instanceof SkylineHyperCube s) {
            return s;
        } else if (sub instanceof SubscriptionWithRegion swr && swr.getRegion() instanceof SkylineHyperCube s) {
            return s;
        }
        return null;
    }
}
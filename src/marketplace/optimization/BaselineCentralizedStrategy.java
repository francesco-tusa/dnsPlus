package marketplace.optimization;

import java.util.List;
import java.util.Map;

import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;

public class BaselineCentralizedStrategy implements ServiceSelectionStrategy {

    private final String lockedInVendorPrefix;
    private final WeightedUtilityStrategy scoringStrategy = new WeightedUtilityStrategy();

    public BaselineCentralizedStrategy(String lockedInVendorPrefix) {
        this.lockedInVendorPrefix = lockedInVendorPrefix;
    }

    @Override
    public SelectionResult selectBestProvider(ServiceRequest request, Map<TreeNode, List<SimulationSubscription>> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new SelectionResult(null, WeightedUtilityStrategy.PENALTY_SCORE, -1.0, "No Candidates");
        }

        TreeNode bestNode = null;
        double minDistance = Double.MAX_VALUE;
        ServiceOffer blindlyChosenOffer = null; 
        
        Location clientLoc = request.getLocation();
        String diagnosis = "No matching [" + lockedInVendorPrefix + "] provider found";

        // 1. ROUTING PHASE: Make decision purely based on Geographic Proximity
        for (Map.Entry<TreeNode, List<SimulationSubscription>> entry : candidates.entrySet()) {
            TreeNode candidateNode = entry.getKey();
            if (candidateNode == request.getSource()) continue;

            for (SimulationSubscription sub : entry.getValue()) {
                if (sub instanceof ServiceOffer offer) {
                    if (offer.getServiceId() != request.getServiceId()) continue;

                    boolean isTargetBranch = false;

                    if (offer.getProviderName() != null && offer.getProviderName().startsWith(lockedInVendorPrefix)) {
                        isTargetBranch = true;
                    } else if ("Aggregated-Cluster".equals(offer.getProviderName())) {
                        isTargetBranch = true;
                    }

                    if (isTargetBranch) {
                        double distance;
                        if (offer.getLocation() != null) {
                            distance = Math.sqrt(clientLoc.distanceSquared(offer.getLocation()));
                        } else if ("Aggregated-Cluster".equals(offer.getProviderName()) && offer.getRegion() instanceof MetricHyperCube mhc) {
                            Location centroid = mhc.getDensityCentroid();
                            distance = (centroid != null) ? Math.sqrt(clientLoc.distanceSquared(centroid)) : 0.0;
                        } else if (offer.getRegion() != null) {
                            distance = Math.sqrt(offer.getRegion().distanceSquared(clientLoc));
                        } else {
                            continue;
                        }
                        
                        if (distance < minDistance) {
                            minDistance = distance;
                            bestNode = candidateNode;
                            blindlyChosenOffer = offer;
                            diagnosis = String.format("Baseline Geographic Route via %s (Dist: %.2f)", offer.getProviderName(), distance);
                        }
                    }
                }
            }
        }

        // 2. EVALUATION PHASE: Calculate the ACTUAL utility of the blindly chosen offer
        double finalUtilityScore = WeightedUtilityStrategy.PENALTY_SCORE;

        if (bestNode != null && blindlyChosenOffer != null) {
            // First, run standard inspection to check for feasibility
            WeightedUtilityStrategy.EvaluationResult eval = scoringStrategy.inspect(blindlyChosenOffer, request);

            if (eval.isFeasible()) {
                finalUtilityScore = eval.score();
            } else {
                diagnosis += " [WARNING: Strict SLA Violation - " + eval.reason() + "]";
                finalUtilityScore = eval.score();
            }
        }

        double finalDistance = (minDistance != Double.MAX_VALUE) ? minDistance : -1.0;
        return new SelectionResult(bestNode, finalUtilityScore, finalDistance, diagnosis);
    }
}
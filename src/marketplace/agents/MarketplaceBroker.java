package marketplace.agents;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.policy.StrictPropagationPolicy;
import simulator.regions.store.RegionSubscriptionStore;
import utils.CsvMetricWriter;
import marketplace.topology.store.MarketplaceRegionStore;
import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;
import marketplace.optimization.ServiceSelectionStrategy;
import marketplace.optimization.WeightedUtilityStrategy;

import java.util.List;
import java.util.ArrayList;

public class MarketplaceBroker extends SpatialMatchBroker {

    private final List<TreeNode> matchBuffer = new ArrayList<>();
    
    private ServiceSelectionStrategy selectionStrategy;

    // CONSTRUCTOR OVERRIDES: Explicitly force StrictPropagationPolicy (No Clipping)
    public MarketplaceBroker(String name, double threshold) {
        super(name, false, threshold, new StrictPropagationPolicy());
        this.selectionStrategy = new WeightedUtilityStrategy();
    }

    public MarketplaceBroker(String name, Location p1, Location p2, double threshold) {
        super(name, p1, p2, false, threshold, new StrictPropagationPolicy());
        this.selectionStrategy = new WeightedUtilityStrategy();
    }
    
    public void setSelectionStrategy(ServiceSelectionStrategy strategy) {
        this.selectionStrategy = strategy;
    }

    @Override
    protected RegionSubscriptionStore createStore(boolean forceSingleRegion, double threshold) {
        return new MarketplaceRegionStore(threshold);
    }

    @Override
    protected SubscriptionWithRegion createCandidateSubscription(SubscriptionWithRegion aggregatedState, Region newRegionPayload) {
        if (aggregatedState.getRegion() instanceof MetricHyperCube) {
            MetricHyperCube mhc = (MetricHyperCube) aggregatedState.getRegion();
            
            MetricHyperCube newCube = new MetricHyperCube(
                    mhc.getMinValues(),
                    mhc.getMaxValues(),
                    mhc.getMinimizeFlags(),
                    newRegionPayload
            );

            if (aggregatedState instanceof ServiceOffer) {
                ServiceOffer offer = (ServiceOffer) aggregatedState;
                return ServiceOffer.createAggregated(offer.getServiceId(), newCube);
            }
            return new SubscriptionWithRegion(newCube);
        }
        return super.createCandidateSubscription(aggregatedState, newRegionPayload);
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        if (!(p instanceof ServiceRequest)) {
            return super.matchPublication(p);
        }
        ServiceRequest req = (ServiceRequest) p;

        CsvMetricWriter.getInstance().logPublication(
            p, this.getName(), "RECEIVED", "Level " + this.getNodeLevel()
        );

        this.matchBuffer.clear();
        
        // 1. Spatial Filter (Physical Layer)
        this.getInputStore().findMatches(req.getLocation(), this.matchBuffer);

        if (this.matchBuffer.isEmpty()) {
            this.totalFalsePositiveEvents++; 
            CsvMetricWriter.getInstance().logPublication(
                p, this.getName(), "DROP_NO_MATCH", "No Spatial/Content Match"
            );
            return null;
        }

        // 2. Strategy Execution (Logic Layer)
        TreeNode bestNode = this.selectionStrategy.selectBestProvider(
            req, this.matchBuffer, this.getInputStore()
        );

        // Check config once to avoid repeated calls
        boolean tracingEnabled = SimConfiguration.get().paths.enableEventTracing;

        // 3. Forwarding & Observability
        if (bestNode != null) {
            String details = "Selected " + bestNode.getName();
            
            // OPTIMIZATION: Only calculate score/distance if tracing is explicitly enabled.
            if (tracingEnabled && this.selectionStrategy instanceof WeightedUtilityStrategy) {
                WeightedUtilityStrategy weightedStrat = (WeightedUtilityStrategy) this.selectionStrategy;
                List<SimulationSubscription> subs = this.getInputStore().getAllSubscriptions().get(bestNode);
                
                if (subs != null) {
                    double bestScore = WeightedUtilityStrategy.PENALTY_SCORE;
                    double dist = -1.0;
                    
                    for (SimulationSubscription sub : subs) {
                        if (sub instanceof ServiceOffer) {
                             ServiceOffer offer = (ServiceOffer) sub;
                             
                             var result = weightedStrat.inspect(offer, req);
                             
                             if (result.isFeasible()) {
                                 double s = result.score();
                                 if (s < bestScore) {
                                     bestScore = s;
                                     if (req.getLocation() != null) {
                                         // REFACTORED: Use Region MINDIST if exact location is null
                                         double distSq = (offer.getLocation() != null) 
                                             ? offer.getLocation().distanceSquared(req.getLocation())
                                             : offer.getRegion().distanceSquared(req.getLocation());
                                         dist = Math.sqrt(distSq);
                                     }
                                 }
                             }
                        }
                    }
                    if (bestScore < WeightedUtilityStrategy.PENALTY_SCORE) {
                        details += String.format(" (Score: %.3f, Dist: %.1f)", bestScore, dist);
                    }
                }
            }

            CsvMetricWriter.getInstance().logPublication(
                p, this.getName(), "FORWARDED", details
            );
            forwardPublicationToNode(p, bestNode);
        } else {
            // Drop Logic
            this.totalFalsePositiveEvents++;
            
            String reason = "No suitable provider found via Utility Strategy";

            if (tracingEnabled && this.selectionStrategy instanceof WeightedUtilityStrategy) {
                reason = diagnoseBestReject(req);
            }

            CsvMetricWriter.getInstance().logPublication(
                p, this.getName(), "DROP_STRATEGY_REJECT", reason
            );
        }

        return null;
    }

    /**
     * Re-inspects the candidates to find the "closest" one that failed.
     * Extracts its Score and Distance (MINDIST) to allow comparison with Ground Truth.
     */
    private String diagnoseBestReject(ServiceRequest req) {
        if (!(this.selectionStrategy instanceof WeightedUtilityStrategy)) {
            return "Strategy Reject (Generic)";
        }
        WeightedUtilityStrategy strategy = (WeightedUtilityStrategy) this.selectionStrategy;

        String bestAttempt = "No Candidates";
        double minDistSq = Double.MAX_VALUE;

        // Iterate over all candidates that were spatially matched
        for (TreeNode candidate : this.matchBuffer) {
            List<SimulationSubscription> subs = this.getInputStore().getAllSubscriptions().get(candidate);
            if (subs == null) continue;

            for (SimulationSubscription sub : subs) {
                if (sub instanceof ServiceOffer) {
                    ServiceOffer offer = (ServiceOffer) sub;
                    Location loc = offer.getLocation();
                    
                    // REFACTORED: Null-safe point-to-region vs point-to-point calculation
                    double distSq = Double.MAX_VALUE;
                    if (req.getLocation() != null) {
                        distSq = (loc != null) 
                            ? loc.distanceSquared(req.getLocation()) 
                            : offer.getRegion().distanceSquared(req.getLocation());
                    }
                    
                    // Prioritize the node that was spatially closest (smallest squared distance)
                    if (distSq < minDistSq) {
                        minDistSq = distSq;
                        var result = strategy.inspect(offer, req);
                        
                        double distLinear = (distSq == Double.MAX_VALUE) ? -1.0 : Math.sqrt(distSq);

                        String scoreStr = (result.score() >= WeightedUtilityStrategy.PENALTY_SCORE)
                                ? "INF"
                                : String.format("%.3f", result.score());

                        // Use scoreStr in the format
                        bestAttempt = String.format("BestAttempt=[%s] Reason=[%s] Score=[%s] Dist=[%.2f]",
                                candidate.getName(), result.reason(), scoreStr, distLinear);
                    }
                }
            }
        }
        return bestAttempt;
    }
}
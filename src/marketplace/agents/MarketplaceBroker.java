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

        // 2. Strategy Execution (Logic Layer) - Evaluates everything ONCE
        ServiceSelectionStrategy.SelectionResult selection = this.selectionStrategy.selectBestProvider(
            req, this.matchBuffer, this.getInputStore()
        );
        TreeNode bestNode = selection.bestNode();

        boolean tracingEnabled = SimConfiguration.get().paths.enableEventTracing;

        // 3. Forwarding & Observability
        if (bestNode != null) {
            String details = "Selected " + bestNode.getName();
            
            if (tracingEnabled && selection.bestScore() < WeightedUtilityStrategy.PENALTY_SCORE) {
                details += String.format(" (Score: %.3f, Dist: %.1f)", selection.bestScore(), selection.distance());
            }

            CsvMetricWriter.getInstance().logPublication(
                p, this.getName(), "FORWARDED", details
            );
            forwardPublicationToNode(p, bestNode);
        } else {
            // Drop Logic
            this.totalFalsePositiveEvents++;
            
            String reason = "No suitable provider found via Utility Strategy";
            if (tracingEnabled && selection.diagnosis() != null) {
                reason = selection.diagnosis(); 
            }

            CsvMetricWriter.getInstance().logPublication(
                p, this.getName(), "DROP_STRATEGY_REJECT", reason
            );
        }

        return null;
    }
}
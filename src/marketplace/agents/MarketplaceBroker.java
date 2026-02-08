package marketplace.agents;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.policy.StrictPropagationPolicy;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.topology.store.MarketplaceRegionStore;
import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;
import marketplace.optimization.ServiceSelectionStrategy;
import marketplace.optimization.WeightedUtilityStrategy; // Default
import marketplace.optimization.LatencyFirstStrategy;   // Alternative

import java.util.List;
import java.util.ArrayList;

public class MarketplaceBroker extends SpatialMatchBroker {

    private final List<TreeNode> matchBuffer = new ArrayList<>();
    
    private ServiceSelectionStrategy selectionStrategy;

    public MarketplaceBroker(String name) {
        super(name, false, 0.5, new StrictPropagationPolicy());
        // Default to the new Weighted Strategy
        this.selectionStrategy = new WeightedUtilityStrategy();
    }

    public MarketplaceBroker(String name, Location p1, Location p2) {
        super(name, p1, p2, false, 0.5, new StrictPropagationPolicy());
        // Default to the new Weighted Strategy
        this.selectionStrategy = new WeightedUtilityStrategy();
    }
    
    /**
     * Switch the optimization logic at runtime.
     * Example: broker.setSelectionStrategy(new LatencyFirstStrategy());
     */
    public void setSelectionStrategy(ServiceSelectionStrategy strategy) {
        this.selectionStrategy = strategy;
    }

    @Override
    protected RegionSubscriptionStore createStore(boolean forceSingleRegion, double threshold) {
        return new MarketplaceRegionStore(threshold);
    }

    @Override
    protected SubscriptionWithRegion createCandidateSubscription(SubscriptionWithRegion aggregatedState, Region newRegionPayload) {
        if (aggregatedState.getRegion() instanceof MetricHyperCube mhc) {
            MetricHyperCube newCube = new MetricHyperCube(mhc);
            if (aggregatedState instanceof ServiceOffer offer) {
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

        this.matchBuffer.clear();
        
        // 1. Spatial Filter (Physical Layer)
        this.getInputStore().findMatches(req.getLocation(), this.matchBuffer);

        if (this.matchBuffer.isEmpty())
            return null;

        // 2. Strategy Execution (Logic Layer)
        TreeNode bestNode = this.selectionStrategy.selectBestProvider(
            req, 
            this.matchBuffer, 
            this.getInputStore()
        );

        // 3. Forwarding
        if (bestNode != null) {
            forwardPublicationToNode(p, bestNode);
        }

        return null;
    }
}
package marketplace.agents;

import simulator.core.Location;
import simulator.regions.Region;
import simulator.regions.SpatialRegion;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.common.MetricHyperCube;
import marketplace.config.MarketplaceConfig;
import marketplace.events.ServiceOffer;
import marketplace.optimization.BaselineCentralizedStrategy;
import marketplace.optimization.WeightedUtilityStrategy;
import marketplace.topology.store.MarketplaceRegionStore;

public class HypercubeMarketplaceBroker extends AbstractMarketplaceBroker {

    public HypercubeMarketplaceBroker(String name, double threshold) {
        super(name, threshold);
        assignSelectionStrategy();
    }

    public HypercubeMarketplaceBroker(String name, Location p1, Location p2, double threshold) {
        super(name, p1, p2, threshold);
        assignSelectionStrategy();
    }
    
    
    // Dynamically assigns the routing strategy based on the simulation configuration.
    private void assignSelectionStrategy() {
        MarketplaceConfig config = MarketplaceConfig.get();
        
        if ("BASELINE".equalsIgnoreCase(config.routingStrategy)) {
            this.selectionStrategy = new BaselineCentralizedStrategy(config.baselineVendorPrefix);
        } else {
            // Default to the FaaS Marketplace Multi-Objective strategy
            this.selectionStrategy = new WeightedUtilityStrategy();
        }
    }

    @Override
    protected RegionSubscriptionStore createStore(boolean forceSingleRegion, double threshold) {
        return new MarketplaceRegionStore(threshold);
    }

    @Override
    protected SubscriptionWithRegion createCandidateSubscription(SubscriptionWithRegion aggregatedState, Region newRegionPayload) {
        if (aggregatedState.getRegion() instanceof MetricHyperCube mhc && newRegionPayload instanceof SpatialRegion newSpatialRegion) {
            
            MetricHyperCube newCube = new MetricHyperCube(
                    mhc.getRoutingMinValues(),
                    mhc.getRoutingMaxValues(),
                    mhc.getCapabilityMinValues(),
                    mhc.getCapabilityMaxValues(),
                    mhc.getMinimizeFlags(),
                    newSpatialRegion,
                    mhc.getProviderWeight(),
                    mhc.getDensityCenterX(),     
                    mhc.getDensityCenterY(),     
                    mhc.getQosCenterOfMass()
            );

            if (aggregatedState instanceof ServiceOffer offer) {
                return ServiceOffer.createWithUpdatedRegion(offer, newCube);
            }
            return new SubscriptionWithRegion(newCube);
        }
        return super.createCandidateSubscription(aggregatedState, newRegionPayload);
    }
}
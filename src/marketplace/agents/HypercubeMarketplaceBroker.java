package marketplace.agents;

import simulator.core.Location;
import simulator.regions.Region;
import simulator.regions.SpatialRegion;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.optimization.WeightedUtilityStrategy;
import marketplace.topology.store.MarketplaceRegionStore;

public class HypercubeMarketplaceBroker extends AbstractMarketplaceBroker {

    public HypercubeMarketplaceBroker(String name, double threshold) {
        super(name, threshold);
        this.selectionStrategy = new WeightedUtilityStrategy();
    }

    public HypercubeMarketplaceBroker(String name, Location p1, Location p2, double threshold) {
        super(name, p1, p2, threshold);
        this.selectionStrategy = new WeightedUtilityStrategy();
    }

    @Override
    protected RegionSubscriptionStore createStore(boolean forceSingleRegion, double threshold) {
        return new MarketplaceRegionStore(threshold);
    }

    @Override
    protected SubscriptionWithRegion createCandidateSubscription(SubscriptionWithRegion aggregatedState, Region newRegionPayload) {
        if (aggregatedState.getRegion() instanceof MetricHyperCube mhc && newRegionPayload instanceof SpatialRegion newSpatialRegion) {
            
            // Utilize the Tri-State Propagation Constructor to carry the probabilistic state upward
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
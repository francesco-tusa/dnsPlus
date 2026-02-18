package marketplace.agents;

import simulator.core.Location;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.topology.store.HypercubeRegionStore;
import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.optimization.WeightedUtilityStrategy;

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
        return new HypercubeRegionStore(threshold);
    }

    @Override
    protected SubscriptionWithRegion createCandidateSubscription(SubscriptionWithRegion aggregatedState, Region newRegionPayload) {
        if (aggregatedState.getRegion() instanceof MetricHyperCube mhc) {
            
            MetricHyperCube newCube = new MetricHyperCube(
                    mhc.getMinValues(),
                    mhc.getMaxValues(),
                    mhc.getMinimizeFlags(),
                    newRegionPayload
            );

            if (aggregatedState instanceof ServiceOffer offer) {
                return ServiceOffer.createAggregated(offer.getServiceId(), newCube);
            }
            return new SubscriptionWithRegion(newCube);
        }
        return super.createCandidateSubscription(aggregatedState, newRegionPayload);
    }
}
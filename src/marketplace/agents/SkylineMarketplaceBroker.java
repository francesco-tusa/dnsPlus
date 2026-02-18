package marketplace.agents;

import simulator.core.Location;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.RegionSubscriptionStore;
import marketplace.topology.store.SkylineRegionStore;
import marketplace.common.SkylineHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.optimization.SkylineUtilityStrategy;

public class SkylineMarketplaceBroker extends AbstractMarketplaceBroker {

    public SkylineMarketplaceBroker(String name, double threshold) {
        super(name, threshold);
        this.selectionStrategy = new SkylineUtilityStrategy();
    }

    public SkylineMarketplaceBroker(String name, Location p1, Location p2, double threshold) {
        super(name, p1, p2, threshold);
        this.selectionStrategy = new SkylineUtilityStrategy();
    }

    @Override
    protected RegionSubscriptionStore createStore(boolean forceSingleRegion, double threshold) {
        return new SkylineRegionStore(threshold);
    }

    @Override
    protected SubscriptionWithRegion createCandidateSubscription(SubscriptionWithRegion aggregatedState, Region newRegionPayload) {
        if (aggregatedState.getRegion() instanceof SkylineHyperCube shc) {
            
            // Re-anchor the Skyline to the new geographic bounding box (newRegionPayload)
            SkylineHyperCube newCube = new SkylineHyperCube(
                    shc.getParetoFrontier().get(0), 
                    shc.getMinimizeFlags(), 
                    newRegionPayload
            );
            
            // Safely merge the rest of the Pareto frontier points
            newCube.expand(shc);

            if (aggregatedState instanceof ServiceOffer offer) {
                return ServiceOffer.createSkylineAggregated(offer.getServiceId(), offer.getQosMetrics(), newCube);
            }
            return new SubscriptionWithRegion(newCube);
        }
        return super.createCandidateSubscription(aggregatedState, newRegionPayload);
    }
}
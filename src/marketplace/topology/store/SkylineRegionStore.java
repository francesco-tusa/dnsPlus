package marketplace.topology.store;

import simulator.core.TreeNode;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.StoreUpdate;
import marketplace.events.ServiceOffer;
import marketplace.common.SkylineHyperCube;
import marketplace.common.MarketplaceMetricSchema;

public class SkylineRegionStore extends AbstractMarketplaceRegionStore {

    public SkylineRegionStore(double threshold) {
        super(threshold); 
    }

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        // Intercept initial offer injection to convert to Skyline architecture
        if (sub instanceof ServiceOffer offer && !(sub.getRegion() instanceof SkylineHyperCube)) {
            
            int dim = MarketplaceMetricSchema.KEYS.length;
            double[] initialMetrics = new double[dim];
            boolean[] flags = new boolean[dim];
            
            for (int i = 0; i < dim; i++) {
                String key = MarketplaceMetricSchema.KEYS[i];
                flags[i] = MarketplaceMetricSchema.DIRECTIONS.get(key);
                initialMetrics[i] = offer.getQosMetrics().getOrDefault(key, flags[i] ? 0.0 : Double.MAX_VALUE);
            }
            
            SkylineHyperCube shc = new SkylineHyperCube(initialMetrics, flags, offer.getRegion());
            sub = ServiceOffer.createSkylineAggregated(offer.getServiceId(), offer.getQosMetrics(), shc);
        }
        return super.addOrUpdate(source, sub);
    }

    @Override
    protected boolean shouldMerge(simulator.regions.Region accumulator, SubscriptionWithRegion existing) {
        // 1. Calculate Spatial FPR using the shared base class utility
        double spatialFpr = calculateSpatialFpr(accumulator, existing);

        // 2. We ONLY evaluate spatial threshold. 
        // QoS FPR is inherently zero because the Pareto Skyline explicitly prevents QoS dilution.
        return spatialFpr <= this.getThreshold();
    }
}
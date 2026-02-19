package marketplace.topology.store;

import simulator.core.TreeNode;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.StoreUpdate;
import marketplace.common.SkylineHyperCube;
import marketplace.events.ServiceOffer;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.population.ProviderProfileGenerator;

public class SkylineRegionStore extends AbstractMarketplaceRegionStore {

    private final double maxTierAreaRatio;

    public SkylineRegionStore(double threshold) {
        super(threshold);
        this.maxTierAreaRatio = calculateSafeTierRatio();
    }

    private double calculateSafeTierRatio() {
        double fogEdgeRatio = Math.pow(ProviderProfileGenerator.RADIUS_FOG_MAX / ProviderProfileGenerator.RADIUS_EDGE, 2);   
        double cloudFogRatio = Math.pow(ProviderProfileGenerator.RADIUS_CLOUD_MAX / ProviderProfileGenerator.RADIUS_FOG_MAX, 2); 
        return Math.min(fogEdgeRatio, cloudFogRatio) * 0.25; 
    }

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
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
            // Use the factory to preserve provider Location and Radius
            sub = ServiceOffer.createSkylineOffer(offer, shc);
        }
        return super.addOrUpdate(source, sub);
    }

    @Override
    protected SubscriptionWithRegion wrapAggregatedRegion(SubscriptionWithRegion originalSub, Region accumulator, int mergedCount, int absorbedCount) {
        if (originalSub instanceof ServiceOffer offer && accumulator instanceof SkylineHyperCube shc) {
            return ServiceOffer.createSkylineAggregated(offer.getServiceId(), offer.getQosMetrics(), shc);
        }
        return new SubscriptionWithRegion(accumulator);
    }

   @Override
    protected boolean shouldMerge(Region accumulator, SubscriptionWithRegion existing) {
        
        // 1. TIER ISOLATION: Prevent Macro regions (Cloud) from swallowing Micro regions (Edge/Fog)
        double area1 = Math.max(accumulator.getArea(), 1e-9);
        double area2 = Math.max(existing.getRegion().getArea(), 1e-9);
        double areaRatio = Math.max(area1 / area2, area2 / area1);

        if (areaRatio > this.maxTierAreaRatio) {
            return false; // Cross-tier detected. Force parallel routing branches.
        }

        // 2. SAME-TIER EVALUATION: Evaluate standard spatial bounds
        double spatialFpr = calculateSpatialFpr(accumulator, existing);
        
        // If threshold is 0.0, this safely returns true ONLY for perfect 0.0 overlaps
        return spatialFpr <= this.mergeThreshold; 
    }
}
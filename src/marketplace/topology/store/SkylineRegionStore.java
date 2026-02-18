package marketplace.topology.store;

import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.StoreUpdate;
import simulator.core.TreeNode;
import marketplace.events.ServiceOffer;
import marketplace.common.SkylineHyperCube;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.population.ProviderProfileGenerator;

public class SkylineRegionStore extends AbstractMarketplaceRegionStore {
    
    // Dynamically derived threshold for Tier Isolation
    private final double maxTierAreaRatio;

    public SkylineRegionStore(double threshold) {
        super(threshold); 
        this.maxTierAreaRatio = calculateSafeTierRatio();
    }

    /**
     * Mathematically derives the maximum safe Area Ratio to prevent cross-tier merging.
     * Based directly on the centralized FaaS continuum strata (Edge -> Fog -> Cloud).
     */
    private double calculateSafeTierRatio() {
        // Area ratio is proportional to Radius Squared.
        // Ratio between Fog and Edge = (3.0 / 0.25)^2 = 144.0
        double fogEdgeRatio = Math.pow(ProviderProfileGenerator.RADIUS_FOG_MAX / ProviderProfileGenerator.RADIUS_EDGE, 2);   
        
        // Ratio between Cloud and Fog = (20.0 / 3.0)^2 = 44.44
        double cloudFogRatio = Math.pow(ProviderProfileGenerator.RADIUS_CLOUD_MAX / ProviderProfileGenerator.RADIUS_FOG_MAX, 2); 
        
        // Find the smallest geometric gap between tiers (which is 44.44 in this setup)
        double minTierGap = Math.min(fogEdgeRatio, cloudFogRatio);

        // Allow same-tier MBRs to aggregate and expand up to 25% of the next tier's size.
        // 44.44 * 0.25 = ~11.1. Any area difference > 11.1x is strictly blocked as cross-tier.
        return minTierGap * 0.25; 
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
        
        // 1. DYNAMIC TIER ISOLATION: Prevent Macro regions (Cloud) from swallowing Micro regions (Edge/Fog)
        double area1 = Math.max(accumulator.getArea(), 1e-9);
        double area2 = Math.max(existing.getRegion().getArea(), 1e-9);
        
        double areaRatio = Math.max(area1 / area2, area2 / area1);

        // If the area difference exceeds our dynamically calculated FaaS continuum gap, 
        // they belong to different hardware tiers. Force them into parallel routing branches.
        if (areaRatio > this.maxTierAreaRatio) {
            return false; 
        }

        // 2. SAME-TIER AGGREGATION: If they belong to the same tier, evaluate standard geographic clustering
        double spatialFpr = calculateSpatialFpr(accumulator, existing);

        // QoS FPR is inherently zero because the Pareto Skyline natively compresses SLAs safely.
        return spatialFpr <= this.getThreshold();
    }
}
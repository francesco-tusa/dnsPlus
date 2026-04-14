package marketplace.population.tiers;

import java.util.Random;

import marketplace.config.MarketplaceConfig;
import marketplace.population.ProviderProfileGenerator.ProviderPolicy;

public class FogProviderTier implements ProviderTierStrategy {
    @Override
    public double generateCoverageRadius(Random r) {
        MarketplaceConfig cfg = MarketplaceConfig.get();
        // Gaussian Distribution with strict physical bounds
        double radius = cfg.fogRadiusMean + (r.nextGaussian() * cfg.fogRadiusStdDev);
        return Math.max(cfg.fogRadiusMinClamp, Math.min(cfg.fogRadiusMaxClamp, radius));
    }

    @Override
    public double generateBaseComputeLatency(Random r) {
        return 10.0 + (r.nextDouble() * 5.0);
    }

    @Override
    public double generateBaseBandwidth(Random r) {
        return 200.0 + (r.nextDouble() * 300.0);
    }

    @Override
    public double getBaseCost() {
        return 6.00;
    } // Regional premium

    @Override
    public double getBaseReliability() {
        return 0.99;
    }

    // Fog: Medium orchestration, standard virtualized memory bus
    @Override
    public double getControlPlaneProvisioningMs() {
        return 200.0;
    }

    @Override
    public double getMemorySpeedMultiplier() {
        return 2.0;
    }

    @Override
    public double getRegionClippingMultiplier() {
        return 1.2;
    }

    @Override 
    public ProviderPolicy generateProviderPolicy(Random r) {
        double p = r.nextDouble();

        if (MarketplaceConfig.get().enableQosPolicySkew) {
            // Force 90% of Fog nodes to be purely WARM_OPTIMIZED.
            // This creates highly reliable/fast nodes but at a massive cost premium.
            if (p < 0.90) return ProviderPolicy.WARM_OPTIMIZED;
            return ProviderPolicy.BALANCED; 
        } else {
            // BASELINE: 50% Balanced, 30% Warm, 20% Cost
            if (p < 0.50) return ProviderPolicy.BALANCED;
            if (p < 0.80) return ProviderPolicy.WARM_OPTIMIZED;
            return ProviderPolicy.COST_OPTIMIZED;
        }
    }
}
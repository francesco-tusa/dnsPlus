package marketplace.population.tiers;

import java.util.Random;

import marketplace.config.MarketplaceConfig;
import marketplace.population.ProviderProfileGenerator.ProviderPolicy;

public class EdgeProviderTier implements ProviderTierStrategy {
    @Override
    public double generateCoverageRadius(Random r) {
        MarketplaceConfig cfg = MarketplaceConfig.get();
        return cfg.edgeRadiusMin + (r.nextDouble() * (cfg.edgeRadiusMax - cfg.edgeRadiusMin));
    }

    @Override
    public double generateBaseComputeLatency(Random r) {
        return 15.0 + (r.nextDouble() * 10.0);
    }

    @Override
    public double generateBaseBandwidth(Random r) {
        return 20.0 + (r.nextDouble() * 80.0);
    }

    @Override
    public double getBaseCost() {
        return 12.50;
    } // High perimeter premium

    @Override
    public double getBaseReliability() {
        return 0.95;
    }

    // Edge: Fast local orchestration (e.g., K3s/containerd), but weaker CPU/Memory
    // bus
    @Override
    public double getControlPlaneProvisioningMs() {
        return 50.0;
    }

    @Override
    public double getMemorySpeedMultiplier() {
        return 3.5;
    }

    @Override
    public double getRegionClippingMultiplier() {
        return 1.0;
    }

    @Override 
    public ProviderPolicy generateProviderPolicy(Random r) {
        double p = r.nextDouble();
        
        if (MarketplaceConfig.get().enableQosPolicySkew) {
            // Force 85% of Edge nodes to be purely COST_OPTIMIZED.
            // This introduces massive latency penalties (Cold Starts) at the spatial perimeter.
            if (p < 0.85) return ProviderPolicy.COST_OPTIMIZED;
            if (p < 0.95) return ProviderPolicy.BALANCED;
            return ProviderPolicy.WARM_OPTIMIZED;
        } else {
            // BASELINE: 60% Warm, 30% Balanced, 10% Cost
            if (p < 0.60) return ProviderPolicy.WARM_OPTIMIZED;
            if (p < 0.90) return ProviderPolicy.BALANCED;
            return ProviderPolicy.COST_OPTIMIZED;
        }
    }
}
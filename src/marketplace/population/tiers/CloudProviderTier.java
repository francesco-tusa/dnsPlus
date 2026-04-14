package marketplace.population.tiers;

import java.util.Random;

import marketplace.config.MarketplaceConfig;
import marketplace.population.ProviderProfileGenerator.ProviderPolicy;

public class CloudProviderTier implements ProviderTierStrategy {
    @Override
    public double generateCoverageRadius(Random r) {
        MarketplaceConfig cfg = MarketplaceConfig.get();
        // Gaussian Distribution representing macro-level routing latency envelopes
        double radius = cfg.cloudRadiusMean + (r.nextGaussian() * cfg.cloudRadiusStdDev);
        return Math.max(cfg.cloudRadiusMinClamp, Math.min(cfg.cloudRadiusMaxClamp, radius));
    }

    @Override
    public double generateBaseComputeLatency(Random r) {
        return 2.0 + (r.nextDouble() * 5.0);
    }

    @Override
    public double generateBaseBandwidth(Random r) {
        return 800.0 + (r.nextDouble() * 200.0);
    }

    @Override
    public double getBaseCost() {
        return 2.50;
    } // Base AWS Lambda equivalent

    @Override
    public double getBaseReliability() {
        return 0.999;
    }

    // Cloud: Slow orchestration (Heavy k8s/Firecracker control plane), fast memory
    // bus
    @Override
    public double getControlPlaneProvisioningMs() {
        return 500.0;
    }

    @Override
    public double getMemorySpeedMultiplier() {
        return 1.2;
    }

    @Override
    public double getRegionClippingMultiplier() {
        return 1.5;
    }

    @Override
    public ProviderPolicy generateProviderPolicy(Random r) {
        return (r.nextDouble() < 0.80) ? ProviderPolicy.WARM_OPTIMIZED : ProviderPolicy.BALANCED;
    }
}
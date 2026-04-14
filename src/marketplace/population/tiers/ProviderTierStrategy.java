package marketplace.population.tiers;

import java.util.Random;

import marketplace.population.ProviderProfileGenerator.ProviderPolicy;

public interface ProviderTierStrategy {
    double generateCoverageRadius(Random r);

    double generateBaseComputeLatency(Random r);
    double generateBaseBandwidth(Random r);
    double getBaseCost();
    double getBaseReliability();

    double getControlPlaneProvisioningMs();
    double getMemorySpeedMultiplier();
    double getRegionClippingMultiplier();

    ProviderPolicy generateProviderPolicy(Random r);
}
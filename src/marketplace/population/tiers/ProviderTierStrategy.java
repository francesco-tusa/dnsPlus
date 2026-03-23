package marketplace.population.tiers;

import java.util.Random;

public interface ProviderTierStrategy {
    double getCoverageRadius();
    double generateBaseComputeLatency(Random random);
    double generateBaseBandwidth(Random random);
    double getBaseCost();
    double getBaseReliability();
}
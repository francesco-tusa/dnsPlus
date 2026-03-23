package marketplace.population;

import java.util.Map;
import java.util.Random;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.population.tiers.ProviderTierStrategy;

public class StandardProviderProfileGenerator implements ProviderProfileGenerator {
    private final Random random;

    public StandardProviderProfileGenerator(Random seededRandom) { 
        this.random = seededRandom; 
    }

    @Override
    public Map<String, Double> generateProfile(ProviderTierStrategy tier, ProviderPolicy policy, long functionId) {
        // Ignores functionId. Base cost and hardware boundaries are defined by the Tier Strategy.
        double latency = tier.generateBaseComputeLatency(random);
        double cost = tier.getBaseCost();
        double reliability = tier.getBaseReliability();

        switch (policy) {
            case WARM_OPTIMIZED -> { 
                cost *= 2.5; 
                reliability += 0.005; 
            }
            case COST_OPTIMIZED -> { 
                latency += 150.0; 
                cost *= 0.5; 
                reliability = Math.max(0.90, reliability - 0.02); // Restored floor bound
            }
            case BALANCED -> {
                latency += 25.0;
            }
        }

        return Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, latency,
            MarketplaceMetricSchema.METRIC_COST, cost,
            MarketplaceMetricSchema.METRIC_RELIABILITY, Math.min(1.0, reliability), // Ceiling bound preserved
            MarketplaceMetricSchema.METRIC_BANDWIDTH, tier.generateBaseBandwidth(random)
        );
    }
}
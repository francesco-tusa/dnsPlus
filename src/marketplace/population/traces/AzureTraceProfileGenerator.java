package marketplace.population.traces;

import java.util.Map;
import java.util.Random;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.population.ProviderProfileGenerator;
import marketplace.population.tiers.ProviderTierStrategy;
import marketplace.workload.traces.AzureTraceRepository;
import marketplace.workload.traces.AzureTraceRecord;

public class AzureTraceProfileGenerator implements ProviderProfileGenerator {
    private final Random random;
    private final AzureTraceRepository traceRepo;

    public AzureTraceProfileGenerator(Random seededRandom) { 
        this.random = seededRandom; 
        this.traceRepo = AzureTraceRepository.getInstance();
    }

    @Override
    public Map<String, Double> generateProfile(ProviderTierStrategy tier, ProviderPolicy policy, long functionId) {
        double latency = tier.generateBaseComputeLatency(random);
        double cost = tier.getBaseCost();
        double reliability = tier.getBaseReliability();

        switch (policy) {
            case WARM_OPTIMIZED -> { cost *= 2.5; reliability += 0.005; }
            case COST_OPTIMIZED -> { latency += 150.0; cost *= 0.5; reliability -= 0.02; }
            case BALANCED -> latency += 25.0;
        }

        // AZURE EXTENSION: Memory-Proportional Pricing mapped against QoS boundaries
        AzureTraceRecord record = traceRepo.getRecord(functionId);
        if (record != null) {
            double memoryProportion = Math.max(0.125, record.memory() / 1024.0); 
            cost *= memoryProportion;
        }

        return Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, latency,
            MarketplaceMetricSchema.METRIC_COST, cost,
            MarketplaceMetricSchema.METRIC_RELIABILITY, Math.min(1.0, reliability),
            MarketplaceMetricSchema.METRIC_BANDWIDTH, tier.generateBaseBandwidth(random)
        );
    }
}
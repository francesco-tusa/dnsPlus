package marketplace.population.traces;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import marketplace.common.MarketplaceMetricSchema;
import marketplace.config.MarketplaceConfig;
import marketplace.population.ProviderProfileGenerator;
import marketplace.population.tiers.CloudProviderTier;
import marketplace.population.tiers.ProviderTierStrategy;
import marketplace.workload.traces.AzureTraceRecord;
import marketplace.workload.traces.AzureTraceRepository;

public class AzureTraceProfileGenerator implements ProviderProfileGenerator {
    private final Random random;
    private final AzureTraceRepository repository;
    private final Set<Long> globalWarmCache; 

    public AzureTraceProfileGenerator(Random seededRandom) { 
        this.random = seededRandom; 
        this.repository = AzureTraceRepository.getInstance();
        
        MarketplaceConfig config = MarketplaceConfig.get();
        
        this.globalWarmCache = repository.getAllRecords().values().stream()
                .sorted((a, b) -> Long.compare(b.invocationCount(), a.invocationCount()))
                .limit(config.universalWarmCacheSize)
                .map(AzureTraceRecord::functionId)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, Double> generateProfile(ProviderTierStrategy tier, ProviderPolicy policy, long functionId) {
        
        double latency = tier.generateBaseComputeLatency(random);
        double cost = tier.getBaseCost();
        double reliability = tier.getBaseReliability();

        switch (policy) {
            case WARM_OPTIMIZED -> { cost *= 2.5; reliability += 0.005; }
            case COST_OPTIMIZED -> { latency += 150.0; cost *= 0.5; reliability = Math.max(0.90, reliability - 0.02); }
            case BALANCED -> { latency += 25.0; }
        }

        // Trace-Specific Software Physics
        AzureTraceRecord record = repository.getRecord(functionId);
        double memoryMb = (record != null) ? record.memory() : 128.0;

        boolean isUniversalWarm = globalWarmCache.contains(functionId);
        
        if (!isUniversalWarm) {
            double controlPlaneMs = tier.getControlPlaneProvisioningMs();
            double memorySpeedMultiplier = tier.getMemorySpeedMultiplier();
            
            double containerSpinUpMs = memoryMb * memorySpeedMultiplier;
            latency += (controlPlaneMs + containerSpinUpMs);
        }

        return Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, latency,
            MarketplaceMetricSchema.METRIC_COST, cost,
            MarketplaceMetricSchema.METRIC_RELIABILITY, Math.min(1.0, reliability), 
            MarketplaceMetricSchema.METRIC_BANDWIDTH, tier.generateBaseBandwidth(random)
        );
    }
}
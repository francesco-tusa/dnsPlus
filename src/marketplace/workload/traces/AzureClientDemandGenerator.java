package marketplace.workload.traces;

import java.util.Map;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.population.tiers.CloudProviderTier;
import marketplace.population.tiers.ProviderTierStrategy;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.ClientDemandProfile;
import utils.SimulationRandom;

public class AzureClientDemandGenerator implements ClientDemandGenerator {

    private final AzureTraceRepository repository;
    private final java.util.Random random;
    private final double configuredEdgeProb;
    private final double configuredBudgetProb;
    
    // Maintain a reference to the Cloud physics to establish the worst-case SLA baseline
    private final ProviderTierStrategy baselineCloudPhysics;

    public AzureClientDemandGenerator() {
        this.repository = AzureTraceRepository.getInstance();
        this.random = SimulationRandom.get();
        this.baselineCloudPhysics = new CloudProviderTier();

        marketplace.config.MarketplaceConfig config = marketplace.config.MarketplaceConfig.get();
        this.configuredEdgeProb = config.edgeWorkloadProbability;
        this.configuredBudgetProb = config.strictBudgetProbability;
    }

    @Override
    public ClientDemandProfile generateDemand(int clientIndex, long functionId) {
        
        AzureTraceRecord record = repository.getRecord(functionId);
        double durationMs = (record != null) ? record.duration() : 50.0;
        double memoryMb   = (record != null) ? record.memory()   : 128.0;
        double baseCostEstimator = (memoryMb / 1024.0) * (durationMs / 1000.0) * 16.0;

        // --- DYNAMIC WORKLOAD PROBABILITY DISTRIBUTION ---
        
        // 1. Safety Check: Ensure probabilities do not exceed 100%
        double safeEdgeProb = Math.min(this.configuredEdgeProb, 1.0);
        double safeBudgetProb = Math.min(this.configuredBudgetProb, 1.0 - safeEdgeProb);

        double p = random.nextDouble();
        
        // 2. Map 'p' to the three slices of the 0.0 -> 1.0 number line
        boolean isUltraCritical = p < safeEdgeProb; 
        boolean isInteractive   = p >= safeEdgeProb && p < (1.0 - safeBudgetProb);
        // The implicit "else" covers p >= (1.0 - safeBudgetProb), which is the Async Batch (Budget) workload.
        
        double strictLatencyBound;
        double maxBudget;
        Map<String, Double> weights;

        if (isUltraCritical) {
            // 1. ULTRA-CRITICAL (Corresponds to edgeWorkloadProbability)
            //strictLatencyBound = 5.0 + durationMs + 10.0;
            strictLatencyBound = 10.0 + durationMs + 20.0;
            maxBudget = Math.max(40.0, baseCostEstimator * 50.0); 
            weights = Map.of(
                MarketplaceMetricSchema.METRIC_LATENCY, 0.85, 
                MarketplaceMetricSchema.METRIC_COST, 0.05,
                MarketplaceMetricSchema.METRIC_RELIABILITY, 0.05,
                MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.05
            );
        }
        else if (isInteractive) {
            // 2. INTERACTIVE (The remaining middle percentage)
            //strictLatencyBound = 15.0 + durationMs + 100.0;
            strictLatencyBound = 25.0 + durationMs + 150.0;
            maxBudget = Math.max(4.0, baseCostEstimator * 5.0); 
            weights = Map.of(
                MarketplaceMetricSchema.METRIC_LATENCY, 0.65,
                MarketplaceMetricSchema.METRIC_COST, 0.15,
                MarketplaceMetricSchema.METRIC_RELIABILITY, 0.10,
                MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.10
            );
        } 
        else {
            // 3. ASYNC BATCH (Corresponds to strictBudgetProbability)
            double controlPlaneProvisioningMs = baselineCloudPhysics.getControlPlaneProvisioningMs();
            double ramColdStartPenalty = controlPlaneProvisioningMs + (memoryMb * baselineCloudPhysics.getMemorySpeedMultiplier());
            
            //strictLatencyBound = 2.0 + durationMs + ramColdStartPenalty + 2000.0;
            //maxBudget = Math.max(0.5, baseCostEstimator * 2.0);

            strictLatencyBound = 5.0 + durationMs + ramColdStartPenalty + 3000.0;
            maxBudget = Math.max(1.5, baseCostEstimator * 3.5);
            
            weights = Map.of(
                MarketplaceMetricSchema.METRIC_LATENCY, 0.10,
                MarketplaceMetricSchema.METRIC_COST, 0.70, // Cost dictates selection
                MarketplaceMetricSchema.METRIC_RELIABILITY, 0.15,
                MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.05
            );
        }

        Map<String, Double> constraints = Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, strictLatencyBound,
            MarketplaceMetricSchema.METRIC_COST, maxBudget,
            MarketplaceMetricSchema.METRIC_RELIABILITY, (isUltraCritical ? 0.90 : 0.95), // Tolerate slight drops for speed
            MarketplaceMetricSchema.METRIC_BANDWIDTH, deriveBandwidthRequirement(memoryMb)
        );

        return new ClientDemandProfile(constraints, weights);
    }

    private double deriveBandwidthRequirement(double memoryMb) {
        if (memoryMb > 1024.0) return 50.0; 
        if (memoryMb > 256.0) return 15.0;  
        return 5.0;                         
    }
}
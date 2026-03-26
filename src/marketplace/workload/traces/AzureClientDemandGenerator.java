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
    
    // Maintain a reference to the Cloud physics to establish the worst-case SLA baseline
    private final ProviderTierStrategy baselineCloudPhysics;

    public AzureClientDemandGenerator() {
        this.repository = AzureTraceRepository.getInstance();
        this.random = SimulationRandom.get();
        this.baselineCloudPhysics = new CloudProviderTier();
    }

    @Override
    public ClientDemandProfile generateDemand(int clientIndex, long functionId) {
        
        AzureTraceRecord record = repository.getRecord(functionId);
        double durationMs = (record != null) ? record.duration() : 50.0;
        double memoryMb   = (record != null) ? record.memory()   : 128.0;
        double baseCostEstimator = (memoryMb / 1024.0) * (durationMs / 1000.0) * 16.0;

        // --- Models three types of requests ---
        double p = random.nextDouble();
        boolean isUltraCritical = p < 0.10; // 10% AR/VR, Autonomous Systems
        boolean isInteractive   = p >= 0.10 && p < 0.40; // 30% Web APIs
        
        double strictLatencyBound;
        double maxBudget;
        Map<String, Double> weights;

        if (isUltraCritical) {
            // 1. ULTRA-CRITICAL: Unforgiving Latency, Blank Check Budget
            strictLatencyBound = 5.0 + durationMs + 10.0; 
            
            // BUDGET FIX: Raised to $40.00 to safely clear the Warm Edge Cost ($31.25)
            maxBudget = Math.max(40.0, baseCostEstimator * 50.0); 
            
            weights = Map.of(
                MarketplaceMetricSchema.METRIC_LATENCY, 0.85, 
                MarketplaceMetricSchema.METRIC_COST, 0.05,
                MarketplaceMetricSchema.METRIC_RELIABILITY, 0.05,
                MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.05
            );
        }
        else if (isInteractive) {
            // 2. INTERACTIVE: Standard Edge/Fog bounds. 
            strictLatencyBound = 15.0 + durationMs + 100.0;
            maxBudget = Math.max(4.0, baseCostEstimator * 5.0); 
            
            weights = Map.of(
                MarketplaceMetricSchema.METRIC_LATENCY, 0.65,
                MarketplaceMetricSchema.METRIC_COST, 0.15,
                MarketplaceMetricSchema.METRIC_RELIABILITY, 0.10,
                MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.10
            );
        } 
        else {
            // 3. ASYNC BATCH: Cloud Baseline + Cold Start Penalty
            // POLYMORPHIC ALIGNMENT: Dynamically fetch the Cloud orchestrator penalty
            double controlPlaneProvisioningMs = baselineCloudPhysics.getControlPlaneProvisioningMs();
            double ramColdStartPenalty = controlPlaneProvisioningMs + (memoryMb * baselineCloudPhysics.getMemorySpeedMultiplier());
            
            strictLatencyBound = 2.0 + durationMs + ramColdStartPenalty + 2000.0;
            maxBudget = Math.max(0.5, baseCostEstimator * 2.0); // Pennies on the dollar
            
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
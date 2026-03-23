package marketplace.workload.traces;

import java.util.Map;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.ClientDemandProfile;

public class AzureClientDemandGenerator implements ClientDemandGenerator {

    private final AzureTraceRepository repository;

    public AzureClientDemandGenerator() {
        this.repository = AzureTraceRepository.getInstance();
    }

    @Override
    public ClientDemandProfile generateDemand(int clientIndex, long functionId) {
        
        // 1. Fetch the exact physical characteristics of the requested function
        // (Assuming your record has duration() and memory() accessors)
        AzureTraceRecord record = repository.getRecord(functionId);

        // Fallback to prevent crashes if a bad ID slips through
        double durationMs = (record != null) ? record.duration() : 50.0;
        double memoryMb   = (record != null) ? record.memory()   : 128.0;

        // 2. Derive SLA Bounds mathematically from the trace data
        // Latency SLA = Baseline Duration + Queue/Network Jitter Buffer
        double strictLatencyBound = durationMs * 2.5; 
        
        // Cost Bound = A function of Memory Allocated (e.g., GB-seconds pricing model)
        // Base Cost ~ (MB / 1024) * (Ms / 1000) * ComputeMultiplier
        double baseCostEstimator = (memoryMb / 1024.0) * (durationMs / 1000.0) * 16.0;
        // The client is willing to pay up to 3x the base compute cost for premium edge routing
        double maxBudget = Math.max(0.5, baseCostEstimator * 3.0); 

        // 3. Construct Empirical Constraints
        Map<String, Double> constraints = Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, strictLatencyBound,
            MarketplaceMetricSchema.METRIC_COST, maxBudget,
            MarketplaceMetricSchema.METRIC_RELIABILITY, 0.95,
            MarketplaceMetricSchema.METRIC_BANDWIDTH, deriveBandwidthRequirement(memoryMb)
        );

        // 4. Construct Dynamic Weights (Multi-Objective Preferences)
        Map<String, Double> weights;
        if (durationMs < 100.0) {
            // Highly sensitive, fast-executing functions heavily prioritize Latency placement (e.g., Edge)
            weights = Map.of(
                MarketplaceMetricSchema.METRIC_LATENCY, 0.70,
                MarketplaceMetricSchema.METRIC_COST, 0.10,
                MarketplaceMetricSchema.METRIC_RELIABILITY, 0.10,
                MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.10
            );
        } else {
            // Long-running batch compute heavily prioritizes Cost placement (e.g., Cloud Spot)
            weights = Map.of(
                MarketplaceMetricSchema.METRIC_LATENCY, 0.15,
                MarketplaceMetricSchema.METRIC_COST, 0.65,
                MarketplaceMetricSchema.METRIC_RELIABILITY, 0.15,
                MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.05
            );
        }

        return new ClientDemandProfile(constraints, weights);
    }

    /**
     * Estimates required link capacity based on the memory footprint of the container/function state.
     */
    private double deriveBandwidthRequirement(double memoryMb) {
        if (memoryMb > 1024.0) return 50.0; // High memory implies large payload/state transfer (e.g., ETL)
        if (memoryMb > 256.0) return 15.0;  // Medium payload
        return 5.0;                         // Lightweight functions need minimal bandwidth
    }
}
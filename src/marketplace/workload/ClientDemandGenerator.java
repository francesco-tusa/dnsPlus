package marketplace.workload;

import java.util.Map;
import java.util.Random;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.config.MarketplaceConfig;

public class ClientDemandGenerator {
    
    private final Random random;

    public ClientDemandGenerator(Random seededRandom) {
        this.random = seededRandom;
    }

    public ClientDemandProfile generateDemand(int clientIndex) {
        Map<String, Double> constraints;
        Map<String, Double> weights;

        MarketplaceConfig config = MarketplaceConfig.get();

        // 1. Dynamic Probabilistic Class Distribution
        double edgeProb = config.edgeWorkloadProbability;
        double remainingProb = 1.0 - edgeProb;
        double fogProb = remainingProb * 0.60; 

        double rand = random.nextDouble();
        int classType;
        
        if (rand < edgeProb) {
            classType = 0; // CLASS 0: EDGE (Latency-Sensitive, Low Payload)
        } else if (rand < (edgeProb + fogProb)) {
            classType = 2; // CLASS 2: FOG (Balanced, Video/Stream Analytics)
        } else {
            classType = 1; // CLASS 1: CLOUD (Budget/Compute Heavy, High Data)
        }

        // 2. Assign Constraints and SLA Weights (Weights MUST sum to 1.0)
        switch (classType) {
            case 0: // EDGE (e.g., Drone Telemetry / Industrial IoT)
                constraints = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 55.0 + (random.nextDouble() * 10.0), 
                    MarketplaceMetricSchema.METRIC_COST, 90.0 + (random.nextDouble() * 20.0), // Can afford edge premiums
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.90,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 15.0 // Minimal bandwidth required for telemetry
                );
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.65, 
                    MarketplaceMetricSchema.METRIC_COST, 0.15,
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.10,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.10
                );
                break;

            case 1: // CLOUD (e.g., Big Data ETL / ML Inference)
                double maxLatency = 400.0 + (random.nextDouble() * 200.0); // Relaxed latency
                double maxCost;
                
                // --- BUDGET SENSITIVITY SWEEP CONTROL ---
                if (random.nextDouble() <= config.strictBudgetProbability) {
                    maxCost = 10.0; // Strict Budget (Forces selection of COST_OPTIMIZED hyperscale nodes)
                } else {
                    maxCost = 50.0; // Relaxed Budget (Can afford Provisioned Concurrency)
                }

                constraints = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, maxLatency, 
                    MarketplaceMetricSchema.METRIC_COST, maxCost,
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.99,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 100.0 // Requires high throughput
                );
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.05,
                    MarketplaceMetricSchema.METRIC_COST, 0.70, // Overwhelming bias toward financial optimization
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.15,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.10
                );
                break;

            case 2: // FOG (e.g., Real-Time Video Analytics / AR Rendering)
            default:
                constraints = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 90.0 + (random.nextDouble() * 30.0),
                    MarketplaceMetricSchema.METRIC_COST, 30.0 + (random.nextDouble() * 10.0),
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.95,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 50.0 // Video feeds require strict minimum bandwidth
                );
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.30, 
                    MarketplaceMetricSchema.METRIC_COST, 0.30,   
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.20,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.20 // Bandwidth is highly prioritized
                );
                break;
        }
        
        return new ClientDemandProfile(constraints, weights);
    }
}
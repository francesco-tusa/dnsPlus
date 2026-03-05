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
        
        // Divide the remaining probability between Fog and Cloud workloads
        double remainingProb = 1.0 - edgeProb;
        double fogProb = remainingProb * 0.60; 

        double rand = random.nextDouble();
        int classType;
        
        if (rand < edgeProb) {
            classType = 0; // CLASS 1: EDGE (Latency-Sensitive, Low Payload)
        } else if (rand < (edgeProb + fogProb)) {
            classType = 2; // CLASS 3: FOG (Balanced, Video/Stream Analytics)
        } else {
            classType = 1; // CLASS 2: CLOUD (Budget/Compute Heavy, High Data)
        }

        // 2. Assign constraints and SLA weights based on the chosen class
        switch (classType) {
            case 0: // EDGE (e.g., Drone Telemetry / Industrial IoT)
                constraints = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 55.0 + (random.nextDouble() * 10.0), 
                    MarketplaceMetricSchema.METRIC_COST, 90.0 + (random.nextDouble() * 20.0),
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.90,
                    // Minimal bandwidth required for small telemetry JSON payloads
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 15.0 
                );
                // Weights heavily favor latency
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.65, 
                    MarketplaceMetricSchema.METRIC_COST, 0.15,
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.10,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.10
                );
                break;

            case 1: // CLOUD (e.g., Big Data ETL / ML Inference)
                double maxLatency = 400.0 + (random.nextDouble() * 200.0); // Relaxed latency for Cloud
                double maxCost;
                
                // --- BUDGET SENSITIVITY SWEEP ---
                double strictBudgetProb = config.strictBudgetProbability;
                
                if (random.nextDouble() <= strictBudgetProb) {
                    maxCost = 10.0; // Strict $10.00 / 1M Invocations
                } else {
                    maxCost = 50.0; // Relaxed Budget
                }

                constraints = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, maxLatency, 
                    MarketplaceMetricSchema.METRIC_COST, maxCost,
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.99,
                    // High bandwidth required to move bulk data payloads to the container
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 100.0 
                );
                
                // Weights overwhelmingly favor cost optimization (for our mathematical proofs)
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.05,
                    MarketplaceMetricSchema.METRIC_COST, 0.70, 
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
                    // Video feeds require strict minimum bandwidth
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 50.0 
                );
                // Balanced weights, with a strong emphasis on bandwidth
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.30, 
                    MarketplaceMetricSchema.METRIC_COST, 0.30,   
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.20,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.20
                );
                break;
        }
        
        return new ClientDemandProfile(constraints, weights);
    }
}
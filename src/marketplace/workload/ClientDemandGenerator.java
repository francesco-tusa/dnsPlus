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
        double fogProb = (edgeProb == 0.0) ? 0.0 : remainingProb * 0.40;

        double rand = random.nextDouble();
        int classType;
        
        if (rand < edgeProb) {
            classType = 0; // CLASS 0: EDGE (Latency-Sensitive, Low Payload)
        } else if (rand < (edgeProb + fogProb)) {
            classType = 2; // CLASS 2: FOG (Balanced, Video/Stream Analytics)
        } else {
            classType = 1; // CLASS 1: CLOUD (Budget/Compute Heavy, High Data)
        }

        // 2. Assign constraints and SLA weights based on the chosen class
        switch (classType) {
            case 0: // EDGE 
                constraints = Map.of(
                    // ORGANIC LATENCY WALL: 30.0ms
                    // Edge: ~20ms compute + ~2ms local network = ~22ms -> PASSES
                    // Cloud: ~5ms compute + ~45ms long-haul network = ~50ms -> FAILS
                    MarketplaceMetricSchema.METRIC_LATENCY, 30.0, 
                    MarketplaceMetricSchema.METRIC_COST, 150.0,
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.90,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 15.0 
                );
                // Weights heavily favor latency
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.80, // Increased weight
                    MarketplaceMetricSchema.METRIC_COST, 0.05,
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.10,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.05
                );
                break;

            case 1: // CLOUD (e.g., Big Data ETL / ML Inference)
                double maxLatency = 500.0; // Very relaxed latency
                double maxCost;
                
                // --- BUDGET SENSITIVITY SWEEP ---
                double strictBudgetProb = config.strictBudgetProbability;
                
                if (random.nextDouble() <= strictBudgetProb) {
                    maxCost = 4.5; // STRICT BUDGET: Forces routing to Cloud COST/BALANCED nodes. Edge will fail.
                } else {
                    maxCost = 80.0; // Relaxed Budget
                }

                constraints = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, maxLatency, 
                    MarketplaceMetricSchema.METRIC_COST, maxCost,
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.99,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 100.0 
                );
                
                // Weights overwhelmingly favor cost optimization
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.05,
                    MarketplaceMetricSchema.METRIC_COST, 0.80, // Increased weight
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.10,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.05
                );
                break;

            case 2: // FOG (e.g., Real-Time Video Analytics / AR Rendering)
            default:
                constraints = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 80.0,
                    MarketplaceMetricSchema.METRIC_COST, 40.0,
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.95,
                    // STRICT BANDWIDTH: Only Fog/Cloud nodes can process video streams
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 80.0 
                );
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.25, 
                    MarketplaceMetricSchema.METRIC_COST, 0.25,   
                    MarketplaceMetricSchema.METRIC_RELIABILITY, 0.25,
                    MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.25
                );
                break;
        }
        
        return new ClientDemandProfile(constraints, weights);
    }
}
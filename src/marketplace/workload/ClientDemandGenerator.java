package marketplace.workload;

import java.util.Map;
import java.util.Random;
import marketplace.common.MarketplaceMetricSchema;

/**
 * Generates stratified demand profiles for Marketplace Clients.
 * Divides the client population into distinct operational classes (Edge-bound, Cloud-bound, Balanced).
 */
public class ClientDemandGenerator {
    
    private final Random random;

    public ClientDemandGenerator(Random seededRandom) {
        this.random = seededRandom;
    }

    public ClientDemandProfile generateDemand(int clientIndex) {
        Map<String, Double> constraints;
        Map<String, Double> weights;

        // Divide clients evenly into 3 distinct behavioral classes
        int classType = clientIndex % 3;

        switch (classType) {
            case 0:
                // CLASS 1: Ultra-Low Latency (EDGE Target)
                // Math: Requires < 15ms. 
                // Cloud fails inherently (20ms minimum intrinsic). 
                // Fog fails if > 3 degrees away. Edge wins easily.
                constraints = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 15.0, 
                    MarketplaceMetricSchema.METRIC_COST, 100.0  
                );
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.95, 
                    MarketplaceMetricSchema.METRIC_COST, 0.05
                );
                break;

            case 1:
                // CLASS 2: Budget Constrained (CLOUD Target)
                // Math: Strict cost < $8.0. 
                // Fog ($10+) and Edge ($40+) instantly fail. Cloud wins.
                constraints = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 250.0, 
                    MarketplaceMetricSchema.METRIC_COST, 8.0       
                );
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.1, 
                    MarketplaceMetricSchema.METRIC_COST, 0.9       
                );
                break;

            case 2:
            default:
                // CLASS 3: Balanced / Web Backend (FOG Target)
                // Math: Cost limit drops expensive Edge. 
                // Latency limit (< 45ms) drops Cloud if > 12 degrees away.
                // Fog sits in the mathematical sweet spot.
                constraints = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 45.0,
                    MarketplaceMetricSchema.METRIC_COST, 25.0
                );
                weights = Map.of(
                    MarketplaceMetricSchema.METRIC_LATENCY, 0.6, 
                    MarketplaceMetricSchema.METRIC_COST, 0.4
                );
                break;
        }

        return new ClientDemandProfile(constraints, weights);
    }
}
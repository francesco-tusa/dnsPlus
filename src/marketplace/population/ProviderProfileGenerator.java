package marketplace.population;

import java.util.Map;
import java.util.Random;
import marketplace.common.MarketplaceMetricSchema;

/**
 * Generates stratified QoS profiles for Marketplace Providers based on tier and operational policy.
 * Models intrinsic processing capabilities (Compute), pricing models, and cold/warm start dynamics.
 * Network latency is dynamically calculated at runtime by the Routing Strategy.
 */
public class ProviderProfileGenerator {

    public static final double RADIUS_CLOUD_MAX = 8.0; // ~900km
    public static final double RADIUS_FOG_MAX   = 3.0;  // ~330km
    public static final double RADIUS_EDGE      = 0.25; // ~27km
    
    public enum ProviderPolicy { WARM_OPTIMIZED, COST_OPTIMIZED, BALANCED }

    private final Random random;

    public ProviderProfileGenerator(Random seededRandom) {
        this.random = seededRandom;
    }

    public Map<String, Double> generateProfile(String tierType, ProviderPolicy policy) {
        double computeLatency, cost, reliability, bandwidth;

        // 1. Base hardware capabilities (Compute Latency)
        double baseComputeLatency = switch (tierType.toUpperCase()) {
            case "CLOUD" -> 2.0 + (random.nextDouble() * 3.0);   // 2-5ms intrinsic
            case "FOG"   -> 10.0 + (random.nextDouble() * 10.0); // 10-20ms intrinsic
            default      -> 25.0 + (random.nextDouble() * 25.0); // EDGE: 25-50ms intrinsic
        };

        // 2. Base Bandwidth (Mbps)
        bandwidth = switch (tierType.toUpperCase()) {
            case "CLOUD" -> 1000.0;
            case "FOG"   -> 200.0 + (random.nextDouble() * 300.0);
            default      -> 50.0 + (random.nextDouble() * 50.0); // EDGE
        };

        // 3. Apply Warm/Cold Start Policy Modifiers
        switch (policy) {
            case WARM_OPTIMIZED:
                computeLatency = baseComputeLatency * 1.0; 
                cost = getBaseCost(tierType) * 2.5; // Premium for idle memory retention
                reliability = getBaseReliability(tierType) + 0.005; 
                break;
            case COST_OPTIMIZED:
                computeLatency = baseComputeLatency + 200.0; // Simulated amortized cold-start penalty
                cost = getBaseCost(tierType) * 0.5; 
                reliability = Math.max(0.90, getBaseReliability(tierType) - 0.02); // Startup failures
                break;
            case BALANCED:
            default:
                computeLatency = baseComputeLatency + 50.0; // Average penalty mix
                cost = getBaseCost(tierType) * 1.0;
                reliability = getBaseReliability(tierType);
                break;
        }

        // Bound reliability to max 1.0
        reliability = Math.min(1.0, reliability);

        return Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, computeLatency,
            MarketplaceMetricSchema.METRIC_COST, cost,
            MarketplaceMetricSchema.METRIC_RELIABILITY, reliability,
            MarketplaceMetricSchema.METRIC_BANDWIDTH, bandwidth
        );
    }

    private double getBaseCost(String tierType) {
        return switch (tierType.toUpperCase()) {
            case "CLOUD" -> 5.0;  // Economies of scale
            case "FOG"   -> 12.0;
            default      -> 25.0; // EDGE: Premium real estate, low scale
        };
    }

    private double getBaseReliability(String tierType) {
        return switch (tierType.toUpperCase()) {
            case "CLOUD" -> 0.999;
            case "FOG"   -> 0.99;
            default      -> 0.95; // EDGE: local power/network drops
        };
    }
}
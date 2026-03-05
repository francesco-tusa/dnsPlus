package marketplace.population;

import java.util.Map;
import java.util.Random;
import marketplace.common.MarketplaceMetricSchema;

/**
 * Generates stratified QoS profiles for Marketplace Providers based on tier and operational policy.
 * * IMPORTANT FaaS ECONOMIC MODEL:
 * Cost is modeled as Normalized Execution Cost (USD per 1,000,000 invocations for a 1GB container).
 * - Base Cloud Cost aligns with standard AWS Lambda on-demand pricing (~$2.50).
 * - WARM_OPTIMIZED applies a 5x premium to model AWS Provisioned Concurrency idle fees.
 * - EDGE tiers apply a 5x premium to model physical perimeter infrastructure costs (AWS Wavelength).
 */
public class ProviderProfileGenerator {

    // Realistic regional coverage radii based on geographical dispersion bounds
    public static final double RADIUS_CLOUD_MAX = 8.0;  // ~900km (e.g., us-east-1 reach)
    public static final double RADIUS_FOG_MAX   = 3.0;  // ~330km (e.g., Regional Local Zones)
    public static final double RADIUS_EDGE      = 0.25; // ~27km  (e.g., Metro 5G coverage)
    
    public enum ProviderPolicy { WARM_OPTIMIZED, COST_OPTIMIZED, BALANCED }

    private final Random random;

    public ProviderProfileGenerator(Random seededRandom) {
        this.random = seededRandom;
    }

    public Map<String, Double> generateProfile(String tierType, ProviderPolicy policy) {
        double computeLatency, cost, reliability;

        // 1. Base Hardware Compute Latency (Intrinsic container spin-up capability)
        double baseComputeLatency = switch (tierType.toUpperCase()) {
            case "CLOUD" -> 2.0 + (random.nextDouble() * 3.0);   // 2-5ms
            case "FOG"   -> 10.0 + (random.nextDouble() * 10.0); // 10-20ms
            default      -> 25.0 + (random.nextDouble() * 25.0); // EDGE: 25-50ms
        };

        // 2. Base Bandwidth (Mbps) - Driven strictly by physical tier infrastructure
        double bandwidth = switch (tierType.toUpperCase()) {
            case "CLOUD" -> 800.0 + (random.nextDouble() * 200.0); // 800-1000 Mbps Fiber
            case "FOG"   -> 200.0 + (random.nextDouble() * 300.0); // 200-500 Mbps Metro Peering
            default      -> 20.0 + (random.nextDouble() * 80.0);   // EDGE: 20-100 Mbps 5G/Wireless
        };

        // 3. Apply Warm/Cold Start Policy Modifiers (Economics & SLAs)
        switch (policy) {
            case WARM_OPTIMIZED:
                computeLatency = baseComputeLatency * 1.0;          // Zero cold-start penalty
                cost = getBaseCost(tierType) * 5.0;                 // 5x Premium for Provisioned Concurrency
                reliability = getBaseReliability(tierType) + 0.005; 
                break;
            case COST_OPTIMIZED:
                computeLatency = baseComputeLatency + 200.0;        // Simulated amortized cold-start penalty
                cost = getBaseCost(tierType) * 1.0;                 // Standard on-demand pricing
                reliability = Math.max(0.90, getBaseReliability(tierType) - 0.02); // Factor in startup failures
                break;
            case BALANCED:
            default:
                computeLatency = baseComputeLatency + 50.0;         // Average penalty mix (container reuse)
                cost = getBaseCost(tierType) * 2.0;                 // Moderate pricing
                reliability = getBaseReliability(tierType);
                break;
        }

        // Bound reliability to mathematically safe limits (Max 1.0)
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
            case "CLOUD" -> 2.50;  // Hyperscale economies of scale (AWS baseline)
            case "FOG"   -> 6.00;  // Regional datacenter premium
            default      -> 12.50; // EDGE: Premium perimeter real estate (5x Cloud baseline)
        };
    }

    private double getBaseReliability(String tierType) {
        return switch (tierType.toUpperCase()) {
            case "CLOUD" -> 0.999;
            case "FOG"   -> 0.99;
            default      -> 0.95; // EDGE: Vulnerable to local power/network drops
        };
    }
}
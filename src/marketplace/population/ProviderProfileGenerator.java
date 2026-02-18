package marketplace.population;

import java.util.Map;
import java.util.Random;
import marketplace.common.MarketplaceMetricSchema;

/**
 * Generates stratified QoS profiles for Marketplace Providers based on their tier.
 * Models intrinsic processing capabilities (Compute) and pricing models.
 * Network latency is NOT modeled here; it is dynamically calculated at runtime by the Strategy.
 */
public class ProviderProfileGenerator {

    // --- CENTRALIZED TOPOLOGY TIER RADII ---
    // Shared between placement strategy and the Skyline Area Ratio limits
    public static final double RADIUS_CLOUD_MAX = 20.0; // ~2200km
    public static final double RADIUS_FOG_MAX   = 3.0;  // ~330km
    public static final double RADIUS_EDGE      = 0.25; // ~27km
    
    private final Random random;

    public ProviderProfileGenerator(Random seededRandom) {
        this.random = seededRandom;
    }

    public Map<String, Double> generateProfile(String tierType) {
        double intrinsicLatency, cost, reliability;

        switch (tierType.toUpperCase()) {
            case "CLOUD":
                // 1. CLOUD (AWS)
                // Compute: High orchestration overhead (API Gateway/MicroVMs).
                intrinsicLatency = 20.0 + (random.nextDouble() * 10.0); // 20ms - 30ms
                // Cost: Economies of scale -> Lowest cost.
                cost = 1.0 + (random.nextDouble() * 4.0);               // $1.0 - $5.0
                // Reliability: Massive redundancy.
                reliability = 0.999 + (random.nextDouble() * 0.0009);   // 99.9% - 99.99%
                break;

            case "FOG":
                // 2. FOG (Telco/Regional)
                // Compute: Moderate orchestration (K8s/Containers).
                intrinsicLatency = 10.0 + (random.nextDouble() * 10.0); // 10ms - 20ms
                // Cost: Moderate infrastructure costs.
                cost = 10.0 + (random.nextDouble() * 10.0);             // $10.0 - $20.0
                // Reliability: Standard redundancy.
                reliability = 0.99 + (random.nextDouble() * 0.009);     // 99.0% - 99.9%
                break;

            case "EDGE":
            default:
                // 3. EDGE (Metro/Street-level)
                // Compute: Ultra-lightweight WASM isolates -> Near-zero overhead.
                intrinsicLatency = 2.0 + (random.nextDouble() * 5.0);   // 2ms - 7ms
                // Cost: Premium for physical real estate and maintenance -> Highest cost.
                cost = 40.0 + (random.nextDouble() * 20.0);             // $40.0 - $60.0
                // Reliability: Susceptible to local failures/power drops.
                reliability = 0.95 + (random.nextDouble() * 0.04);      // 95.0% - 99.0%
                break;
        }

        return Map.of(
            MarketplaceMetricSchema.METRIC_LATENCY, intrinsicLatency,
            MarketplaceMetricSchema.METRIC_COST, cost,
            MarketplaceMetricSchema.METRIC_RELIABILITY, reliability
        );
    }
}
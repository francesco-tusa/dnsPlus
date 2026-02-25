package marketplace.common;

import java.util.Map;

/**
 * Defines the standard schema for QoS metrics in the FaaS Marketplace.
 * Centralizes the definition of metric names, order, and optimization direction.
 */
public class MarketplaceMetricSchema {

    // Metric Names
    public static final String METRIC_LATENCY = "latency";
    public static final String METRIC_COST = "cost";
    public static final String METRIC_RELIABILITY = "reliability";
    public static final String METRIC_BANDWIDTH = "bandwidth";
    public static final String METRIC_ENERGY = "energy";

    // =========================================================================
    // SYSTEM MAXIMUMS (Operational SLA Limits for Hybrid Normalization)
    // =========================================================================
    
    // LATENCY (Minimize): 300.0 milliseconds
    // Justification: The absolute maximum operational delay a real-time or interactive 
    // Edge client will tolerate before considering the service degraded or dead.
    public static final double SYSTEM_MAX_LATENCY = 300.0;

    // COST (Minimize): 100.0 (Normalized FaaS Execution Tokens / Micro-dollars)
    public static final double SYSTEM_MAX_COST = 100.0;

    // RELIABILITY (Maximize): 1.0 (Probability / Nines of Availability)
    public static final double SYSTEM_MAX_RELIABILITY = 1.0;

    // BANDWIDTH (Maximize): 1000.0 (Megabits per second - Mbps)
    public static final double SYSTEM_MAX_BANDWIDTH = 1000.0;

    // ENERGY (Minimize): 100.0 (Joules per Megabyte processed)
    public static final double SYSTEM_MAX_ENERGY = 100.0;

    // 1. The Fixed Order of Dimensions in the Hypercube/Vector
    public static final String[] KEYS = {
        METRIC_LATENCY,      // Index 0
        METRIC_COST,         // Index 1
        METRIC_RELIABILITY,  // Index 2
        METRIC_BANDWIDTH,    // Index 3
        METRIC_ENERGY        // Index 4
    };

    // 2. Optimization Direction: True = Minimize, False = Maximize
    public static final Map<String, Boolean> DIRECTIONS = Map.of(
        METRIC_LATENCY,      true,  // Minimize (Lower is better)
        METRIC_COST,         true,  // Minimize
        METRIC_RELIABILITY,  false, // Maximize (Higher is better)
        METRIC_BANDWIDTH,    false, // Maximize
        METRIC_ENERGY,       true   // Minimize
    );

    // Short labels for compact logging
    public static final Map<String, String> SHORT_NAMES = Map.of(
        METRIC_LATENCY, "Lat",
        METRIC_COST, "Cst",
        METRIC_RELIABILITY, "Rel",
        METRIC_BANDWIDTH, "Bw",
        METRIC_ENERGY, "Erg"
    );

    // Quick Lookup Indices
    public static final int IDX_LATENCY = 0;
    
    // 1.665 ms of network delay per 1 degree of geographical distance
    public static final double DISTANCE_TO_TIME_FACTOR = 1.665;

    /**
     * @return The index of a specific metric key, or -1 if not found.
     */
    public static int getIndexOf(String key) {
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) return i;
        }
        return -1;
    }

    /**
     * Retrieves the predefined system maximum bound for a given metric.
     * This prevents HyperCubes from expanding to Double.MAX_VALUE (Infinity).
     */
    public static double getSystemMax(String key) {
        return switch (key) {
            case METRIC_LATENCY -> SYSTEM_MAX_LATENCY;
            case METRIC_COST -> SYSTEM_MAX_COST;
            case METRIC_RELIABILITY -> SYSTEM_MAX_RELIABILITY;
            case METRIC_BANDWIDTH -> SYSTEM_MAX_BANDWIDTH;
            case METRIC_ENERGY -> SYSTEM_MAX_ENERGY;
            default -> Double.MAX_VALUE;
        };
    }
}
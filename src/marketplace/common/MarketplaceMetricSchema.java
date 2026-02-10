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

    // Quick Lookup Indices
    public static final int IDX_LATENCY = 0;
    
    // Simulation Constant: How to convert Distance Units to Time (ms).
    // Example: 1.0 Distance Unit = 1.0 ms Latency (Simplification)
    public static final double DISTANCE_TO_TIME_FACTOR = 1.0;

    /**
     * @return The index of a specific metric key, or -1 if not found.
     */
    public static int getIndexOf(String key) {
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) return i;
        }
        return -1;
    }
}
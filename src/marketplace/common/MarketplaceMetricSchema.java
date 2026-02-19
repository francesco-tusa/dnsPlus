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
    // SYSTEM MAXIMUMS (Used for Utility Normalization when Unconstrained)
    // =========================================================================
    
    // LATENCY (Minimize): 5000.0 milliseconds (5 seconds)
    // Justification: In an extreme Edge-Cloud continuum, offloading a cold-started 
    // FaaS function to a distant, geographically remote Cloud region over a congested 
    // network typically bounds around 5 seconds. Anything beyond this is essentially a timeout.
    public static final double SYSTEM_MAX_LATENCY = 5000.0;

    // COST (Minimize): 100.0 (Normalized FaaS Execution Tokens / Micro-dollars)
    // Justification: FaaS billing (e.g., AWS Lambda) is usually priced per GB-second 
    // and invocation. Using a normalized scale of 0-100 gives a clean distribution 
    // where Cloud=5, Fog=30, and Edge=80 (paying a premium for local proximity).
    public static final double SYSTEM_MAX_COST = 100.0;

    // RELIABILITY (Maximize): 1.0 (Probability / Nines of Availability)
    // Justification: Reliability is mathematically best represented as a probability 
    // from 0.0 to 1.0 (e.g., 0.999 for "three nines"). When maximizing, 1.0 represents 
    // a theoretical 100% guarantee of execution success.
    public static final double SYSTEM_MAX_RELIABILITY = 1.0;

    // BANDWIDTH (Maximize): 1000.0 (Megabits per second - Mbps)
    // Justification: 1 Gbps (1000 Mbps) is a standard upper bound for a high-tier 
    // Cloud provider or local 5G Edge slice. A mobile client might only have 10-50 Mbps.
    public static final double SYSTEM_MAX_BANDWIDTH = 1000.0;

    // ENERGY (Minimize): 100.0 (Joules per Megabyte processed)
    // Justification: Edge devices (like battery-powered IoT) are highly sensitive to 
    // energy. 100.0 Joules represents a heavily sustained compute operation, whereas 
    // an optimized lightweight FaaS offload might consume < 1 Joule.
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
}
package marketplace.workload;

import java.util.Map;

/**
 * Encapsulates the QoS constraints and optimization weights for a client's Service Request.
 */
public record ClientDemandProfile(
    Map<String, Double> constraints, 
    Map<String, Double> weights
) {}
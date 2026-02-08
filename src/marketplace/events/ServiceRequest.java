package marketplace.events;

import java.util.Map;
import marketplace.common.MetricLocation;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;

/**
 * Represents a specific Service Request (Demand) in the Marketplace.
 * Encapsulates the Service ID, QoS preferences (Metrics), and Utility Weights.
 */
public class ServiceRequest extends PublicationWithLocation {

    private final long serviceId;
    private final Map<String, Double> preferences;
    // Weights corresponding to [Latency, Cost]
    private final double[] weights; 

    /**
     * @param serviceId Unique ID of the service being requested
     * @param preferences Map of constraints (e.g., "latency" -> 100.0)
     * @param weightsMap Map of importance weights (e.g., "latency" -> 0.8). Defaults to 1.0 if missing.
     * @param physicalLoc The physical location of the requester
     */
    public ServiceRequest(long serviceId, Map<String, Double> preferences, Map<String, Double> weightsMap, Location physicalLoc) {
        super(createMetricLocation(preferences, physicalLoc));
        this.serviceId = serviceId;
        this.preferences = preferences;
        this.weights = createWeightsArray(weightsMap);
    }

    /**
     * Copy Constructor (for propagation cloning)
     */
    private ServiceRequest(ServiceRequest other) {
        super(other.getLocation());
        this.copyStateFrom(other);
        this.serviceId = other.serviceId;
        this.preferences = other.preferences;
        this.weights = other.weights;
    }

    private static MetricLocation createMetricLocation(Map<String, Double> preferences, Location physicalLoc) {
        // Extract targets (e.g., Latency < 20)
        double targetLatency = preferences.getOrDefault("latency", Double.MAX_VALUE);
        double targetCost = preferences.getOrDefault("cost", Double.MAX_VALUE);

        // Dimensions: [Latency, Cost]
        double[] reqValues = new double[] { targetLatency, targetCost };

        return new MetricLocation(reqValues, physicalLoc);
    }

    private static double[] createWeightsArray(Map<String, Double> weightsMap) {
        // Default weight is 1.0 if not specified
        double wLatency = (weightsMap != null) ? weightsMap.getOrDefault("latency", 1.0) : 1.0;
        double wCost = (weightsMap != null) ? weightsMap.getOrDefault("cost", 1.0) : 1.0;

        return new double[] { wLatency, wCost };
    }

    @Override
    public SimulationPublication getPublication() {
        // Return specific type to preserve data during propagation
        return new ServiceRequest(this);
    }

    public long getServiceId() {
        return serviceId;
    }

    public Map<String, Double> getPreferences() {
        return preferences;
    }

    public double[] getWeights() {
        return weights;
    }

    @Override
    public String toString() {
        return "ServiceRequest[ID=" + serviceId + ", Prefs=" + preferences + "]";
    }
}
package marketplace.events;

import java.util.Map;
import marketplace.common.MetricLocation;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;

/**
 * Represents a specific Service Request (Demand) in the Marketplace.
 * Encapsulates the Service ID and the QoS preferences (Metrics).
 */
public class ServiceRequest extends PublicationWithLocation {

    private final long serviceId;
    private final Map<String, Double> preferences;

    public ServiceRequest(long serviceId, Map<String, Double> preferences, Location physicalLoc) {
        super(createMetricLocation(preferences, physicalLoc));
        this.serviceId = serviceId;
        this.preferences = preferences;
    }

    /**
     * Copy Constructor (for propagation cloning)
     */
    private ServiceRequest(ServiceRequest other) {
        super(other.getLocation());
        this.copyStateFrom(other);
        this.serviceId = other.serviceId;
        this.preferences = other.preferences;
    }

    private static MetricLocation createMetricLocation(Map<String, Double> preferences, Location physicalLoc) {
        // Extract targets (e.g., Latency < 20)
        double targetLatency = preferences.getOrDefault("latency", Double.MAX_VALUE);
        double targetCost = preferences.getOrDefault("cost", Double.MAX_VALUE);

        // Dimensions: [Latency, Cost]
        double[] reqValues = new double[] { targetLatency, targetCost };

        return new MetricLocation(reqValues, physicalLoc);
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

    @Override
    public String toString() {
        return "ServiceRequest[ID=" + serviceId + ", Prefs=" + preferences + "]";
    }
}
package marketplace.events;

import java.util.Map;
import marketplace.common.MetricHyperCube;
import simulator.core.Location;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

/**
 * Represents a specific Service Offer in the Marketplace.
 * It is a specialized Subscription that carries QoS Metrics (Latency, Cost)
 * encoded within a MetricHyperCube.
 */
public class ServiceOffer extends SubscriptionWithRegion {

    private final long serviceId;
    private final Map<String, Double> qosMetrics;

    public ServiceOffer(long serviceId, Map<String, Double> metrics, Location location, String providerName) {
        super(createMetricRegion(metrics, location, providerName));
        this.serviceId = serviceId;
        this.qosMetrics = metrics;
    }

    /**
     * Copy constructor for propagation.
     * Ensures deep copy of the MetricHyperCube.
     */
    public ServiceOffer(ServiceOffer other) {
        super(other);
        this.serviceId = other.serviceId;
        this.qosMetrics = other.qosMetrics;

        // Deep copy the region to preserve MetricHyperCube logic
        if (other.getRegion() instanceof MetricHyperCube mhc) {
            this.setRegion(new MetricHyperCube(mhc));
        }
    }

    private static Region createMetricRegion(Map<String, Double> metrics, Location loc, String providerName) {
        // 1. Extract Metrics
        double latency = metrics.getOrDefault("latency", 0.0);
        double cost = metrics.getOrDefault("cost", 0.0);
        double[] values = new double[] { latency, cost };
        boolean[] flags = new boolean[] { true, true }; // Minimize both

        // 2. Determine Physical Scope
        simulator.regions.Region physicalScope;
        if (providerName.contains("Cloud")) {
            // Global Scope
            physicalScope = new simulator.regions.Region(-100, -100, 1100, 1100);
        } else {
            // Local Scope (e.g., +/- 150 units)
            double halfSize = 150.0;
            physicalScope = new simulator.regions.Region(
                    loc.getX() - halfSize, loc.getY() - halfSize,
                    loc.getX() + halfSize, loc.getY() + halfSize);
        }

        // 3. Return HyperCube
        return new MetricHyperCube(values, values, flags, physicalScope);
    }

    public long getServiceId() {
        return serviceId;
    }

    public Map<String, Double> getQosMetrics() {
        return qosMetrics;
    }

    @Override
    public String toString() {
        return "ServiceOffer[ID=" + serviceId + ", Metrics=" + qosMetrics + "]";
    }
}
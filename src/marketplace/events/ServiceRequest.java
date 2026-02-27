package marketplace.events;

import java.util.Map;
import marketplace.common.MarketplaceMetricSchema;
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
    private final double[] weights; 
    private final boolean[] minimizeFlags;

    public ServiceRequest(long serviceId, Map<String, Double> constraints, Map<String, Double> weightsMap, Location physicalLoc) {
        // We wrap the constraints into a MetricLocation and pass it to super
        super(createMetricLocation(constraints, physicalLoc));
        this.serviceId = serviceId;
        this.preferences = constraints;
        this.weights = createWeightsArray(weightsMap, constraints);
        this.minimizeFlags = createOptimizationFlags();
    }

    private ServiceRequest(ServiceRequest other) {
        super(other.getLocation());
        this.copyStateFrom(other);
        this.serviceId = other.serviceId;
        this.preferences = other.preferences;
        this.weights = other.weights;
        this.minimizeFlags = other.minimizeFlags;
    }

    public MetricLocation getQoSConstraintsLocation() {
        return (MetricLocation) this.getLocation();
    }

    private static MetricLocation createMetricLocation(Map<String, Double> constraints, Location physicalLoc) {
        double[] reqValues = new double[MarketplaceMetricSchema.KEYS.length];
        for (int i = 0; i < MarketplaceMetricSchema.KEYS.length; i++) {
            String key = MarketplaceMetricSchema.KEYS[i];
            boolean isMinimization = MarketplaceMetricSchema.DIRECTIONS.get(key);
            double defaultVal = isMinimization ? Double.MAX_VALUE : 0.0;
            reqValues[i] = constraints.getOrDefault(key, defaultVal);
        }
        return new MetricLocation(reqValues, physicalLoc);
    }

    // Dynamically ignore metrics that the client does not care about.
    private static double[] createWeightsArray(Map<String, Double> weightsMap, Map<String, Double> constraints) {
        double[] w = new double[MarketplaceMetricSchema.KEYS.length];
        for (int i = 0; i < MarketplaceMetricSchema.KEYS.length; i++) {
            String key = MarketplaceMetricSchema.KEYS[i];
            if (weightsMap != null && weightsMap.containsKey(key)) {
                w[i] = weightsMap.get(key);
            } else if (constraints != null && constraints.containsKey(key)) {
                // Apply a balanced weight ONLY to dimensions the client explicitly requests
                w[i] = 1.0; 
            } else {
                // Ignore missing dimensions entirely to prevent utility math inflation
                w[i] = 0.0; 
            }
        }
        return w;
    }

    private static boolean[] createOptimizationFlags() {
        boolean[] flags = new boolean[MarketplaceMetricSchema.KEYS.length];
        for (int i = 0; i < MarketplaceMetricSchema.KEYS.length; i++) {
            flags[i] = MarketplaceMetricSchema.DIRECTIONS.get(MarketplaceMetricSchema.KEYS[i]);
        }
        return flags;
    }

    @Override
    public SimulationPublication getPublication() {
        return new ServiceRequest(this);
    }

    public long getServiceId() { return serviceId; }
    public Map<String, Double> getPreferences() { return preferences; }
    public double[] getWeights() { return weights; }
    public boolean[] getMinimizeFlags() { return minimizeFlags; }

    @Override
    public String toString() {
        return "ServiceRequest[ID=" + serviceId + "]";
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < MarketplaceMetricSchema.KEYS.length; i++) {
            String key = MarketplaceMetricSchema.KEYS[i];
            if (preferences.containsKey(key)) {
                if (!first) sb.append("|");
                sb.append(MarketplaceMetricSchema.SHORT_NAMES.get(key))
                  .append(":").append(String.format("%.2f", preferences.get(key)))
                  .append("(w:").append(String.format("%.2f", weights[i])).append(")");
                first = false;
            }
        }
        return sb.toString();
    }
}
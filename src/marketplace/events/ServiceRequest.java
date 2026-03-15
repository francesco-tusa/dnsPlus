package marketplace.events;

import java.util.Map;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.common.MetricLocation;
import marketplace.common.identifiers.ServiceIdentifier;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;

public class ServiceRequest extends PublicationWithLocation {

    private final long originalRequestId; // Persistent reference to track the request across broker hops
    
    private final long oracleServiceId; // SIMULATION ARTIFACT: For GroundTruthCalculator only
    private final ServiceIdentifier routingIdentifier; // ROUTING PAYLOAD: For Broker/Directory routing
    
    private final Map<String, Double> preferences;
    private final double[] weights; 
    private final boolean[] minimizeFlags;

    public ServiceRequest(long oracleServiceId, ServiceIdentifier routingIdentifier, Map<String, Double> constraints, Map<String, Double> weightsMap, Location physicalLoc) {
        super(createMetricLocation(constraints, physicalLoc));
        this.originalRequestId = this.getId(); // Capture the root ID
        this.oracleServiceId = oracleServiceId;
        this.routingIdentifier = routingIdentifier;
        this.preferences = constraints;
        this.weights = createWeightsArray(weightsMap, constraints);
        this.minimizeFlags = createOptimizationFlags();
    }

    private ServiceRequest(ServiceRequest other) {
        super(other.getLocation());
        this.copyStateFrom(other);
        this.originalRequestId = other.originalRequestId; // Preserve the root ID during broker cloning
        this.oracleServiceId = other.oracleServiceId;
        this.routingIdentifier = other.routingIdentifier;
        this.preferences = other.preferences;
        this.weights = other.weights;
        this.minimizeFlags = other.minimizeFlags;
    }

    public long getOriginalRequestId() {
        return originalRequestId;
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

    public long getOracleServiceId() { return oracleServiceId; }
    public ServiceIdentifier getIdentifier() { return routingIdentifier; }
    
    public Map<String, Double> getPreferences() { return preferences; }
    public double[] getWeights() { return weights; }
    public boolean[] getMinimizeFlags() { return minimizeFlags; }

    @Override
    public String toString() {
        return "ServiceRequest[OracleID=" + oracleServiceId + "]";
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
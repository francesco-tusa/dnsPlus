package marketplace.events;

import java.util.Map;
import marketplace.common.MetricHyperCube;
import marketplace.common.MarketplaceMetricSchema; // Import Schema
import simulator.core.Location;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

public class ServiceOffer extends SubscriptionWithRegion {

    private final long serviceId;
    private final Map<String, Double> qosMetrics;

    // REMOVED: private static final String[] METRIC_KEYS = { "latency", "cost" };

    public ServiceOffer(long serviceId, Map<String, Double> metrics, Location location, String providerName) {
        super(createMetricRegion(metrics, location, providerName));
        this.serviceId = serviceId;
        this.qosMetrics = metrics;
    }

    public ServiceOffer(ServiceOffer other) {
        super(other);
        this.serviceId = other.serviceId;
        this.qosMetrics = other.qosMetrics;
        if (other.getRegion() instanceof MetricHyperCube mhc) {
            this.setRegion(new MetricHyperCube(mhc));
        }
    }
    
    // Internal constructor for reconstruction
    private ServiceOffer(long serviceId, Map<String, Double> metrics, Region region) {
        super(region);
        this.serviceId = serviceId;
        this.qosMetrics = metrics;
    }

    public static Map<String, Double> reconstructMetrics(MetricHyperCube cube) {
        // FIX: Use the global Schema Keys
        return cube.getMetricsMap(MarketplaceMetricSchema.KEYS);
    }
    
    public static ServiceOffer createAggregated(long serviceId, MetricHyperCube aggregatedCube) {
        Map<String, Double> derivedMetrics = reconstructMetrics(aggregatedCube);
        return new ServiceOffer(serviceId, derivedMetrics, aggregatedCube);
    }

    /**
     * UNIFIED REGION LOGIC:
     * 1. Uses MarketplaceMetricSchema to determine dimensions.
     * 2. Sets "Interest Intervals" based on optimization direction.
     */
    private static Region createMetricRegion(Map<String, Double> metrics, Location loc, String providerName) {
        
        // --- 1. Metric Dimension Setup ---
        int dim = MarketplaceMetricSchema.KEYS.length;
        double[] minValues = new double[dim];
        double[] maxValues = new double[dim];
        boolean[] flags = new boolean[dim]; 

        for (int i = 0; i < dim; i++) {
            String key = MarketplaceMetricSchema.KEYS[i];
            boolean minimize = MarketplaceMetricSchema.DIRECTIONS.get(key);
            double value = metrics.getOrDefault(key, minimize ? 0.0 : Double.MAX_VALUE);
            
            flags[i] = minimize; 

            // Logic:
            // Minimize (e.g. Latency 20ms): We satisfy requests for >= 20ms. Range: [20, MAX]
            // Maximize (e.g. Reliability 99%): We satisfy requests for <= 99%. Range: [0, 99]
            if (minimize) {
                minValues[i] = value;
                maxValues[i] = Double.MAX_VALUE;
            } else {
                minValues[i] = 0.0;
                maxValues[i] = value;
            }
        }

        // --- 2. Physical Scope Setup ---
        String name = providerName.toLowerCase();
        simulator.regions.Region physicalScope;

        if (name.contains("cloud") || name.contains("global")) {
            double r = 60.0;
            physicalScope = new simulator.regions.Region(
                    loc.getX() - r, loc.getY() - r, loc.getX() + r, loc.getY() + r);
        } 
        else if (name.contains("fog") || name.contains("region")) {
            double r = 20.0;
            physicalScope = new simulator.regions.Region(
                    loc.getX() - r, loc.getY() - r, loc.getX() + r, loc.getY() + r);
        } 
        else {
            double r = 5.0;
            physicalScope = new simulator.regions.Region(
                    loc.getX() - r, loc.getY() - r, loc.getX() + r, loc.getY() + r);
        }

        return new MetricHyperCube(minValues, maxValues, flags, physicalScope);
    }

    public long getServiceId() { return serviceId; }
    public Map<String, Double> getQosMetrics() { return qosMetrics; }

    @Override
    public String toString() {
        return "ServiceOffer[ID=" + serviceId + ", QoS=" + qosMetrics + "]";
    }
    
    @Override
    public SimulationSubscription getSubscription() {
        return new ServiceOffer(this);
    }
}
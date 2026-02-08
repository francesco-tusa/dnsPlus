package marketplace.events;

import java.util.Map;
import marketplace.common.MetricHyperCube;
import simulator.core.Location;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

public class ServiceOffer extends SubscriptionWithRegion {

    private final long serviceId;
    private final Map<String, Double> qosMetrics;
    private static final String[] METRIC_KEYS = { "latency", "cost" };

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
        return cube.getMetricsMap(METRIC_KEYS);
    }
    
    public static ServiceOffer createAggregated(long serviceId, MetricHyperCube aggregatedCube) {
        Map<String, Double> derivedMetrics = reconstructMetrics(aggregatedCube);
        return new ServiceOffer(serviceId, derivedMetrics, aggregatedCube);
    }

    /**
     * UNIFIED REGION LOGIC:
     * Assigns spatial scope based on Provider Type.
     * * hierarchy:
     * - Cloud/Global: Country/AWS Region size (e.g. +/- 60.0). Not entire world.
     * - Fog/Regional: Large Radius (e.g. +/- 20.0). Covers multiple cities.
     * - Edge/Local:   Small Radius (e.g. +/- 5.0). City/District level.
     */
    private static Region createMetricRegion(Map<String, Double> metrics, Location loc, String providerName) {
        double latency = metrics.getOrDefault("latency", 0.0);
        double cost = metrics.getOrDefault("cost", 0.0);
        
        double[] values = new double[] { latency, cost };
        boolean[] flags = new boolean[] { true, true }; // Minimize both

        String name = providerName.toLowerCase();
        simulator.regions.Region physicalScope;

        if (name.contains("cloud") || name.contains("global")) {
            // Cloud Scope: A large region (e.g., Country or AWS Region)
            // Radius 60.0 ensures it covers most of the test map (0-60) 
            // but is bounded (e.g., doesn't cover the antipodes).
            double r = 60.0;
            physicalScope = new simulator.regions.Region(
                    loc.getX() - r, loc.getY() - r, loc.getX() + r, loc.getY() + r);
        } 
        else if (name.contains("fog") || name.contains("region")) {
            // Regional Scope: Covers neighboring brokers (e.g., +/- 20 units)
            double r = 20.0;
            physicalScope = new simulator.regions.Region(
                    loc.getX() - r, loc.getY() - r, loc.getX() + r, loc.getY() + r);
        } 
        else {
            // Edge/City Scope: Strictly local (e.g., +/- 5 units)
            double r = 5.0;
            physicalScope = new simulator.regions.Region(
                    loc.getX() - r, loc.getY() - r, loc.getX() + r, loc.getY() + r);
        }

        return new MetricHyperCube(values, values, flags, physicalScope);
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
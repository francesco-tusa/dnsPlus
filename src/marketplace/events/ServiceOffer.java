package marketplace.events;

import java.util.Map;
import marketplace.common.MetricHyperCube;
import marketplace.common.MarketplaceMetricSchema; 
import simulator.core.Location;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

public class ServiceOffer extends SubscriptionWithRegion {

    private final long serviceId;
    private final Map<String, Double> qosMetrics;
    private final Location providerLocation; 
    private final String providerName; 
    
    // The explicit, topology-defined physical limit of this offer
    private final double coverageRadius; 

    // 1. Primary Constructor: Strictly requires the coverageRadius
    public ServiceOffer(long serviceId, Map<String, Double> metrics, Location location, String providerName, double coverageRadius) {
        super(createMetricRegion(metrics, location, coverageRadius));
        this.serviceId = serviceId;
        this.qosMetrics = metrics;
        this.providerLocation = location;
        this.providerName = providerName;
        this.coverageRadius = coverageRadius; 
    }

    // 2. Legacy/Fallback Constructor: Defaults to a 0.0 radius (Exact Point Match) if not provided
    public ServiceOffer(long serviceId, Map<String, Double> metrics, Location location, String providerName) {
        this(serviceId, metrics, location, providerName, 0.0);
    }

    // 3. Copy Constructor
    public ServiceOffer(ServiceOffer other) {
        super(other);
        this.serviceId = other.serviceId;
        this.qosMetrics = other.qosMetrics;
        this.providerLocation = other.providerLocation;
        this.providerName = other.providerName; 
        this.coverageRadius = other.coverageRadius; 
        if (other.getRegion() instanceof MetricHyperCube mhc) {
            this.setRegion(new MetricHyperCube(mhc));
        }
    }

    // 4. Internal Expansion/Aggregation Constructor
    protected ServiceOffer(long serviceId, Map<String, Double> metrics, Region region, Location location, String providerName, double coverageRadius) {
        super(region);
        this.serviceId = serviceId;
        this.qosMetrics = metrics;
        this.providerLocation = location;
        this.providerName = providerName;
        this.coverageRadius = coverageRadius;
    }

    public static ServiceOffer createWithUpdatedRegion(ServiceOffer original, MetricHyperCube newRegion) {
        return new ServiceOffer(
            original.getServiceId(), 
            original.getQosMetrics(), 
            newRegion, 
            original.getLocation(), 
            original.getProviderName(),
            original.getCoverageRadius() // Preserve the original physical limit
        );
    }

    public static Map<String, Double> reconstructMetrics(MetricHyperCube cube) {
        return cube.getMetricsMap(MarketplaceMetricSchema.KEYS);
    }
    
    public static ServiceOffer createAggregated(long serviceId, MetricHyperCube aggregatedCube) {
        Map<String, Double> derivedMetrics = reconstructMetrics(aggregatedCube);
        // Aggregated clusters represent entire tree branches, so they inherently have
        // infinite fallback radius to prevent spatial clipping during upward routing.
        return new ServiceOffer(serviceId, derivedMetrics, aggregatedCube, null, "Aggregated-Cluster",
                Double.MAX_VALUE);
    }

    public static ServiceOffer createSkylineAggregated(long serviceId, Map<String, Double> metrics,
            Region skylineRegion) {
        // Aggregated clusters represent entire tree branches, infinite fallback radius
        // to prevent spatial clipping
        return new ServiceOffer(serviceId, metrics, skylineRegion, null, "Aggregated-Skyline", Double.MAX_VALUE);
    }

    // Purely mathematical generation based on the explicitly provided radius
    private static Region createMetricRegion(Map<String, Double> metrics, Location loc, double coverageRadius) {        
        int dim = MarketplaceMetricSchema.KEYS.length;
        double[] minValues = new double[dim];
        double[] maxValues = new double[dim];

        for (int i = 0; i < dim; i++) {
            String key = MarketplaceMetricSchema.KEYS[i];
            boolean minimize = MarketplaceMetricSchema.DIRECTIONS.get(key);
            
            // Get the realistic system maximum instead of Infinity
            double systemMax = MarketplaceMetricSchema.getSystemMax(key);
            
            double value = metrics.getOrDefault(key, minimize ? 0.0 : systemMax);
            
            if (minimize) {
                // Interval: [ProviderValue, RealisticSystemMax]
                minValues[i] = value;
                maxValues[i] = systemMax; 
            } else {
                // Interval: [0.0, ProviderValue]
                minValues[i] = 0.0;
                maxValues[i] = value;
            }
        }

        // Apply the strictly provided radius
        simulator.regions.Region physicalScope = new simulator.regions.Region(
                loc.getX() - coverageRadius, loc.getY() - coverageRadius, 
                loc.getX() + coverageRadius, loc.getY() + coverageRadius);

        boolean[] flags = new boolean[dim];
        for(int i=0; i<dim; i++) flags[i] = MarketplaceMetricSchema.DIRECTIONS.get(MarketplaceMetricSchema.KEYS[i]);

        return new MetricHyperCube(minValues, maxValues, flags, physicalScope);
    }

    public long getServiceId() { return serviceId; }
    public Map<String, Double> getQosMetrics() { return qosMetrics; }
    public Location getLocation() { return providerLocation; }
    public String getProviderName() { return providerName; }
    public double getCoverageRadius() { return coverageRadius; } 

    @Override
    public String toString() {
        return "ServiceOffer[ID=" + serviceId + ", Provider=" + providerName + "]";
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        
        String radStr = (coverageRadius > 1e9) ? "INF" : String.format("%.2f", coverageRadius);
        sb.append("Rad:").append(radStr);
        
        for (String key : MarketplaceMetricSchema.KEYS) {
            if (qosMetrics.containsKey(key)) {
                double val = qosMetrics.get(key);
                String valStr = (val > 1e9) ? "INF" : String.format("%.2f", val);
                sb.append("|").append(MarketplaceMetricSchema.SHORT_NAMES.get(key)).append(":").append(valStr);
            }
        }
        return sb.toString();
    }
    
    @Override
    public SimulationSubscription getSubscription() {
        return new ServiceOffer(this);
    }
}
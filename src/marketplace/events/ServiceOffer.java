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
    
    // Identity field for Ground Truth attribution
    private final String providerName; 

    public ServiceOffer(long serviceId, Map<String, Double> metrics, Location location, String providerName) {
        super(createMetricRegion(metrics, location, providerName, 0));
        this.serviceId = serviceId;
        this.qosMetrics = metrics;
        this.providerLocation = location;
        this.providerName = providerName;
    }

    public ServiceOffer(long serviceId, Map<String, Double> metrics, Location location, String providerName, double explicitRadius) {
        super(createMetricRegion(metrics, location, providerName, explicitRadius));
        this.serviceId = serviceId;
        this.qosMetrics = metrics;
        this.providerLocation = location;
        this.providerName = providerName;
    }

    // Copy Constructor
    public ServiceOffer(ServiceOffer other) {
        super(other);
        this.serviceId = other.serviceId;
        this.qosMetrics = other.qosMetrics;
        this.providerLocation = other.providerLocation;
        this.providerName = other.providerName; // Copy identity
        if (other.getRegion() instanceof MetricHyperCube mhc) {
            this.setRegion(new MetricHyperCube(mhc));
        }
    }

    // Constructor with explicit Provider Name and Region
    protected ServiceOffer(long serviceId, Map<String, Double> metrics, Region region, Location location, String providerName) {
        super(region);
        this.serviceId = serviceId;
        this.qosMetrics = metrics;
        this.providerLocation = location;
        this.providerName = providerName;
    }

    /**
     * Creates a copy of the original offer but replaces its Region with the new expanded HyperCube.
     * Crucially, this PRESERVES the exact provider location and identity.
     */
    public static ServiceOffer createWithUpdatedRegion(ServiceOffer original, MetricHyperCube newRegion) {
        return new ServiceOffer(
            original.getServiceId(), 
            original.getQosMetrics(), 
            newRegion, 
            original.getLocation(), 
            original.getProviderName()
        );
    }

    // Aggregation Constructor (Provider Name is "Aggregated-Cluster")
    private ServiceOffer(long serviceId, Map<String, Double> metrics, Region region, Location location) {
        this(serviceId, metrics, region, location, "Aggregated-Cluster");
    }


    public static Map<String, Double> reconstructMetrics(MetricHyperCube cube) {
        return cube.getMetricsMap(MarketplaceMetricSchema.KEYS);
    }
    
    public static ServiceOffer createAggregated(long serviceId, MetricHyperCube aggregatedCube) {
        Map<String, Double> derivedMetrics = reconstructMetrics(aggregatedCube);
        return new ServiceOffer(serviceId, derivedMetrics, aggregatedCube, null);
    }

    private static Region createMetricRegion(Map<String, Double> metrics, Location loc, String providerName, double explicitRadius) {        
        int dim = MarketplaceMetricSchema.KEYS.length;
        double[] minValues = new double[dim];
        double[] maxValues = new double[dim];

        for (int i = 0; i < dim; i++) {
            String key = MarketplaceMetricSchema.KEYS[i];
            boolean minimize = MarketplaceMetricSchema.DIRECTIONS.get(key);
            double value = metrics.getOrDefault(key, minimize ? 0.0 : Double.MAX_VALUE);
            
            if (minimize) {
                minValues[i] = value;
                maxValues[i] = Double.MAX_VALUE;
            } else {
                minValues[i] = 0.0;
                maxValues[i] = value;
            }
        }

        simulator.regions.Region physicalScope;
        double r;

        if (explicitRadius > 0) {
            r = explicitRadius;
        } else {
            String name = (providerName != null) ? providerName.toLowerCase() : "unknown";
            if (name.contains("cloud")) r = 60.0;
            else if (name.contains("fog")) r = 20.0;
            else r = 5.0;
        }

        physicalScope = new simulator.regions.Region(
                loc.getX() - r, loc.getY() - r, loc.getX() + r, loc.getY() + r);

        // Pass boolean flags for optimization direction
        boolean[] flags = new boolean[dim];
        for(int i=0; i<dim; i++) flags[i] = MarketplaceMetricSchema.DIRECTIONS.get(MarketplaceMetricSchema.KEYS[i]);

        return new MetricHyperCube(minValues, maxValues, flags, physicalScope);
    }

    public long getServiceId() { return serviceId; }
    public Map<String, Double> getQosMetrics() { return qosMetrics; }
    public Location getLocation() { return providerLocation; }
    public String getProviderName() { return providerName; }

    @Override
    public String toString() {
        return "ServiceOffer[ID=" + serviceId + ", Provider=" + providerName + "]";
    }
    
    @Override
    public SimulationSubscription getSubscription() {
        return new ServiceOffer(this);
    }
}
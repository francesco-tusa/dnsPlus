package marketplace.common;

import simulator.regions.Region;
import simulator.regions.SpatialRegion;
import simulator.core.Location;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import marketplace.common.aggregation.AggregationStrategy;
import marketplace.config.MarketplaceConfig;

public class MetricHyperCube extends Region {

    // STATE 1: Homomorphic Routing Envelope (Smeared to System Limits)
    private double[] routingMinValues;
    private double[] routingMaxValues;
    
    // STATE 2: Strict Capability Envelope (Used for Tri-State FPR)
    private double[] capabilityMinValues;
    private double[] capabilityMaxValues;
    
    // STATE 3: Utility Expected Yield
    private double[] qosCenterOfMass;

    private boolean[] minimizeFlags;
    
    // Density Tracking
    private int providerWeight; 
    private double densityCenterX;
    private double densityCenterY;

    /**
     * Primary Constructor for Base Providers (Leaf Nodes).
     */
    public MetricHyperCube(double[] rMin, double[] rMax, double[] cMin, double[] cMax, double[] rawCapabilities, boolean[] minimizeFlags, SpatialRegion physicalScope, Location anchorLocation) {
        super(physicalScope);
        this.routingMinValues = Arrays.copyOf(rMin, rMin.length);
        this.routingMaxValues = Arrays.copyOf(rMax, rMax.length);
        
        // ACCEPT the calculated SLA Flexibility volume
        this.capabilityMinValues = Arrays.copyOf(cMin, cMin.length);
        this.capabilityMaxValues = Arrays.copyOf(cMax, cMax.length);
        this.minimizeFlags = Arrays.copyOf(minimizeFlags, minimizeFlags.length);
        
        this.qosCenterOfMass = Arrays.copyOf(rawCapabilities, rawCapabilities.length); 
        
        this.providerWeight = 1;
        if (anchorLocation != null) {
            this.densityCenterX = anchorLocation.getX();
            this.densityCenterY = anchorLocation.getY();
        } else {
            Location center = physicalScope.getCenter();
            this.densityCenterX = center.getX();
            this.densityCenterY = center.getY();
        }
    }

    /**
     * Copy Constructor.
     */
    public MetricHyperCube(MetricHyperCube other) {
        super(other);
        this.routingMinValues = Arrays.copyOf(other.routingMinValues, other.routingMinValues.length);
        this.routingMaxValues = Arrays.copyOf(other.routingMaxValues, other.routingMaxValues.length);
        this.capabilityMinValues = Arrays.copyOf(other.capabilityMinValues, other.capabilityMinValues.length);
        this.capabilityMaxValues = Arrays.copyOf(other.capabilityMaxValues, other.capabilityMaxValues.length);
        this.minimizeFlags = Arrays.copyOf(other.minimizeFlags, other.minimizeFlags.length);
        this.qosCenterOfMass = Arrays.copyOf(other.qosCenterOfMass, other.qosCenterOfMass.length);
        
        this.providerWeight = other.providerWeight;
        this.densityCenterX = other.densityCenterX;
        this.densityCenterY = other.densityCenterY;
    }

    /**
     * INTERNAL AGGREGATION CONSTRUCTOR for Topological Upward Propagation.
     */
    public MetricHyperCube(double[] rMin, double[] rMax, double[] cMin, double[] cMax, boolean[] flags, SpatialRegion phys, int weight, double cx, double cy, double[] qos) {
        super(phys);
        this.routingMinValues = Arrays.copyOf(rMin, rMin.length);
        this.routingMaxValues = Arrays.copyOf(rMax, rMax.length);
        this.capabilityMinValues = Arrays.copyOf(cMin, cMin.length);
        this.capabilityMaxValues = Arrays.copyOf(cMax, cMax.length);
        this.minimizeFlags = Arrays.copyOf(flags, flags.length);
        this.qosCenterOfMass = Arrays.copyOf(qos, qos.length);
        this.providerWeight = weight;
        this.densityCenterX = cx;
        this.densityCenterY = cy;
    }

    @Override
    public Region copy() { return new MetricHyperCube(this); }

    // =========================================================
    // --- TRI-STATE GETTERS ---
    // =========================================================
    
    public double[] getRoutingMinValues() { return routingMinValues; }
    public double[] getRoutingMaxValues() { return routingMaxValues; }
    public double[] getCapabilityMinValues() { return capabilityMinValues; }
    public double[] getCapabilityMaxValues() { return capabilityMaxValues; }
    public double[] getQosCenterOfMass() { return qosCenterOfMass; }
    public int getProviderWeight() { return providerWeight; }
    public boolean[] getMinimizeFlags() { return minimizeFlags; }
    public double getDensityCenterX() { return densityCenterX; }
    public double getDensityCenterY() { return densityCenterY; }

    /**
     * Encapsulates the Density Barycenter into a Location object.
     * Heavily utilized by WeightedUtilityStrategy and Broker logic.
     */
    public Location getDensityCentroid() {
        return new Location(densityCenterX, densityCenterY, 0.0);
    }

    /**
     * LEGACY ALIASES: Guarantees backward compatibility with standard 
     * DNS++ Substrate logic that expects getMinValues/getMaxValues.
     * We return the Smeared Routing Bounds to ensure R-Tree intersection succeeds.
     */
    public double[] getMinValues() { return routingMinValues; }
    public double[] getMaxValues() { return routingMaxValues; }

    public Map<String, Double> getMetricsMap(String[] dimensionNames) {
        Map<String, Double> map = new HashMap<>();
        for (int i = 0; i < qosCenterOfMass.length; i++) {
            map.put(dimensionNames[i], qosCenterOfMass[i]); 
        }
        return map;
    }

    // --- SPATIAL AND LOGICAL EVALUATION ---

    @Override
    public boolean contains(simulator.regions.SpatialRegion r) {
        // 1. Must be spatially contained (Inherited from Region's 2D GPS check)
        if (!super.contains(r)) return false;

        // 2. Must be dimensionally dominated in the N-Dimensional QoS Space
        if (r instanceof MetricHyperCube other) {
            for (int i = 0; i < this.routingMinValues.length; i++) {
                if (this.minimizeFlags[i]) {
                    // For metrics like Latency or Cost, we want LOWER values.
                    // If the new offer has a BETTER (lower) minimum than what our hypercube 
                    // currently provides, it expands our capabilities. It is NOT contained.
                    if (other.routingMinValues[i] < this.routingMinValues[i]) {
                        return false; 
                    }
                } else {
                    // For metrics like Reliability or CPU shares, we want HIGHER values.
                    // If the new offer has a BETTER (higher) maximum than what our hypercube 
                    // currently provides, it expands our capabilities. It is NOT contained.
                    if (other.routingMaxValues[i] > this.routingMaxValues[i]) {
                        return false;
                    }
                }
            }
        }
        
        // If we reach here, the new offer provides absolutely no multi-objective benefit 
        // over our existing aggregated state. It is Pareto-dominated and contained.
        return true;
    }

    // --- TRI-STATE AGGREGATION LOGIC ---

    @Override
    public boolean expand(SpatialRegion r) {
        boolean physicalChanged = super.expand(r);
        if (!(r instanceof MetricHyperCube other)) return physicalChanged;

        AggregationStrategy strategy = MarketplaceConfig.get().activeAggregationStrategy;

        // 1. Delegate Spatial Density Calculation
        this.densityCenterX = strategy.calculateAggregatedValue(
                this.densityCenterX, this.providerWeight,
                other.densityCenterX, other.providerWeight);

        this.densityCenterY = strategy.calculateAggregatedValue(
                this.densityCenterY, this.providerWeight,
                other.densityCenterY, other.providerWeight);

        boolean metricChanged = false;
    
        for (int i = 0; i < routingMinValues.length; i++) {
            // 2. Delegate QoS Expected Yield (Center of Mass) Calculation
            double newQos = strategy.calculateAggregatedValue(
                    this.qosCenterOfMass[i], this.providerWeight,
                    other.qosCenterOfMass[i], other.providerWeight);

            if (Math.abs(newQos - this.qosCenterOfMass[i]) > 1e-9) {
                this.qosCenterOfMass[i] = newQos;
                metricChanged = true;
            }

            // 2. UPDATE CAPABILITY ENVELOPE (For FPR Thresholding)
            double newCapMin = Math.min(this.capabilityMinValues[i], other.capabilityMinValues[i]);
            double newCapMax = Math.max(this.capabilityMaxValues[i], other.capabilityMaxValues[i]);
            if (Math.abs(newCapMin - this.capabilityMinValues[i]) > 1e-9 || Math.abs(newCapMax - this.capabilityMaxValues[i]) > 1e-9) {
                this.capabilityMinValues[i] = newCapMin;
                this.capabilityMaxValues[i] = newCapMax;
                metricChanged = true;
            }

            // 3. UPDATE ROUTING ENVELOPE (For Blind Matching Intersection)
            double newRMin = Math.min(this.routingMinValues[i], other.routingMinValues[i]);
            double newRMax = Math.max(this.routingMaxValues[i], other.routingMaxValues[i]);
            if (Math.abs(newRMin - this.routingMinValues[i]) > 1e-9 || Math.abs(newRMax - this.routingMaxValues[i]) > 1e-9) {
                this.routingMinValues[i] = newRMin;
                this.routingMaxValues[i] = newRMax;
                metricChanged = true;
            }
        }
        
        this.providerWeight = (int) (this.providerWeight + other.providerWeight);
        return physicalChanged || metricChanged;
    }
}
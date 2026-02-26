package marketplace.common;

import simulator.regions.Region;
import simulator.regions.SpatialRegion;
import simulator.core.Location;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MetricHyperCube extends Region {

    private double[] minValues;
    private double[] maxValues;
    private boolean[] minimizeFlags;
    
    // --- DENSITY TRACKING FIELDS ---
    private int providerWeight; 
    private double densityCenterX;
    private double densityCenterY;

    /**
     * CONSTRUCTOR 1: The Unified Master Constructor.
     * @param physicalScope The geographic coverage area (Bounding Box) used for R-Tree routing.
     * @param anchorLocation The exact physical location of the hardware. If null, defaults to the scope's center.
     */
    public MetricHyperCube(double[] min, double[] max, boolean[] minimizeFlags, SpatialRegion physicalScope, Location anchorLocation) {
        super(physicalScope);
        this.minValues = Arrays.copyOf(min, min.length);
        this.maxValues = Arrays.copyOf(max, max.length);
        this.minimizeFlags = Arrays.copyOf(minimizeFlags, minimizeFlags.length);
        
        // Initialize the FaaS node mass
        this.providerWeight = 1;
        
        if (anchorLocation != null) {
            // Day-Zero Truth: Anchor the mass exactly to the hardware
            this.densityCenterX = anchorLocation.getX();
            this.densityCenterY = anchorLocation.getY();
        } else {
            // Fallback for abstract mathematical regions
            Location center = physicalScope.getCenter();
            this.densityCenterX = center.getX();
            this.densityCenterY = center.getY();
        }
    }

    /**
     * CONSTRUCTOR 2: Legacy/Fallback Constructor.
     * Retained for backward compatibility with brokers that only pass a physical scope.
     */
    public MetricHyperCube(double[] min, double[] max, boolean[] minimizeFlags, SpatialRegion physicalScope) {
        this(min, max, minimizeFlags, physicalScope, null);
    }

    /**
     * CONSTRUCTOR 3: Abstract Default Constructor.
     * Used mainly for unit tests where spatial geography is irrelevant.
     */
    public MetricHyperCube(double[] min, double[] max, boolean[] minimizeFlags) {
        this(min, max, minimizeFlags, new Region(0, 0, 0, 0), new Location(0, 0, 0));
    }

    /**
     * CONSTRUCTOR 4: Copy Constructor.
     * Deep copies the absolute SLA bounds and the density state.
     */
    public MetricHyperCube(MetricHyperCube other) {
        super(other);
        this.minValues = Arrays.copyOf(other.minValues, other.minValues.length);
        this.maxValues = Arrays.copyOf(other.maxValues, other.maxValues.length);
        this.minimizeFlags = Arrays.copyOf(other.minimizeFlags, other.minimizeFlags.length);
        
        this.providerWeight = other.providerWeight;
        this.densityCenterX = other.densityCenterX;
        this.densityCenterY = other.densityCenterY;
    }

    /**
     * CONSTRUCTOR 5: Propagation Constructor.
     * Updates the physical R-Tree boundaries while strictly preserving the FaaS
     * population density.
     */
    public MetricHyperCube(double[] min, double[] max, boolean[] minimizeFlags, SpatialRegion physicalScope,
            int preservedMass, double preservedCX, double preservedCY) {
        super(physicalScope);
        this.minValues = Arrays.copyOf(min, min.length);
        this.maxValues = Arrays.copyOf(max, max.length);
        this.minimizeFlags = Arrays.copyOf(minimizeFlags, minimizeFlags.length);

        // Preserve the actual FaaS Node Gravity
        this.providerWeight = preservedMass;
        this.densityCenterX = preservedCX;
        this.densityCenterY = preservedCY;
    }

    @Override
    public Region copy() {
        return new MetricHyperCube(this);
    }

    // --- GETTERS ---
    
    public double[] getMinValues() { return minValues; }
    public double[] getMaxValues() { return maxValues; }
    public boolean[] getMinimizeFlags() { return minimizeFlags; }
    public int getProviderWeight() { return providerWeight; }

    /**
     * Exposes the true FaaS population center for accurate utility routing.
     */
    public Location getDensityCentroid() {
        return new Location(densityCenterX, densityCenterY, 0); 
    }

    public Map<String, Double> getMetricsMap(String[] dimensionNames) {
        if (dimensionNames.length != minValues.length) {
            throw new IllegalArgumentException("Dimension names length mismatch.");
        }
        Map<String, Double> map = new HashMap<>();
        for (int i = 0; i < minValues.length; i++) {
            double val = minimizeFlags[i] ? minValues[i] : maxValues[i];
            map.put(dimensionNames[i], val);
        }
        return map;
    }

    // --- SPATIAL AND LOGICAL EVALUATION ---

    @Override
    public boolean contains(Location location) {
        if (!super.contains(location)) return false;

        if (!(location instanceof MetricLocation req)) {
            throw new IllegalArgumentException("MetricHyperCube requires a MetricLocation for QoS evaluation.");
        }

        if (req.getDimensions() != minValues.length) return false;

        for (int i = 0; i < minValues.length; i++) {
            double reqValue = req.getMetric(i);
            if (minimizeFlags[i]) {
                if (reqValue < this.minValues[i]) return false;
            } else {
                if (reqValue > this.maxValues[i]) return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(SpatialRegion r) {
        if (r == null || r.getBottomLeft() == null) return false;

        boolean physicalContains = (this.getWidth() >= 360.0 - 1e-5) || 
                                   (super.contains(r.getBottomLeft()) && super.contains(r.getTopRight()));
        
        if (!physicalContains) return false;
        if (!(r instanceof MetricHyperCube other)) return false;
        if (this.minValues.length != other.minValues.length) return false;

        for (int i = 0; i < minValues.length; i++) {
            if (this.minValues[i] > other.minValues[i] + 1e-9) return false;
            if (this.maxValues[i] < other.maxValues[i] - 1e-9) return false;
        }

        return true;
    }

    // --- AGGREGATION LOGIC ---

    @Override
    public boolean expand(SpatialRegion r) {
        // 1. Expand the geographic bounding box (R-Tree mechanics)
        boolean physicalChanged = super.expand(r);
        if (!(r instanceof MetricHyperCube other)) return physicalChanged;

        // 2. Shift the Center of Mass based on provider density
        double totalWeight = this.providerWeight + other.providerWeight;
        this.densityCenterX = ((this.densityCenterX * this.providerWeight) + 
                               (other.densityCenterX * other.providerWeight)) / totalWeight;
        this.densityCenterY = ((this.densityCenterY * this.providerWeight) + 
                               (other.densityCenterY * other.providerWeight)) / totalWeight;
        this.providerWeight = (int) totalWeight;

        // 3. Maintain strict L-Infinity bounds for SLA guarantees
        boolean metricChanged = false;
        for (int i = 0; i < minValues.length; i++) {
            double newMin = Math.min(this.minValues[i], other.minValues[i]);
            double newMax = Math.max(this.maxValues[i], other.maxValues[i]);

            if (Math.abs(newMin - this.minValues[i]) > 1e-9 || Math.abs(newMax - this.maxValues[i]) > 1e-9) {
                this.minValues[i] = newMin;
                this.maxValues[i] = newMax;
                metricChanged = true;
            }
        }
        return physicalChanged || metricChanged;
    }
    
    @Override
    public boolean intersects(SpatialRegion r) {
        if (!(r instanceof MetricHyperCube other)) return super.intersects(r);
        if (!super.intersects(r)) return false;

        for (int i = 0; i < minValues.length; i++) {
            double maxOfMins = Math.max(this.minValues[i], other.minValues[i]);
            double minOfMaxs = Math.min(this.maxValues[i], other.maxValues[i]);
            if (maxOfMins > minOfMaxs) return false;
        }
        return true;
    }

    @Override
    public SpatialRegion intersection(SpatialRegion r) {
        if (!intersects(r)) return null;
        if (!(r instanceof MetricHyperCube other)) return super.intersection(r);

        SpatialRegion physicalInt = super.intersection(r);
        if (physicalInt == null) return null;

        double[] newMin = new double[minValues.length];
        double[] newMax = new double[minValues.length];

        for (int i = 0; i < minValues.length; i++) {
            newMin[i] = Math.max(this.minValues[i], other.minValues[i]);
            newMax[i] = Math.min(this.maxValues[i], other.maxValues[i]);
        }
        
        // When computing intersections mathematically, we fallback to the geometric center
        return new MetricHyperCube(newMin, newMax, this.minimizeFlags, physicalInt, null);
    }

    @Override
    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toShortString()).append(" | Mass:").append(providerWeight).append(" | QoS[");
        for (int i = 0; i < minValues.length; i++) {
            String shortKey = MarketplaceMetricSchema.SHORT_NAMES.get(MarketplaceMetricSchema.KEYS[i]);
            String minStr = minValues[i] > 1e9 ? "INF" : String.format("%.2f", minValues[i]);
            String maxStr = maxValues[i] > 1e9 ? "INF" : String.format("%.2f", maxValues[i]);
            sb.append(shortKey).append(":").append(minStr).append("->").append(maxStr);
            if (i < minValues.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
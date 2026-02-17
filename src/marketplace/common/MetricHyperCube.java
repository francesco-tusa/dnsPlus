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

    public MetricHyperCube(double[] min, double[] max, boolean[] minimizeFlags) {
        super(new Location(0, 0, 0), new Location(0, 0, 0));
        this.minValues = Arrays.copyOf(min, min.length);
        this.maxValues = Arrays.copyOf(max, max.length);
        this.minimizeFlags = Arrays.copyOf(minimizeFlags, minimizeFlags.length);
    }

    public MetricHyperCube(double[] min, double[] max, boolean[] minimizeFlags, SpatialRegion physicalScope) {
        super(physicalScope);
        this.minValues = Arrays.copyOf(min, min.length);
        this.maxValues = Arrays.copyOf(max, max.length);
        this.minimizeFlags = Arrays.copyOf(minimizeFlags, minimizeFlags.length);
    }

    public MetricHyperCube(MetricHyperCube other) {
        super(other);
        this.minValues = Arrays.copyOf(other.minValues, other.minValues.length);
        this.maxValues = Arrays.copyOf(other.maxValues, other.maxValues.length);
        this.minimizeFlags = Arrays.copyOf(other.minimizeFlags, other.minimizeFlags.length);
    }

    public Region copy() {
        return new MetricHyperCube(this);
    }

    public double[] getMinValues() { return minValues; }
    public double[] getMaxValues() { return maxValues; }
    public boolean[] getMinimizeFlags() { return minimizeFlags; }

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

    @Override
    public boolean contains(Location location) {
        // Check physical bounds first
        if (!super.contains(location)) return false;

        // If a standard Location leaks into a QoS evaluation, it is a fatal routing error.
        if (!(location instanceof MetricLocation)) {
            throw new IllegalArgumentException(
                "Strict Type Enforcement: MetricHyperCube requires a MetricLocation for QoS evaluation. " +
                "Received plain Location type: " + location.getClass().getName()
            );
        }

        MetricLocation req = (MetricLocation) location;

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

        // 1. Physical Containment
        // We explicitly use super.contains() to evaluate the corners as raw coordinates, 
        // completely bypassing our strict QoS type-check above.
        boolean physicalContains = false;
        if (this.getWidth() >= 360.0 - 1e-5) {
            physicalContains = true;
        } else {
            physicalContains = super.contains(r.getBottomLeft()) && super.contains(r.getTopRight());
        }
        
        if (!physicalContains) return false;

        // 2. Logical QoS Containment
        if (!(r instanceof MetricHyperCube)) {
            // A HyperCube requires exact QoS constraints. It cannot logically "contain" 
            // an unbounded/plain spatial region.
            return false;
        }

        MetricHyperCube other = (MetricHyperCube) r;
        if (this.minValues.length != other.minValues.length) return false;

        // For `this` to logically contain `other`, `this` must have equal or wider bounds in all dimensions.
        for (int i = 0; i < minValues.length; i++) {
            if (this.minValues[i] > other.minValues[i] + 1e-9) return false;
            if (this.maxValues[i] < other.maxValues[i] - 1e-9) return false;
        }

        return true;
    }

    @Override
    public boolean expand(SpatialRegion r) {
        boolean physicalChanged = super.expand(r);
        if (!(r instanceof MetricHyperCube)) return physicalChanged;

        MetricHyperCube other = (MetricHyperCube) r;
        boolean metricChanged = false;
        for (int i = 0; i < minValues.length; i++) {
            double newMin = Math.min(this.minValues[i], other.minValues[i]);
            double newMax = Math.max(this.maxValues[i], other.maxValues[i]);

            if (Math.abs(newMin - this.minValues[i]) > 1e-9 ||
                    Math.abs(newMax - this.maxValues[i]) > 1e-9) {
                this.minValues[i] = newMin;
                this.maxValues[i] = newMax;
                metricChanged = true;
            }
        }
        return physicalChanged || metricChanged;
    }
    
    @Override
    public boolean intersects(SpatialRegion r) {
        if (!(r instanceof MetricHyperCube)) return super.intersects(r);
        if (!super.intersects(r)) return false;

        MetricHyperCube other = (MetricHyperCube) r;
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
        if (!(r instanceof MetricHyperCube)) return super.intersection(r);

        MetricHyperCube other = (MetricHyperCube) r;
        SpatialRegion physicalInt = super.intersection(r);
        if (physicalInt == null) return null;

        double[] newMin = new double[minValues.length];
        double[] newMax = new double[minValues.length];

        for (int i = 0; i < minValues.length; i++) {
            newMin[i] = Math.max(this.minValues[i], other.minValues[i]);
            newMax[i] = Math.min(this.maxValues[i], other.maxValues[i]);
        }
        return new MetricHyperCube(newMin, newMax, this.minimizeFlags, physicalInt);
    }

    @Override
    public String toShortString() {
        return super.toShortString() + " | Metrics: " + Arrays.toString(minValues) + "->" + Arrays.toString(maxValues);
    }
}
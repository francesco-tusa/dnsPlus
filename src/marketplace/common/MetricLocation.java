package marketplace.common;

import simulator.core.Location;

/**
 * Represents a Point in N-Dimensional Metric Space.
 * Used by Clients to define their REQUIREMENTS.
 * Also carries Physical Location for efficient Spatial Indexing.
 */
public class MetricLocation extends Location {

    private final double[] metrics;

    public MetricLocation(double[] metrics) {
        super(0, 0, 0); // Default, dangerous if using R-Tree
        this.metrics = metrics;
    }

    public MetricLocation(double[] metrics, Location physicalLoc) {
        super(physicalLoc.getX(), physicalLoc.getY(), physicalLoc.getZ());
        this.metrics = metrics;
    }

    public int getDimensions() {
        return metrics.length;
    }

    /**
     * RENAMED: Returns the raw array of metric values (e.g., [Latency, Cost])
     * distinct from the physical coordinates (X, Y).
     */
    public double[] getMetricValues() {
        return metrics;
    }

    public double getMetric(int index) {
        if (index < 0 || index >= metrics.length) {
            throw new IndexOutOfBoundsException("Invalid metric dimension: " + index);
        }
        return metrics[index];
    }
}